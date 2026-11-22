package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DeceasedPrayer
import com.example.model.ReaderSettings
import com.example.model.ReadingState
import com.example.model.UiState

enum class SurahTahlilSubSection(val label: String, val testTag: String) {
  DASHBOARD("Ringkasan", "subtab_dashboard"),
  YASIN("Surah Yasin", "subtab_yasin"),
  TAHLIL("Tahlil & Doa", "subtab_tahlil"),
  SHORT_SURAHS("Surah Pendek", "subtab_short_surahs"),
}

@Composable
fun SurahTahlilDashboardScreen(
  activeSubSection: SurahTahlilSubSection,
  onSubSectionChange: (SurahTahlilSubSection) -> Unit,
  readingState: UiState<ReadingState>,
  deceasedState: UiState<List<DeceasedPrayer>>,
  settings: ReaderSettings,
  lifetimeTasbihCount: Long,
  pendingYasinVerse: Int?,
  pendingShortSurahTab: Boolean,
  stepCounts: Map<String, Int>,
  onSaveLastReadVerse: (Int) -> Unit,
  onToggleBookmark: (Int) -> Unit,
  onConsumeTargetVerse: () -> Unit,
  onIncrementStepCount: (stepId: String, target: Int) -> Unit,
  onResetStepCount: (stepId: String) -> Unit,
  onSaveTahlilStep: (Int) -> Unit,
  onConsumeShortSurahTab: () -> Unit,
  onOpenYasinAt: (Int) -> Unit,
  onOpenTahlil: (openShortSurahs: Boolean) -> Unit,
  onSwitchToTasbih: () -> Unit,
  onSwitchToAlmarhumList: () -> Unit,
  onToggleNightMode: () -> Unit,
  onOpenDisplaySettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (activeSubSection != SurahTahlilSubSection.DASHBOARD) {
    BackHandler {
      onSubSectionChange(SurahTahlilSubSection.DASHBOARD)
    }
  }

  LaunchedEffect(pendingShortSurahTab) {
    if (pendingShortSurahTab) {
      onConsumeShortSurahTab()
    }
  }

  val isReadingStateLoaded = readingState is UiState.Success
  val initialTahlilStep = ((readingState as? UiState.Success)?.data?.lastTahlilStep ?: 0L).toInt()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
    ) {
      ScrollableTabRow(
        selectedTabIndex = activeSubSection.ordinal,
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
      ) {
        SurahTahlilSubSection.entries.forEach { section ->
          Tab(
            selected = activeSubSection == section,
            onClick = { onSubSectionChange(section) },
            text = { Text(section.label, maxLines = 1) },
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag(section.testTag),
          )
        }
      }

      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (activeSubSection) {
          SurahTahlilSubSection.DASHBOARD -> {
            HomeScreen(
              readingState = readingState,
              deceasedState = deceasedState,
              settings = settings,
              lifetimeTasbihCount = lifetimeTasbihCount,
              onOpenYasin = onOpenYasinAt,
              onOpenTahlil = onOpenTahlil,
              onOpenTasbih = onSwitchToTasbih,
              onOpenArwahList = onSwitchToAlmarhumList,
              onToggleNightMode = onToggleNightMode,
              onOpenDisplaySettings = onOpenDisplaySettings,
            )
          }

          SurahTahlilSubSection.YASIN -> {
            YasinReaderScreen(
              initialTargetVerse = pendingYasinVerse,
              readingState = readingState,
              settings = settings,
              onSaveLastReadVerse = onSaveLastReadVerse,
              onToggleBookmark = onToggleBookmark,
              onOpenDisplaySettings = onOpenDisplaySettings,
              onConsumeTargetVerse = onConsumeTargetVerse,
            )
          }

          SurahTahlilSubSection.TAHLIL -> {
            TahlilReadingGuideScreen(
              initialStepIndex = initialTahlilStep,
              isReadingStateLoaded = isReadingStateLoaded,
              deceasedState = deceasedState,
              settings = settings,
              stepCounts = stepCounts,
              onIncrementStepCount = onIncrementStepCount,
              onResetStepCount = onResetStepCount,
              onSaveTahlilStep = onSaveTahlilStep,
              onOpenArwahList = onSwitchToAlmarhumList,
              onOpenDisplaySettings = onOpenDisplaySettings,
            )
          }

          SurahTahlilSubSection.SHORT_SURAHS -> {
            ShortSurahsScreen(
              settings = settings,
              onOpenDisplaySettings = onOpenDisplaySettings,
            )
          }
        }
      }
    }
  }
}
