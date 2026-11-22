package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TahlilAndSurahsData
import com.example.model.ReaderSettings
import com.example.ui.components.TasbihProgressRing
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.viewmodel.TasbihUiState

@Composable
fun TasbihScreen(
  tasbihUi: TasbihUiState,
  settings: ReaderSettings,
  onIncrement: () -> Unit,
  onResetCurrent: () -> Unit,
  onSelectPreset: (String) -> Unit,
  onSelectTarget: (Long) -> Unit,
  onToggleHaptic: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  var showResetConfirm by remember { mutableStateOf(false) }

  val selectedPreset = remember(tasbihUi.selectedDhikrId) {
    TahlilAndSurahsData.dhikrPresets.firstOrNull { it.id == tasbihUi.selectedDhikrId }
      ?: TahlilAndSurahsData.dhikrPresets.first()
  }

  val isTargetReached = tasbihUi.targetCount > 0L && tasbihUi.currentCount >= tasbihUi.targetCount
  val rawProgress = if (tasbihUi.targetCount > 0L) {
    (tasbihUi.currentCount.toFloat() / tasbihUi.targetCount.toFloat()).coerceIn(0f, 1f)
  } else {
    ((tasbihUi.currentCount % 33L).toFloat() / 33f)
  }
  val animatedProgress by animateFloatAsState(
    targetValue = rawProgress,
    animationSpec = spring(stiffness = 380f),
    label = "tasbih_progress",
  )

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 620.dp)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Header Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Tasbih Digital",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "Total sepanjang waktu: ${tasbihUi.lifetimeCount} dzikir",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = { onToggleHaptic(!settings.hapticEnabled) },
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("tasbih_haptic_toggle"),
        ) {
          Icon(
            imageVector = Icons.Filled.Vibration,
            contentDescription = if (settings.hapticEnabled) "Matikan getar" else "Aktifkan getar",
            tint = if (settings.hapticEnabled) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
          )
        }
      }

      // Dhikr Presets
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(TahlilAndSurahsData.dhikrPresets, key = { it.id }) { preset ->
          FilterChip(
            selected = preset.id == selectedPreset.id,
            onClick = { onSelectPreset(preset.id) },
            label = { Text(preset.title) },
            modifier = Modifier.testTag("dhikr_chip_${preset.id}"),
          )
        }
      }

      // Selected Dhikr Display Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
              text = selectedPreset.arabic,
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = AmiriFontFamily,
                fontSize = 30.sp,
                lineHeight = 52.sp,
              ),
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Text(
            text = selectedPreset.latin,
            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
          )
          Text(
            text = selectedPreset.meaning,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }

      // Tactile Circular Tap Ring
      Box(
        modifier = Modifier
          .size(250.dp)
          .clip(CircleShape)
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true),
            role = Role.Button,
            onClick = {
              if (settings.hapticEnabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              }
              onIncrement()
            },
          )
          .testTag("tasbih_tap_circle"),
        contentAlignment = Alignment.Center,
      ) {
        TasbihProgressRing(
          progress = animatedProgress,
          isTargetReached = isTargetReached,
          modifier = Modifier.fillMaxSize(),
        )
        Surface(
          shape = CircleShape,
          color = if (isTargetReached) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
          } else {
            MaterialTheme.colorScheme.surface
          },
          tonalElevation = 2.dp,
          modifier = Modifier.size(176.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = tasbihUi.currentCount.toString(),
              style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp),
              color = if (isTargetReached) {
                MaterialTheme.colorScheme.onSecondaryContainer
              } else {
                MaterialTheme.colorScheme.primary
              },
            )
            Text(
              text = if (tasbihUi.targetCount > 0L) {
                "dari ${tasbihUi.targetCount}"
              } else {
                "Tanpa Batas"
              },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Ketuk di sini",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      AnimatedVisibility(visible = isTargetReached) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = MaterialTheme.shapes.medium,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "Alhamdulillah, target ${tasbihUi.targetCount} tercapai",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }

      // Target Selector & Reset Row
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Target Hitungan",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          listOf(33L to "33", 99L to "99", 100L to "100", 0L to "Bebas").forEach { (value, label) ->
            FilterChip(
              selected = tasbihUi.targetCount == value,
              onClick = { onSelectTarget(value) },
              label = { Text(label) },
              modifier = Modifier.testTag("tasbih_target_$value"),
            )
          }
        }

        OutlinedButton(
          onClick = {
            if (tasbihUi.currentCount > 0L) {
              showResetConfirm = true
            }
          },
          enabled = tasbihUi.currentCount > 0L,
          modifier = Modifier.testTag("tasbih_reset_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier
              .padding(end = 6.dp)
              .size(18.dp),
          )
          Text("Ulangi Hitungan")
        }
      }
    }
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      title = { Text("Ulangi Hitungan Tasbih?") },
      text = { Text("Hitungan sesi saat ini akan kembali ke 0. Total dzikir sepanjang waktu Anda tetap tersimpan.") },
      confirmButton = {
        TextButton(
          onClick = {
            showResetConfirm = false
            onResetCurrent()
          }
        ) {
          Text("Ulangi")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) {
          Text("Batal")
        }
      },
    )
  }
}
