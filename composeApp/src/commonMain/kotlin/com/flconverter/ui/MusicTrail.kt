package com.flconverter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.TimeSource

private const val LIFETIME_MILLIS = 1000L
private const val MAX_NOTES = 36

private class TrailNote(
    val x: Float,
    val y: Float,
    val born: Long,
    val angle: Float,
    val size: Float,
    val accent: Boolean
)

@Composable
fun MusicTrail(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val notes = remember { mutableStateListOf<TrailNote>() }
    val origin = remember { TimeSource.Monotonic.markNow() }
    var frame by remember { mutableLongStateOf(0L) }
    var lastSpawn by remember { mutableLongStateOf(0L) }
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val soft = colors.onSurfaceVariant

    LaunchedEffect(notes.isNotEmpty()) {
        while (notes.isNotEmpty()) {
            androidx.compose.runtime.withFrameMillis {
                frame = origin.elapsedNow().inWholeMilliseconds
                notes.removeAll { note -> frame - note.born > LIFETIME_MILLIS }
            }
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            val spacing = 26.dp.toPx()
            val size = 20.dp.toPx()
            var last = Offset.Unspecified

            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull() ?: continue

                    when (event.type) {
                        PointerEventType.Press, PointerEventType.Move -> {
                            val position = change.position
                            val now = origin.elapsedNow().inWholeMilliseconds
                            val far = last == Offset.Unspecified || (position - last).getDistance() > spacing
                            if (far && now - lastSpawn > 45) {
                                last = position
                                lastSpawn = now
                                if (notes.size >= MAX_NOTES) notes.removeAt(0)
                                notes.add(
                                    TrailNote(
                                        x = position.x,
                                        y = position.y,
                                        born = now,
                                        angle = Random.nextFloat() * 50f - 25f,
                                        size = size * (0.8f + Random.nextFloat() * 0.5f),
                                        accent = Random.nextFloat() > 0.35f
                                    )
                                )
                            }
                        }
                        PointerEventType.Release, PointerEventType.Exit -> last = Offset.Unspecified
                        else -> Unit
                    }
                }
            }
        }
    ) {
        content()

        Canvas(Modifier.fillMaxSize()) {
            val rise = 46.dp.toPx()
            val sway = 8.dp.toPx()
            notes.forEach { note ->
                val progress = ((frame - note.born).coerceAtLeast(0L).toFloat() / LIFETIME_MILLIS).coerceIn(0f, 1f)
                val fade = (1f - progress) * (1f - progress)
                val base: Color = if (note.accent) accent else soft
                drawEighthNote(
                    center = Offset(
                        x = note.x + sin(progress * 6f + note.angle) * sway,
                        y = note.y - progress * rise
                    ),
                    height = note.size * (1f - progress * 0.25f),
                    color = base.copy(alpha = fade * 0.9f),
                    angle = note.angle + progress * 30f
                )
            }
        }
    }
}
