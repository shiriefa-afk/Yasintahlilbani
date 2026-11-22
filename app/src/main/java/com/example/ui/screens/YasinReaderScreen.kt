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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SurahYasinData
import com.example.model.ReaderSettings
import com.example.model.ReadingState
import com.example.model.UiState
import com.example.ui.components.MushafSectionDivider
import com.example.ui.components.VerseCard
import com.example.ui.theme.AmiriFontFamily

@Composable
fun YasinReaderScreen(
  initialTargetVerse: Int?,
  readingState: UiState<ReadingState>,
  settings: ReaderSettings,
  onSaveLastReadVerse: (Int) -> Unit,
  onToggleBookmark: (Int) -> Unit,
  onOpenDisplaySettings: () -> Unit,
  onConsumeTargetVerse: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val reading = (readingState as? UiState.Success)?.data
  val bookmarkedSet = reading?.bookmarkedVerses?.map { it.toInt() }?.toSet() ?: emptySet()
  val lastReadVerse = (reading?.lastYasinVerse ?: 1L).toInt()

  var showBookmarkedOnly by remember { mutableStateOf(false) }
  var pendingJumpVerse by remember { mutableStateOf<Int?>(null) }
  val listState = rememberLazyListState()

  val displayedVerses = remember(showBookmarkedOnly, bookmarkedSet) {
    if (showBookmarkedOnly) {
      SurahYasinData.verses.filter { it.number in bookmarkedSet }
    } else {
      SurahYasinData.verses
    }
  }

  LaunchedEffect(initialTargetVerse, pendingJumpVerse, showBookmarkedOnly) {
    val target = initialTargetVerse ?: pendingJumpVerse
    if (target != null && target in 1..83) {
      if (showBookmarkedOnly) {
        showBookmarkedOnly = false
      } else {
        // Index 0 is Surah Header Card, so verse `n` is at index `n`
        listState.animateScrollToItem(target)
        if (initialTargetVerse != null) {
          onConsumeTargetVerse()
        }
        pendingJumpVerse = null
      }
    }
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
      // Compact Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = SurahYasinData.SURAH_NAME,
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = SurahYasinData.SURAH_SUBTITLE,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = onOpenDisplaySettings,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("yasin_display_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "Pengaturan tampilan Surah Yasin",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
      }

      // Quick Filter & Jump Chips
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
      ) {
        item {
          FilterChip(
            selected = !showBookmarkedOnly,
            onClick = { showBookmarkedOnly = false },
            label = { Text("Semua (83)") },
            modifier = Modifier.testTag("filter_all_yasin"),
          )
        }
        item {
          FilterChip(
            selected = showBookmarkedOnly,
            onClick = { showBookmarkedOnly = true },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
              )
            },
            label = { Text("Ditandai (${bookmarkedSet.size})") },
            modifier = Modifier.testTag("filter_bookmarked_yasin"),
          )
        }
        if (lastReadVerse > 1) {
          item {
            FilterChip(
              selected = false,
              onClick = { pendingJumpVerse = lastReadVerse },
              label = { Text("Terakhir: Ayat $lastReadVerse") },
              modifier = Modifier.testTag("jump_last_read_yasin"),
            )
          }
        }
        val jumpMarks = listOf(1, 20, 36, 58, 70, 83)
        items(jumpMarks) { jump ->
          FilterChip(
            selected = false,
            onClick = { pendingJumpVerse = jump },
            label = { Text("Ayat $jump") },
          )
        }
      }

      if (showBookmarkedOnly && displayedVerses.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Bookmark,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
              text = "Belum ada ayat yang ditandai",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = "Ketuk ikon penanda pada ayat mana saja untuk menyimpannya di sini.",
              style = MaterialTheme.typography.bodySmall,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      } else {
        LazyColumn(
          state = listState,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          if (!showBookmarkedOnly) {
            item(key = "yasin_bismillah_header") {
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
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                      text = SurahYasinData.SURAH_ARABIC_NAME,
                      style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = AmiriFontFamily,
                        fontSize = 30.sp,
                      ),
                      color = MaterialTheme.colorScheme.primary,
                    )
                    MushafSectionDivider()
                    Text(
                      text = SurahYasinData.BISMILLAH_ARABIC,
                      style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AmiriFontFamily,
                        fontSize = (settings.arabicFontSizeSp + 2f).sp,
                      ),
                      textAlign = TextAlign.Center,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                  if (settings.showLatin) {
                    Text(
                      text = SurahYasinData.BISMILLAH_LATIN,
                      style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  if (settings.showTranslation) {
                    Text(
                      text = SurahYasinData.BISMILLAH_TRANSLATION,
                      style = MaterialTheme.typography.bodySmall,
                      textAlign = TextAlign.Center,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    text = "Ketuk kartu ayat untuk menandai batas bacaan terakhir.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }

          items(
            items = displayedVerses,
            key = { it.number },
          ) { verse ->
            VerseCard(
              verse = verse,
              settings = settings,
              isBookmarked = verse.number in bookmarkedSet,
              isLastRead = verse.number == lastReadVerse,
              onToggleBookmark = { onToggleBookmark(verse.number) },
              onCardTap = { onSaveLastReadVerse(verse.number) },
            )
          }
        }
      }
    }
  }
}
