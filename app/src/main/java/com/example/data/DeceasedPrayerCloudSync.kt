package com.example.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import com.example.data.local.DeceasedPrayerEntity
import com.example.data.local.YasinTahlilDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class DeceasedPrayerCloudSync(context: Context, private val dao: YasinTahlilDao) {
  private val preferences = EncryptedSharedPreferences.create(
    context,
    "deceased_prayer_cloud_session",
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
  )
  private val client = OkHttpClient.Builder()
    .callTimeout(20, TimeUnit.SECONDS)
    .build()
  private val jsonType = "application/json; charset=utf-8".toMediaType()

  fun accountEmail(): String? = preferences.getString(KEY_EMAIL, null)

  suspend fun signInWithGoogle(idToken: String): String = withContext(Dispatchers.IO) {
    require(
      BuildConfig.NEXT_PUBLIC_SUPABASE_URL.startsWith("https://") &&
        !BuildConfig.NEXT_PUBLIC_SUPABASE_URL.contains("your-project") &&
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank() &&
        !BuildConfig.SUPABASE_PUBLISHABLE_KEY.startsWith("your-"),
    ) { "Konfigurasi Supabase belum tersedia." }
    require(idToken.isNotBlank()) { "Token Google tidak valid." }
    val response = request(
      path = "/auth/v1/token?grant_type=id_token",
      method = "POST",
      body = JSONObject().put("provider", "google").put("id_token", idToken),
      authenticated = false,
    )
    preferences.edit()
      .putString(KEY_ACCESS_TOKEN, response.getString("access_token"))
      .putString(KEY_REFRESH_TOKEN, response.getString("refresh_token"))
      .putString(KEY_EMAIL, response.optJSONObject("user")?.optString("email"))
      .apply()
    syncLocked()
    accountEmail() ?: "Akun Google"
  }

  suspend fun syncNow() = withContext(Dispatchers.IO) { syncLocked() }

  suspend fun syncIfSignedIn() = withContext(Dispatchers.IO) {
    if (accountEmail() != null) runCatching { syncLocked() }
  }

  suspend fun deleteEntry(id: String) = withContext(Dispatchers.IO) {
    val pending = preferences.getStringSet(KEY_PENDING_DELETES, mutableSetOf()).orEmpty().toMutableSet()
    pending.add(id)
    preferences.edit().putStringSet(KEY_PENDING_DELETES, pending).apply()
    if (accountEmail() != null) runCatching { processPendingDeletes() }
  }

  fun signOut() {
    val pendingDeletes = preferences.getStringSet(KEY_PENDING_DELETES, mutableSetOf()).orEmpty().toMutableSet()
    preferences.edit().clear().putStringSet(KEY_PENDING_DELETES, pendingDeletes).apply()
  }

  private fun syncLocked() {
    check(accountEmail() != null) { "Hubungkan akun Google untuk menyinkronkan daftar." }
    processPendingDeletes()
    val local = dao.getDeceasedPrayersOnce().map { it.toCloudJson() }
    val remote = request(
      path = "/rest/v1/deceased_prayers?select=id,honorific,full_name,parent_prefix,parent_name,relation_note,included_in_prayer,created_at,updated_at",
      method = "GET",
    ).arrayFromResponse()
    val merged = linkedMapOf<String, JSONObject>()
    (remote + local).forEach { item ->
      val id = item.getString("id")
      val previous = merged[id]
      if (previous == null || item.optString("updated_at") >= previous.optString("updated_at")) {
        merged[id] = item
      }
    }
    val userId = request("/auth/v1/user", "GET").getString("id")
    val mergedRows = merged.values.map { it.put("user_id", userId) }
    if (mergedRows.isNotEmpty()) {
      val payload = JSONArray().apply { mergedRows.forEach(::put) }
      request(
        path = "/rest/v1/deceased_prayers?on_conflict=id",
        method = "POST",
        body = JSONObject().put("rows", payload).getJSONArray("rows"),
        prefer = "resolution=merge-duplicates,return=minimal",
      )
    }
    val entities = mergedRows.map { it.toEntity() }
    if (entities.isNotEmpty()) dao.upsertDeceasedPrayers(entities)
  }

  private fun processPendingDeletes() {
    val pending = preferences.getStringSet(KEY_PENDING_DELETES, mutableSetOf()).orEmpty().toSet()
    pending.forEach { id ->
      val encodedId = java.net.URLEncoder.encode(id, "UTF-8")
      request("/rest/v1/deceased_prayers?id=eq.$encodedId", "DELETE")
      val remaining = preferences.getStringSet(KEY_PENDING_DELETES, mutableSetOf()).orEmpty().toMutableSet()
      remaining.remove(id)
      preferences.edit().putStringSet(KEY_PENDING_DELETES, remaining).apply()
    }
  }

  private fun request(
    path: String,
    method: String,
    body: Any? = null,
    authenticated: Boolean = true,
    prefer: String? = null,
    retryAuth: Boolean = true,
  ): JSONObject {
    val baseUrl = BuildConfig.NEXT_PUBLIC_SUPABASE_URL.trimEnd('/')
    val requestBuilder = Request.Builder()
      .url("$baseUrl$path")
      .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
      .header("Accept", "application/json")
    if (authenticated) {
      val token = preferences.getString(KEY_ACCESS_TOKEN, null)
        ?: throw IOException("Hubungkan akun Google terlebih dahulu.")
      requestBuilder.header("Authorization", "Bearer $token")
    }
    if (prefer != null) requestBuilder.header("Prefer", prefer)
    val payload = when (body) {
      is JSONObject -> body.toString()
      is JSONArray -> body.toString()
      else -> null
    }
    val request = requestBuilder.method(
      method,
      payload?.toRequestBody(jsonType) ?: if (method == "POST" || method == "PATCH") "{}".toRequestBody(jsonType) else null,
    ).build()
    client.newCall(request).execute().use { response ->
      val text = response.body?.string().orEmpty()
      if (response.code == 401 && authenticated && retryAuth) {
        refreshSession()
        return request(path, method, body, authenticated, prefer, retryAuth = false)
      }
      if (!response.isSuccessful) throw IOException("Sinkronisasi gagal (${response.code}). ${text.take(200)}")
      return if (text.isBlank()) JSONObject() else if (text.startsWith("[")) JSONObject().put("data", JSONArray(text)) else JSONObject(text)
    }
  }

  private fun refreshSession() {
    val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)
      ?: throw IOException("Sesi berakhir. Masuk kembali dengan Google.")
    val refreshed = request(
      "/auth/v1/token?grant_type=refresh_token",
      "POST",
      JSONObject().put("refresh_token", refreshToken),
      authenticated = false,
      retryAuth = false,
    )
    preferences.edit()
      .putString(KEY_ACCESS_TOKEN, refreshed.getString("access_token"))
      .putString(KEY_REFRESH_TOKEN, refreshed.getString("refresh_token"))
      .apply()
  }

  private fun JSONObject.arrayFromResponse(): List<JSONObject> {
    val array = optJSONArray("data") ?: JSONArray()
    return (0 until array.length()).map { array.getJSONObject(it) }
  }

  private fun DeceasedPrayerEntity.toCloudJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("honorific", honorific)
    .put("full_name", fullName)
    .put("parent_prefix", parentPrefix)
    .put("parent_name", parentName)
    .put("relation_note", relationNote ?: JSONObject.NULL)
    .put("included_in_prayer", includedInPrayer)
    .put("created_at", isoDate(createdAtMillis))
    .put("updated_at", isoDate(updatedAtMillis))

  private fun JSONObject.toEntity(): DeceasedPrayerEntity = DeceasedPrayerEntity(
    id = optString("id").takeIf { runCatching { UUID.fromString(it) }.isSuccess } ?: UUID.randomUUID().toString(),
    honorific = optString("honorific", "Almarhum"),
    fullName = optString("full_name"),
    parentPrefix = optString("parent_prefix", "bin"),
    parentName = optString("parent_name"),
    relationNote = if (isNull("relation_note")) null else optString("relation_note"),
    includedInPrayer = optBoolean("included_in_prayer", true),
    createdAtMillis = parseDate(optString("created_at")),
    updatedAtMillis = parseDate(optString("updated_at")),
  )

  private fun isoDate(millis: Long): String = dateFormat().format(Date(millis))
  private fun parseDate(value: String): Long = runCatching {
    val normalized = value.replace(Regex("\\.(\\d{3})\\d+(?=Z|[+-])"), ".$1")
    dateFormat().parse(normalized)?.time
  }.getOrNull() ?: System.currentTimeMillis()
  private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
  private companion object {
    const val KEY_EMAIL = "email"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_REFRESH_TOKEN = "refresh_token"
    const val KEY_PENDING_DELETES = "pending_deletes"
  }
}
