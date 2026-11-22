package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DeceasedPrayer
import com.example.model.ReaderSettings
import com.example.model.ReadingState
import com.example.model.UiState
import com.example.ui.components.MushafSectionDivider
import com.example.ui.theme.AmiriFontFamily

@Composable
fun HomeScreen(
  readingState: UiState<ReadingState>,
  deceasedState: UiState<List<DeceasedPrayer>>,
  settings: ReaderSettings,
  lifetimeTasbihCount: Long,
  onOpenYasin: (startVerse: Int) -> Unit,
  onOpenTahlil: (openShortSurahs: Boolean) -> Unit,
  onOpenTasbih: () -> Unit,
  onOpenArwahList: () -> Unit,
  onToggleNightMode: () -> Unit,
  onOpenDisplaySettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val reading = (readingState as? UiState.Success)?.data
  val lastVerse = (reading?.lastYasinVerse ?: 1L).toInt().coerceIn(1, 83)
  val bookmarkCount = reading?.bookmarkedVerses?.size ?: 0
  val activeArwah = (deceasedState as? UiState.Success)?.data?.filter { it.includedInPrayer } ?: emptyList()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 720.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Top Greeting & Action Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Assalamu'alaikum",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "Yasin & Tahlil Digital",
              style = MaterialTheme.typography.headlineSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onToggleNightMode,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("home_night_mode_button"),
            ) {
              Icon(
                imageVector = if (settings.nightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Ubah mode baca malam",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = onOpenDisplaySettings,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("home_display_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Pengaturan tampilan bacaan",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Hero Resume Banner
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
          shape = MaterialTheme.shapes.large,
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(196.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_yasin_banner),
              contentDescription = null,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(
                      Color(0xFF062C21).copy(alpha = 0.92f),
                      Color(0xFF0D5C46).copy(alpha = 0.72f),
                    )
                  )
                ),
            )
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
              verticalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Surface(
                    color = Color(0xFFF6E8C6).copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.small,
                  ) {
                    Text(
                      text = "Surah Yasin • Ayat $lastVerse dari 83",
                      style = MaterialTheme.typography.labelMedium,
                      color = Color(0xFFF6E8C6),
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "Lanjutkan Bacaan Yasin",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                  )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                  Text(
                    text = "سُوْرَةُ يٰسۤ",
                    style = MaterialTheme.typography.headlineSmall.copy(
                      fontFamily = AmiriFontFamily,
                      fontSize = 26.sp,
                    ),
                    color = Color(0xFFF6E8C6),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "$bookmarkCount ayat ditandai",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.85f),
                )
                Button(
                  onClick = { onOpenYasin(lastVerse) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE2BE67),
                    contentColor = Color(0xFF2B2004),
                  ),
                  modifier = Modifier.testTag("resume_yasin_button"),
                ) {
                  Text(
                    text = if (lastVerse > 1) "Ayat $lastVerse" else "Mulai Baca",
                    style = MaterialTheme.typography.labelLarge,
                  )
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                      .padding(start = 6.dp)
                      .size(18.dp),
                  )
                }
              }
            }
          }
        }
      }

      item { MushafSectionDivider() }

      // Quick Menu 2x2 Grid
      item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Daftar Bacaan & Ibadah",
            style = MaterialTheme.typography.titleLarge,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            HomeFeatureCard(
              title = "Surah Yasin",
              subtitle = "83 Ayat Lengkap",
              icon = Icons.Filled.AutoStories,
              accentContainer = MaterialTheme.colorScheme.primaryContainer,
              accentTint = MaterialTheme.colorScheme.onPrimaryContainer,
              onClick = { onOpenYasin(1) },
              modifier = Modifier
                .weight(1f)
                .testTag("home_card_yasin"),
            )
            HomeFeatureCard(
              title = "Tahlil & Doa",
              subtitle = "Susunan & Doa Arwah",
              icon = Icons.AutoMirrored.Filled.FormatListBulleted,
              accentContainer = MaterialTheme.colorScheme.secondaryContainer,
              accentTint = MaterialTheme.colorScheme.onSecondaryContainer,
              onClick = { onOpenTahlil(false) },
              modifier = Modifier
                .weight(1f)
                .testTag("home_card_tahlil"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            HomeFeatureCard(
              title = "Surah Pendek",
              subtitle = "Fatihah, Qulhu & Kursi",
              icon = Icons.AutoMirrored.Filled.MenuBook,
              accentContainer = MaterialTheme.colorScheme.tertiaryContainer,
              accentTint = MaterialTheme.colorScheme.onTertiaryContainer,
              onClick = { onOpenTahlil(true) },
              modifier = Modifier
                .weight(1f)
                .testTag("home_card_short_surahs"),
            )
            HomeFeatureCard(
              title = "Tasbih Digital",
              subtitle = "Total: $lifetimeTasbihCount dzikir",
              icon = Icons.Filled.TouchApp,
              accentContainer = MaterialTheme.colorScheme.primaryContainer,
              accentTint = MaterialTheme.colorScheme.onPrimaryContainer,
              onClick = onOpenTasbih,
              modifier = Modifier
                .weight(1f)
                .testTag("home_card_tasbih"),
            )
          }
        }
      }

      // Active Almarhum / Almarhumah Summary Card
      item {
        Card(
          onClick = onOpenArwahList,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("home_arwah_summary_card"),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.size(38.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Filled.People,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  }
                }
                Column {
                  Text(
                    text = "Daftar Nama Arwah",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "${activeArwah.size} nama disertakan dalam Doa Tahlil",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              TextButton(onClick = onOpenArwahList) {
                Text("Kelola")
              }
            }

            if (activeArwah.isEmpty()) {
              Text(
                text = "Tambahkan nama Almarhum/Almarhumah keluarga agar otomatis tampil saat membaca Tawasul & Doa Arwah.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                activeArwah.take(3).forEach { person ->
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Bookmark,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                      tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                      text = person.formattedPrayerName(),
                      style = MaterialTheme.typography.bodyMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                if (activeArwah.size > 3) {
                  Text(
                    text = "+${activeArwah.size - 3} nama lainnya",
                    style = MaterialTheme.typography.labelSmall,
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
}

@Composable
private fun HomeFeatureCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentContainer: Color,
  accentTint: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    shape = MaterialTheme.shapes.medium,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        color = accentContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentTint,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
