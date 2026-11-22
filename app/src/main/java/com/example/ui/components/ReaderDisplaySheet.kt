package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderSettings
import com.example.ui.theme.AmiriFontFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderDisplaySheet(
  settings: ReaderSettings,
  onFontSizeChange: (Float) -> Unit,
  onShowLatinChange: (Boolean) -> Unit,
  onShowTranslationChange: (Boolean) -> Unit,
  onNightModeChange: (Boolean) -> Unit,
  onHapticChange: (Boolean) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "Pengaturan Tampilan Bacaan",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth(),
      ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          Text(
            text = "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 14.dp, horizontal = 16.dp),
            style = MaterialTheme.typography.headlineSmall.copy(
              fontFamily = AmiriFontFamily,
              fontSize = settings.arabicFontSizeSp.sp,
              lineHeight = (settings.arabicFontSizeSp * 1.9f).sp,
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Ukuran Teks Arab",
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = "${settings.arabicFontSizeSp.roundToInt()} sp",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = settings.arabicFontSizeSp,
          onValueChange = onFontSizeChange,
          valueRange = 20f..38f,
          modifier = Modifier.testTag("arabic_font_slider"),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

      SettingToggleRow(
        title = "Tampilkan Tulisan Latin",
        subtitle = "Transliterasi latin di bawah teks Arab",
        checked = settings.showLatin,
        onCheckedChange = onShowLatinChange,
        tag = "toggle_show_latin",
      )

      SettingToggleRow(
        title = "Tampilkan Terjemahan",
        subtitle = "Arti bahasa Indonesia di setiap ayat & doa",
        checked = settings.showTranslation,
        onCheckedChange = onShowTranslationChange,
        tag = "toggle_show_translation",
      )

      SettingToggleRow(
        title = "Mode Malam (Yasinan Malam)",
        subtitle = "Latar gelap nyaman untuk membaca di malam hari",
        checked = settings.nightMode,
        onCheckedChange = onNightModeChange,
        tag = "toggle_night_mode",
      )

      SettingToggleRow(
        title = "Getar Tasbih (Haptik)",
        subtitle = "Getaran lembut setiap ketukan dzikir",
        checked = settings.hapticEnabled,
        onCheckedChange = onHapticChange,
        tag = "toggle_haptic",
      )

      Spacer(modifier = Modifier.height(6.dp))
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  tag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(tag),
    )
  }
}
