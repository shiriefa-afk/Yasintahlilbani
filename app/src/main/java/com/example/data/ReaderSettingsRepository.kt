package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ReaderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "reader_settings"
)

class ReaderSettingsRepository(context: Context) {
  private val dataStore = context.applicationContext.readerSettingsDataStore

  val settingsFlow: Flow<ReaderSettings> = dataStore.data.map { prefs ->
    ReaderSettings(
      arabicFontSizeSp = prefs[KEY_ARABIC_FONT_SIZE] ?: 26f,
      showLatin = prefs[KEY_SHOW_LATIN] ?: true,
      showTranslation = prefs[KEY_SHOW_TRANSLATION] ?: true,
      nightMode = prefs[KEY_NIGHT_MODE] ?: false,
      hapticEnabled = prefs[KEY_HAPTIC_ENABLED] ?: true,
    )
  }

  suspend fun updateArabicFontSize(sizeSp: Float) {
    val clamped = sizeSp.coerceIn(20f, 38f)
    dataStore.edit { it[KEY_ARABIC_FONT_SIZE] = clamped }
  }

  suspend fun setShowLatin(show: Boolean) {
    dataStore.edit { it[KEY_SHOW_LATIN] = show }
  }

  suspend fun setShowTranslation(show: Boolean) {
    dataStore.edit { it[KEY_SHOW_TRANSLATION] = show }
  }

  suspend fun setNightMode(enabled: Boolean) {
    dataStore.edit { it[KEY_NIGHT_MODE] = enabled }
  }

  suspend fun setHapticEnabled(enabled: Boolean) {
    dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
  }

  private companion object {
    val KEY_ARABIC_FONT_SIZE = floatPreferencesKey("arabic_font_size_sp")
    val KEY_SHOW_LATIN = booleanPreferencesKey("show_latin")
    val KEY_SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
    val KEY_NIGHT_MODE = booleanPreferencesKey("night_mode")
    val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
  }
}
