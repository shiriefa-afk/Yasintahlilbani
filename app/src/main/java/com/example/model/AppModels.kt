package com.example.model

data class DeceasedPrayer(
  val id: String = "",
  val honorific: String = "Almarhum",
  val fullName: String = "",
  val parentPrefix: String = "bin",
  val parentName: String = "",
  val relationNote: String? = null,
  val includedInPrayer: Boolean = true,
  val createdAtMillis: Long = 0L,
  val updatedAtMillis: Long = 0L,
) {
  fun formattedPrayerName(): String {
    val base = "$honorific $fullName".trim()
    return if (parentName.isNotBlank()) {
      "$base $parentPrefix $parentName".trim()
    } else {
      base
    }
  }
}

data class ReadingState(
  val lastYasinVerse: Long = 1L,
  val lastTahlilStep: Long = 0L,
  val bookmarkedVerses: List<Long> = emptyList(),
  val updatedAtMillis: Long = 0L,
)

data class TasbihState(
  val selectedDhikrId: String = "subhanallah",
  val currentCount: Long = 0L,
  val targetCount: Long = 33L,
  val lifetimeCount: Long = 0L,
  val updatedAtMillis: Long = 0L,
)

data class ReaderSettings(
  val arabicFontSizeSp: Float = 26f,
  val showLatin: Boolean = true,
  val showTranslation: Boolean = true,
  val nightMode: Boolean = false,
  val hapticEnabled: Boolean = true,
)

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}
