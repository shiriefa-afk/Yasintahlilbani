package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TahlilAndSurahsData
import com.example.model.DeceasedPrayer
import com.example.model.ReaderSettings
import com.example.model.TahlilStep
import com.example.model.UiState
import com.example.ui.components.MushafSectionDivider
import com.example.ui.components.VerseNumberMedallion
import com.example.ui.theme.AmiriFontFamily
import kotlinx.coroutines.launch

private enum class TahlilGuidePhase(val label: String, val range: IntRange) {
  OPENING("1. Pembukaan & Tawasul", 1..2),
  SURAHS("2. Surah & Ayat Pilihan", 3..10),
  DHIKR("3. Dzikir, Tahlil & Shalawat", 11..15),
  PRAYER("4. Doa Tahlil & Doa Arwah", 16..16),
}

private fun phaseForOrder(order: Int): TahlilGuidePhase =
  TahlilGuidePhase.entries.firstOrNull { order in it.range } ?: TahlilGuidePhase.OPENING

@Composable
fun TahlilReadingGuideScreen(
  initialStepIndex: Int,
  isReadingStateLoaded: Boolean,
  deceasedState: UiState<List<DeceasedPrayer>>,
  settings: ReaderSettings,
  stepCounts: Map<String, Int>,
  onIncrementStepCount: (stepId: String, target: Int) -> Unit,
  onResetStepCount: (stepId: String) -> Unit,
  onSaveTahlilStep: (Int) -> Unit,
  onOpenArwahList: () -> Unit,
  onOpenDisplaySettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val steps = TahlilAndSurahsData.tahlilSteps
  val totalSteps = steps.size
  var currentStepIndex by rememberSaveable {
    mutableIntStateOf(initialStepIndex.coerceIn(0, (totalSteps - 1).coerceAtLeast(0)))
  }
  var hasRestoredSavedStep by rememberSaveable { mutableStateOf(isReadingStateLoaded) }
  var isStepByStepMode by rememberSaveable { mutableStateOf(true) }
  var showTocSheet by remember { mutableStateOf(false) }

  LaunchedEffect(isReadingStateLoaded, initialStepIndex) {
    if (isReadingStateLoaded && !hasRestoredSavedStep) {
      currentStepIndex = initialStepIndex.coerceIn(0, (totalSteps - 1).coerceAtLeast(0))
      hasRestoredSavedStep = true
    }
  }

  val scrollListState = rememberLazyListState()
  val stepScrollState = rememberScrollState()
  val scope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current

  val activeArwah = (deceasedState as? UiState.Success)?.data
    ?.filter { it.includedInPrayer }
    ?: emptyList()

  val currentStep = steps[currentStepIndex]
  val currentPhase = phaseForOrder(currentStep.order)
  val progressFraction = (currentStepIndex + 1).toFloat() / totalSteps.toFloat()

  fun jumpToStep(index: Int) {
    val safeIndex = index.coerceIn(0, totalSteps - 1)
    currentStepIndex = safeIndex
    onSaveTahlilStep(safeIndex)
    scope.launch {
      stepScrollState.scrollTo(0)
      if (!isStepByStepMode) {
        scrollListState.animateScrollToItem(safeIndex)
      }
    }
  }

  LaunchedEffect(currentStepIndex) {
    stepScrollState.scrollTo(0)
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize(),
    ) {
      // Guide Top Bar & Progress
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Panduan Tahlil & Doa Arwah",
              style = MaterialTheme.typography.titleLarge,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${currentPhase.label} • Bacaan ${currentStepIndex + 1} dari $totalSteps",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { isStepByStepMode = !isStepByStepMode },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("tahlil_toggle_view_mode"),
            ) {
              Icon(
                imageVector = if (isStepByStepMode) Icons.Filled.ViewAgenda else Icons.Filled.ViewCarousel,
                contentDescription = if (isStepByStepMode) "Tampilkan semua urutan" else "Mode panduan per langkah",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(
              onClick = { showTocSheet = true },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("tahlil_toc_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "Buka daftar isi Tahlil",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
            IconButton(
              onClick = onOpenDisplaySettings,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("tahlil_display_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Pengaturan tampilan Tahlil",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }

        LinearProgressIndicator(
          progress = { progressFraction },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Quick Guide Shortcuts
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AssistChip(
            onClick = { showTocSheet = true },
            leadingIcon = {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text("Daftar Isi ($totalSteps)") },
            modifier = Modifier.testTag("tahlil_quick_toc_chip"),
          )
          AssistChip(
            onClick = { jumpToStep(totalSteps - 1) },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text("Langsung ke Doa Arwah") },
            modifier = Modifier.testTag("tahlil_jump_doa_arwah_chip"),
          )
        }
      }

      if (isStepByStepMode) {
        // Focused Step-by-Step Reading Guide
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        ) {
          AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
              fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "tahlil_step_transition",
          ) { step ->
            Column(
              modifier = Modifier
                .fillMaxSize()
                .verticalScroll(stepScrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              TahlilGuideStepCard(
                step = step,
                settings = settings,
                currentRepetition = stepCounts[step.id] ?: 0,
                activeArwah = activeArwah,
                isFocused = true,
                onIncrementRepetition = {
                  if (settings.hapticEnabled) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  }
                  onIncrementStepCount(step.id, step.repetitionTarget)
                  onSaveTahlilStep(currentStepIndex)
                },
                onResetRepetition = { onResetStepCount(step.id) },
                onOpenArwahList = onOpenArwahList,
              )
              Spacer(modifier = Modifier.height(8.dp))
            }
          }
        }

        // Bottom Previous / Next Reading Controls
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedButton(
                onClick = { jumpToStep(currentStepIndex - 1) },
                enabled = currentStepIndex > 0,
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("tahlil_prev_step_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = null,
                  modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp),
                )
                Text("Sebelumnya")
              }

              if (currentStepIndex < totalSteps - 1) {
                Button(
                  onClick = { jumpToStep(currentStepIndex + 1) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("tahlil_next_step_button"),
                ) {
                  Text("Berikutnya")
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                      .padding(start = 6.dp)
                      .size(18.dp),
                  )
                }
              } else {
                FilledTonalButton(
                  onClick = { jumpToStep(0) },
                  modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("tahlil_restart_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier
                      .padding(end = 6.dp)
                      .size(18.dp),
                  )
                  Text("Ulangi Awal")
                }
              }
            }
          }
        }
      } else {
        // Full Continuous Scroll View
        LazyColumn(
          state = scrollListState,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        ) {
          itemsIndexed(
            items = steps,
            key = { _, item -> item.id },
          ) { index, step ->
            TahlilGuideStepCard(
              step = step,
              settings = settings,
              currentRepetition = stepCounts[step.id] ?: 0,
              activeArwah = activeArwah,
              isFocused = index == currentStepIndex,
              onCardClick = { jumpToStep(index) },
              onIncrementRepetition = {
                if (settings.hapticEnabled) {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onIncrementStepCount(step.id, step.repetitionTarget)
                if (currentStepIndex != index) {
                  currentStepIndex = index
                  onSaveTahlilStep(index)
                }
              },
              onResetRepetition = { onResetStepCount(step.id) },
              onOpenArwahList = onOpenArwahList,
            )
          }
        }
      }
    }
  }

  if (showTocSheet) {
    TahlilTableOfContentsSheet(
      steps = steps,
      currentStepIndex = currentStepIndex,
      onSelectStep = { idx ->
        showTocSheet = false
        jumpToStep(idx)
      },
      onDismiss = { showTocSheet = false },
    )
  }
}

