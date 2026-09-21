package com.flconverter.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class AmbientNote(val x: Float, val phase: Float, val speed: Float, val size: Float, val tilt: Float)

@Composable
fun AmbientNotes(modifier: Modifier = Modifier) {
    val notes = remember {
        val random = Random(7)
        List(14) {
            AmbientNote(
                x = random.nextFloat(),
                phase = random.nextFloat(),
                speed = 1f + random.nextInt(3),
                size = 22f + random.nextFloat() * 26f,
                tilt = random.nextFloat() * 40f - 20f
            )
        }
    }
    val time by rememberInfiniteTransition(label = "ambient").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(45000, easing = LinearEasing)),
        label = "ambientTime"
    )
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        val sway = 16.dp.toPx()
        notes.forEach { note ->
            val progress = (time * note.speed + note.phase) % 1f
            val visibility = sin(progress * PI.toFloat())
            drawEighthNote(
                center = Offset(
                    x = size.width * note.x + sin(progress * 2f * PI.toFloat() + note.phase * 6f) * sway,
                    y = size.height * (1.05f - progress * 1.1f)
                ),
                height = note.size.dp.toPx(),
                color = color.copy(alpha = 0.13f * visibility),
                angle = note.tilt + progress * 20f
            )
        }
    }
}

@Composable
fun Equalizer(active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    val bars = List(5) { index ->
        transition.animateFloat(
            initialValue = 0.22f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (active) 300 + index * 60 else 900 + index * 140,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 110)
            ),
            label = "bar$index"
        )
    }
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        val gap = size.width / (bars.size * 2f - 1f)
        bars.forEachIndexed { index, bar ->
            val height = size.height * bar.value
            drawRoundRect(
                color = color.copy(alpha = if (active) 1f else 0.55f),
                topLeft = Offset(index * gap * 2f, size.height - height),
                size = Size(gap, height),
                cornerRadius = CornerRadius(gap / 2f)
            )
        }
    }
}
