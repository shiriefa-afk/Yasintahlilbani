package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TahlilAndSurahsData
import com.example.model.ReaderSettings
import com.example.ui.components.VerseCard
import com.example.ui.theme.AmiriFontFamily

@Composable
fun ShortSurahsScreen(
  settings: ReaderSettings,
  onOpenDisplaySettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedShortSurahId by rememberSaveable {
    mutableStateOf(TahlilAndSurahsData.shortSurahs.first().id)
  }

  val selectedSurah = TahlilAndSurahsData.shortSurahs.firstOrNull {
    it.id == selectedShortSurahId
  } ?: TahlilAndSurahsData.shortSurahs.first()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Surah Pendek & Ayat Kursi",
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = "Al-Fatihah, Ayat Kursi, Al-Ikhlas, Al-Falaq, An-Nas",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onOpenDisplaySettings,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("short_surahs_display_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "Pengaturan tampilan Surah Pendek",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }

      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
      ) {
        items(TahlilAndSurahsData.shortSurahs, key = { it.id }) { surah ->
          FilterChip(
            selected = surah.id == selectedSurah.id,
            onClick = { selectedShortSurahId = surah.id },
            label = { Text(surah.titleLatin) },
            modifier = Modifier.testTag("short_surah_chip_${surah.id}"),
          )
        }
      }

      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        item(key = "short_surah_header_${selectedSurah.id}") {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            shape = MaterialTheme.shapes.large,
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                  text = selectedSurah.titleArabic,
                  style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = AmiriFontFamily,
                    fontSize = 28.sp,
                  ),
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Text(
                text = selectedSurah.titleLatin,
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = selectedSurah.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        items(
          items = selectedSurah.verses,
          key = { "${selectedSurah.id}_${it.number}" },
        ) { verse ->
          VerseCard(
            verse = verse,
            settings = settings,
            isBookmarked = false,
            isLastRead = false,
            onToggleBookmark = null,
          )
        }
      }
    }
  }
}