@Composable
private fun TahlilGuideStepCard(
  step: TahlilStep,
  settings: ReaderSettings,
  currentRepetition: Int,
  activeArwah: List<DeceasedPrayer>,
  isFocused: Boolean,
  onCardClick: (() -> Unit)? = null,
  onIncrementRepetition: () -> Unit,
  onResetRepetition: () -> Unit,
  onOpenArwahList: () -> Unit,
) {
  val completed = step.repetitionTarget > 1 && currentRepetition >= step.repetitionTarget

  Card(
    onClick = { onCardClick?.invoke() },
    enabled = onCardClick != null,
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
      disabledContainerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      disabledContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    border = BorderStroke(
      width = if (completed || isFocused || step.supportsDeceasedNames) 1.5.dp else 1.dp,
      color = when {
        completed -> MaterialTheme.colorScheme.primary
        step.supportsDeceasedNames -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
        isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
      },
    ),
    shape = MaterialTheme.shapes.large,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          VerseNumberMedallion(number = step.order, size = 38.dp)
          Column {
            Text(
              text = step.title,
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = step.arabicTitle,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = AmiriFontFamily),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        if (step.repetitionTarget > 1) {
          Surface(
            color = if (completed) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.secondaryContainer
            },
            shape = MaterialTheme.shapes.small,
          ) {
            Text(
              text = "${step.repetitionTarget}x",
              style = MaterialTheme.typography.labelLarge,
              color = if (completed) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSecondaryContainer
              },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      MushafSectionDivider()

      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
          text = step.arabic,
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = AmiriFontFamily,
            fontSize = settings.arabicFontSizeSp.sp,
            lineHeight = (settings.arabicFontSizeSp * 1.98f).sp,
          ),
          textAlign = TextAlign.Right,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      if (settings.showLatin) {
        Text(
          text = step.latin,
          style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      if (settings.showTranslation) {
        Text(
          text = step.translation,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (step.guidanceNote != null) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = step.guidanceNote,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }

      AnimatedVisibility(visible = step.supportsDeceasedNames) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
          shape = MaterialTheme.shapes.medium,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.Favorite,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "Khususon Arwah yang Didoakan",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
              }
              TextButton(onClick = onOpenArwahList) {
                Text(if (activeArwah.isEmpty()) "Tambah" else "Kelola")
              }
            }

            if (activeArwah.isEmpty()) {
              Text(
                text = "Belum ada nama Almarhum/Almarhumah aktif. Tambahkan melalui menu Almarhum List agar disebut langsung di doa ini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                activeArwah.forEachIndexed { idx, person ->
                  Text(
                    text = "${idx + 1}. ${person.formattedPrayerName()}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
              }
            }
          }
        }
      }

      if (step.repetitionTarget > 1) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = onIncrementRepetition,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("tahlil_counter_${step.id}"),
          ) {
            if (completed) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier
                  .padding(end = 8.dp)
                  .size(18.dp),
              )
              Text("Selesai ($currentRepetition / ${step.repetitionTarget})")
            } else {
              Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier
                  .padding(end = 6.dp)
                  .size(18.dp),
              )
              Text("Hitung Bacaan: $currentRepetition / ${step.repetitionTarget}")
            }
          }

          if (currentRepetition > 0) {
            OutlinedButton(
              onClick = onResetRepetition,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Ulangi hitungan ${step.title}",
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TahlilTableOfContentsSheet(
  steps: List<TahlilStep>,
  currentStepIndex: Int,
  onSelectStep: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    LazyColumn(
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp),
    ) {
      item {
        Text(
          text = "Daftar Isi Bacaan Tahlil",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "Pilih bagian untuk langsung melompat ke bacaan.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
      }

      TahlilGuidePhase.entries.forEach { phase ->
        val phaseSteps = steps.mapIndexed { idx, step -> idx to step }
          .filter { (_, step) -> step.order in phase.range }

        item(key = "phase_${phase.name}") {
          Text(
            text = phase.label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
          )
        }

        itemsIndexed(
          items = phaseSteps,
          key = { _, pair -> pair.second.id },
        ) { _, (globalIdx, step) ->
          val isCurrent = globalIdx == currentStepIndex
          Surface(
            color = if (isCurrent) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectStep(globalIdx) }
              .testTag("tahlil_toc_item_${step.id}"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                VerseNumberMedallion(number = step.order, size = 32.dp)
                Column {
                  Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  if (step.supportsDeceasedNames) {
                    Text(
                      text = "Menyertakan nama Arwah",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.secondary,
                    )
                  }
                }
              }
              if (step.repetitionTarget > 1) {
                Text(
                  text = "${step.repetitionTarget}x",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    }
  }
}
