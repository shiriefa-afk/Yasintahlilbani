package com.example.model

data class SurahVerse(
  val number: Int,
  val arabic: String,
  val latin: String,
  val translation: String,
)

data class ShortSurah(
  val id: String,
  val titleLatin: String,
  val titleArabic: String,
  val subtitle: String,
  val verses: List<SurahVerse>,
)

data class TahlilStep(
  val id: String,
  val order: Int,
  val title: String,
  val arabicTitle: String,
  val arabic: String,
  val latin: String,
  val translation: String,
  val repetitionTarget: Int = 1,
  val supportsDeceasedNames: Boolean = false,
  val guidanceNote: String? = null,
)

data class DhikrPreset(
  val id: String,
  val title: String,
  val arabic: String,
  val latin: String,
  val meaning: String,
  val defaultTarget: Int,
)
