package com.example.data

import com.example.model.SurahVerse

object SurahYasinData {
  const val SURAH_NAME = "Surah Yasin"
  const val SURAH_ARABIC_NAME = "سُوْرَةُ يٰسۤ"
  const val SURAH_SUBTITLE = "Surah ke-36 • Makkiyah • 83 Ayat"
  const val BISMILLAH_ARABIC = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ"
  const val BISMILLAH_LATIN = "Bismillāhir-rahmānir-rahīm"
  const val BISMILLAH_TRANSLATION = "Dengan nama Allah Yang Maha Pengasih lagi Maha Penyayang."

  val verses: List<SurahVerse> by lazy {
    surahYasinPart1 + surahYasinPart2
  }
}
