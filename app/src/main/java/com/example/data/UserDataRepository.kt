package com.example.data

import android.content.Context
import com.example.data.local.DeceasedPrayerEntity
import com.example.data.local.ReadingStateEntity
import com.example.data.local.TasbihStateEntity
import com.example.data.local.YasinTahlilDao
import com.example.data.local.YasinTahlilDatabase
import com.example.model.DeceasedPrayer
import com.example.model.ReadingState
import com.example.model.TasbihState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class UserDataRepository(
  private val dao: YasinTahlilDao,
  private val cloudSync: DeceasedPrayerCloudSync? = null,
) {
  constructor(context: Context) : this(
    YasinTahlilDatabase.getInstance(context).yasinTahlilDao(),
    DeceasedPrayerCloudSync(
      context,
      YasinTahlilDatabase.getInstance(context).yasinTahlilDao(),
    ),
  )

  fun cloudAccountEmail(): String? = cloudSync?.accountEmail()

  suspend fun signInWithGoogle(idToken: String): Result<String> = try {
    Result.success(requireNotNull(cloudSync) { "Sinkronisasi belum tersedia." }.signInWithGoogle(idToken))
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Result.failure(e)
  }

  suspend fun syncCloud(): Result<Unit> = try {
    requireNotNull(cloudSync) { "Sinkronisasi belum tersedia." }.syncNow()
    Result.success(Unit)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Result.failure(e)
  }

  fun signOutCloud() = cloudSync?.signOut()

  fun observeDeceasedPrayers(): Flow<List<DeceasedPrayer>> {
    return dao.observeDeceasedPrayers().map { entities ->
      entities.map { it.toModel() }
    }
  }

  suspend fun addDeceasedPrayer(
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    includedInPrayer: Boolean = true,
  ): Result<Unit> {
    return try {
      val cleanHonorific = if (honorific == "Almarhumah") "Almarhumah" else "Almarhum"
      val cleanPrefix = if (parentPrefix == "binti") "binti" else "bin"
      val cleanName = fullName.trim().take(80)
      val cleanParent = parentName.trim().take(80)
      val cleanNote = relationNote?.trim()?.takeIf { it.isNotEmpty() }?.take(100)
      require(cleanName.isNotEmpty()) { "Nama lengkap tidak boleh kosong." }

      val now = System.currentTimeMillis()
      dao.insertDeceasedPrayer(
        DeceasedPrayerEntity(
          id = UUID.randomUUID().toString(),
          honorific = cleanHonorific,
          fullName = cleanName,
          parentPrefix = cleanPrefix,
          parentName = cleanParent,
          relationNote = cleanNote,
          includedInPrayer = includedInPrayer,
          createdAtMillis = now,
          updatedAtMillis = now,
        )
      )
      cloudSync?.syncIfSignedIn()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateDeceasedPrayer(
    entryId: String,
    honorific: String,
    fullName: String,
    parentPrefix: String,
    parentName: String,
    relationNote: String?,
    includedInPrayer: Boolean,
  ): Result<Unit> {
    return try {
      require(entryId.isNotBlank()) { "ID data tidak valid." }
      val cleanHonorific = if (honorific == "Almarhumah") "Almarhumah" else "Almarhum"
      val cleanPrefix = if (parentPrefix == "binti") "binti" else "bin"
      val cleanName = fullName.trim().take(80)
      val cleanParent = parentName.trim().take(80)
      val cleanNote = relationNote?.trim()?.takeIf { it.isNotEmpty() }?.take(100)
      require(cleanName.isNotEmpty()) { "Nama lengkap tidak boleh kosong." }

      val updated = dao.updateDeceasedPrayer(
        id = entryId,
        honorific = cleanHonorific,
        fullName = cleanName,
        parentPrefix = cleanPrefix,
        parentName = cleanParent,
        relationNote = cleanNote,
        includedInPrayer = includedInPrayer,
        updatedAtMillis = System.currentTimeMillis(),
      )
      if (updated == 0) {
        return Result.failure(IllegalStateException("Data arwah tidak ditemukan."))
      }
      cloudSync?.syncIfSignedIn()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun toggleDeceasedPrayerIncluded(entryId: String, included: Boolean): Result<Unit> {
    return try {
      require(entryId.isNotBlank()) { "ID data tidak valid." }
      val updated = dao.updateDeceasedPrayerIncluded(
        id = entryId,
        includedInPrayer = included,
        updatedAtMillis = System.currentTimeMillis(),
      )
      if (updated == 0) {
        return Result.failure(IllegalStateException("Data arwah tidak ditemukan."))
      }
      cloudSync?.syncIfSignedIn()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteDeceasedPrayer(entryId: String): Result<Unit> {
    return try {
      require(entryId.isNotBlank()) { "ID data tidak valid." }
      dao.deleteDeceasedPrayerById(entryId)
      cloudSync?.deleteEntry(entryId)
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun observeReadingState(): Flow<ReadingState> {
    return dao.observeReadingState().map { entity ->
      entity?.toModel() ?: ReadingState()
    }
  }

  suspend fun saveLastYasinVerse(verseNumber: Int): Result<Unit> {
    return try {
      val safeVerse = verseNumber.coerceIn(1, 83).toLong()
      dao.updateLastYasinVerse(safeVerse, System.currentTimeMillis())
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveLastTahlilStep(stepIndex: Int): Result<Unit> {
    return try {
      val safeStep = stepIndex.coerceIn(0, 50).toLong()
      dao.updateLastTahlilStep(safeStep, System.currentTimeMillis())
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  data class BookmarkToggleOutcome(val isBookmarked: Boolean)

  suspend fun toggleYasinBookmark(verseNumber: Int): Result<BookmarkToggleOutcome> {
    return try {
      val safeVerse = verseNumber.coerceIn(1, 83).toLong()
      val current = dao.getReadingStateOnce()?.toModel() ?: ReadingState()
      val added: Boolean
      val updatedBookmarks = if (safeVerse in current.bookmarkedVerses) {
        added = false
        current.bookmarkedVerses - safeVerse
      } else {
        added = true
        (current.bookmarkedVerses + safeVerse).sorted()
      }
      dao.updateBookmarkedVerses(
        csv = updatedBookmarks.joinToString(","),
        updatedAtMillis = System.currentTimeMillis(),
      )
      Result.success(BookmarkToggleOutcome(isBookmarked = added))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun saveReadingState(
    lastYasinVerse: Int,
    lastTahlilStep: Int,
    bookmarkedVerses: List<Long>,
  ): Result<Unit> {
    return try {
      val safeVerse = lastYasinVerse.coerceIn(1, 83).toLong()
      val safeStep = lastTahlilStep.coerceIn(0, 50).toLong()
      val safeBookmarks = bookmarkedVerses.distinct().filter { it in 1L..83L }.sorted().take(83)
      dao.upsertReadingState(
        ReadingStateEntity(
          id = 1,
          lastYasinVerse = safeVerse,
          lastTahlilStep = safeStep,
          bookmarkedVersesCsv = safeBookmarks.joinToString(","),
          updatedAtMillis = System.currentTimeMillis(),
        )
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun observeTasbihState(): Flow<TasbihState> {
    return dao.observeTasbihState().map { entity ->
      entity?.toModel() ?: TasbihState()
    }
  }

  suspend fun saveTasbihState(
    selectedDhikrId: String,
    currentCount: Long,
    targetCount: Long,
    lifetimeCount: Long,
  ): Result<Unit> {
    return try {
      val safeDhikrId = selectedDhikrId.trim().take(40).ifEmpty { "subhanallah" }
      val safeCurrent = currentCount.coerceIn(0L, 100_000L)
      val safeTarget = targetCount.coerceIn(0L, 10_000L)
      val safeLifetime = lifetimeCount.coerceIn(0L, 100_000_000L)
      dao.upsertTasbihState(
        TasbihStateEntity(
          id = 1,
          selectedDhikrId = safeDhikrId,
          currentCount = safeCurrent,
          targetCount = safeTarget,
          lifetimeCount = safeLifetime,
          updatedAtMillis = System.currentTimeMillis(),
        )
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun DeceasedPrayerEntity.toModel(): DeceasedPrayer = DeceasedPrayer(
    id = id,
    honorific = honorific,
    fullName = fullName,
    parentPrefix = parentPrefix,
    parentName = parentName,
    relationNote = relationNote,
    includedInPrayer = includedInPrayer,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
  )

  private fun ReadingStateEntity.toModel(): ReadingState {
    val bookmarks = bookmarkedVersesCsv
      .split(',')
      .mapNotNull { it.trim().toLongOrNull() }
      .filter { it in 1L..83L }
      .distinct()
      .sorted()
    return ReadingState(
      lastYasinVerse = lastYasinVerse,
      lastTahlilStep = lastTahlilStep,
      bookmarkedVerses = bookmarks,
      updatedAtMillis = updatedAtMillis,
    )
  }

  private fun TasbihStateEntity.toModel(): TasbihState = TasbihState(
    selectedDhikrId = selectedDhikrId.ifBlank { "subhanallah" },
    currentCount = currentCount,
    targetCount = targetCount,
    lifetimeCount = lifetimeCount,
    updatedAtMillis = updatedAtMillis,
  )
}
