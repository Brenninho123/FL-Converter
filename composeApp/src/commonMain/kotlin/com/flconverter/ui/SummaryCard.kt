package com.flconverter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flconverter.domain.Note
import com.flconverter.domain.SongSummary
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun SummaryCard(summary: SongSummary?) {
    AnimatedContent(
        targetState = summary,
        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
        label = "summary"
    ) { current ->
        if (current != null) {
            Column(Modifier.padding(top = 24.dp)) {
                SummaryContent(current)
            }
        }
    }
}

@Composable
private fun SummaryContent(summary: SongSummary) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PROJECT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val tempo = rememberCountUp((summary.tempo * 10).roundToInt())
                Stat("BPM", formatTempo(tempo), Modifier.weight(1f))
                Stat("Patterns", rememberCountUp(summary.patternCount).toString(), Modifier.weight(1f))
                Stat("Notes", rememberCountUp(summary.noteCount).toString(), Modifier.weight(1f))
            }

            if (summary.previewNotes.isNotEmpty()) {
                NotePreview(summary.previewNotes)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotePreview(notes: List<Note>) {
    val progress = remember(notes) { Animatable(0f) }
    val barColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(notes) {
        progress.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val minKey = notes.minOf { it.key }
        val maxKey = notes.maxOf { it.key }
        val keyCount = maxKey - minKey + 1
        val end = max(notes.maxOf { it.position + it.length }, 1L).toFloat()

        val inset = 8.dp.toPx()
        val width = size.width - inset * 2
        val height = size.height - inset * 2
        val rowHeight = min(height / keyCount, 14.dp.toPx())
        val top = inset + (height - rowHeight * keyCount) / 2
        val reveal = progress.value * size.width

        notes.forEach { note ->
            val x = inset + width * note.position / end
            if (x < reveal) {
                val noteWidth = max(width * note.length / end, 4.dp.toPx())
                val noteHeight = max(rowHeight * 0.85f, 3.dp.toPx())
                val y = top + (maxKey - note.key) * rowHeight
                val velocity = note.velocity.coerceIn(0, 127) / 127f
                drawRoundRect(
                    color = barColor.copy(alpha = 0.45f + 0.55f * velocity),
                    topLeft = Offset(x, y),
                    size = Size(min(noteWidth, reveal - x), noteHeight),
                    cornerRadius = CornerRadius(1.5.dp.toPx())
                )
            }
        }

        if (progress.value < 1f) {
            drawLine(
                color = barColor.copy(alpha = 0.8f),
                start = Offset(reveal, 0f),
                end = Offset(reveal, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun formatTempo(tenths: Int): String =
    if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
