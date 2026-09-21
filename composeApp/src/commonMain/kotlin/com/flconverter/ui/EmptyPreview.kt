package com.flconverter.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private class GhostNote(val x: Float, val row: Int, val width: Float, val channel: Int)

@Composable
fun EmptyPreview(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val notes = remember {
        val random = Random(11)
        var cursor = 0f
        List(46) {
            cursor += 0.012f + random.nextFloat() * 0.03f
            GhostNote(cursor % 1f, 3 + random.nextInt(9), 0.02f + random.nextFloat() * 0.05f, random.nextInt(4))
        }
    }
    val time by rememberInfiniteTransition(label = "ghost").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(14000, easing = LinearEasing)),
        label = "ghostTime"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant)
            ) {
                val rows = 14
                val rowHeight = size.height / rows
                for (row in 1 until rows step 2) {
                    drawRect(
                        color = colors.onSurfaceVariant.copy(alpha = 0.05f),
                        topLeft = Offset(0f, row * rowHeight),
                        size = Size(size.width, rowHeight)
                    )
                }
                notes.forEach { note ->
                    val x = ((note.x - time) % 1f + 1f) % 1f
                    drawRoundRect(
                        color = channelColor(note.channel).copy(alpha = 0.32f),
                        topLeft = Offset(x * size.width, note.row * rowHeight + rowHeight * 0.1f),
                        size = Size(note.width * size.width, rowHeight * 0.8f),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }

            Text(
                text = "Your project appears here",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Text(
                text = "Pick a project to see its tempo, notes and channels before you convert.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
