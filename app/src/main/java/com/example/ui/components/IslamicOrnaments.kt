package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VerseNumberMedallion(
  number: Int,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  ringColor: Color = MaterialTheme.colorScheme.secondary,
  fillColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
  textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val side = this.size.minDimension * 0.64f
      val topLeft = Offset((this.size.width - side) / 2f, (this.size.height - side) / 2f)
      val squareSize = Size(side, side)
      val stroke = Stroke(width = 1.6.dp.toPx())

      // Outer 8-pointed Rub el Hizb star (two overlapping rotated squares)
      drawRect(color = fillColor, topLeft = topLeft, size = squareSize)
      drawRect(color = ringColor, topLeft = topLeft, size = squareSize, style = stroke)
      rotate(degrees = 45f) {
        drawRect(color = fillColor, topLeft = topLeft, size = squareSize)
        drawRect(color = ringColor, topLeft = topLeft, size = squareSize, style = stroke)
      }
      drawCircle(
        color = fillColor.copy(alpha = 0.9f),
        radius = this.size.minDimension * 0.30f,
      )
    }
    Text(
      text = number.toString(),
      style = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = if (number >= 100) 10.sp else 12.sp,
      ),
      color = textColor,
    )
  }
}

@Composable
fun MushafSectionDivider(
  modifier: Modifier = Modifier,
  accentColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
) {
  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(20.dp),
  ) {
    val midY = size.height / 2f
    val midX = size.width / 2f
    val diamondHalf = 5.dp.toPx()

    drawLine(
      color = accentColor.copy(alpha = 0.35f),
      start = Offset(16.dp.toPx(), midY),
      end = Offset(midX - 18.dp.toPx(), midY),
      strokeWidth = 1.dp.toPx(),
    )
    drawLine(
      color = accentColor.copy(alpha = 0.35f),
      start = Offset(midX + 18.dp.toPx(), midY),
      end = Offset(size.width - 16.dp.toPx(), midY),
      strokeWidth = 1.dp.toPx(),
    )

    rotate(degrees = 45f, pivot = Offset(midX, midY)) {
      drawRect(
        color = accentColor,
        topLeft = Offset(midX - diamondHalf, midY - diamondHalf),
        size = Size(diamondHalf * 2f, diamondHalf * 2f),
      )
    }
  }
}

@Composable
fun TasbihProgressRing(
  progress: Float,
  isTargetReached: Boolean,
  modifier: Modifier = Modifier,
  trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
  progressColor: Color = MaterialTheme.colorScheme.primary,
  goldColor: Color = MaterialTheme.colorScheme.secondary,
) {
  Canvas(modifier = modifier) {
    val strokeWidth = 14.dp.toPx()
    val diameter = size.minDimension - strokeWidth * 2
    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
    val arcSize = Size(diameter, diameter)
    val radius = diameter / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Subtle bead dots around the ring (33 beads)
    val beadCount = 33
    for (i in 0 until beadCount) {
      val angleRad = Math.toRadians((-90.0 + (360.0 / beadCount) * i))
      val bx = center.x + (radius + strokeWidth * 0.9f) * cos(angleRad).toFloat()
      val by = center.y + (radius + strokeWidth * 0.9f) * sin(angleRad).toFloat()
      val lit = (i.toFloat() / beadCount) < progress
      drawCircle(
        color = when {
          isTargetReached -> goldColor
          lit -> progressColor.copy(alpha = 0.85f)
          else -> trackColor
        },
        radius = if (lit) 3.dp.toPx() else 2.dp.toPx(),
        center = Offset(bx, by),
      )
    }

    drawArc(
      color = trackColor,
      startAngle = 0f,
      sweepAngle = 360f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    drawArc(
      color = if (isTargetReached) goldColor else progressColor,
      startAngle = -90f,
      sweepAngle = (progress.coerceIn(0f, 1f) * 360f),
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )
  }
}
