package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReaderSettings
import com.example.model.SurahVerse
import com.example.ui.theme.AmiriFontFamily

@Composable
fun VerseCard(
  verse: SurahVerse,
  settings: ReaderSettings,
  isBookmarked: Boolean,
  isLastRead: Boolean,
  onToggleBookmark: (() -> Unit)?,
  onCardTap: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val borderColor = when {
    isLastRead -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    isBookmarked -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
  }

  Card(
    onClick = { onCardTap?.invoke() },
    enabled = onCardTap != null,
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = if (isLastRead) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
      } else {
        MaterialTheme.colorScheme.surface
      },
      disabledContainerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      disabledContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    border = BorderStroke(if (isLastRead || isBookmarked) 1.5.dp else 1.dp, borderColor),
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          VerseNumberMedallion(number = verse.number)
          if (isLastRead) {
            Text(
              text = "Terakhir Dibaca",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        if (onToggleBookmark != null) {
          IconButton(
            onClick = onToggleBookmark,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("bookmark_verse_${verse.number}"),
          ) {
            Icon(
              imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
              contentDescription = if (isBookmarked) "Hapus penanda ayat ${verse.number}" else "Tandai ayat ${verse.number}",
              tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
          text = verse.arabic,
          modifier = Modifier.fillMaxWidth(),
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = AmiriFontFamily,
            fontSize = settings.arabicFontSizeSp.sp,
            lineHeight = (settings.arabicFontSizeSp * 1.95f).sp,
          ),
          textAlign = TextAlign.Right,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      if (settings.showLatin) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = verse.latin,
          style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
          color = MaterialTheme.colorScheme.primary,
        )
      }

      if (settings.showTranslation) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = verse.translation,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
