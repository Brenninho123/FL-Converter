package com.flconverter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flconverter.presentation.ConversionStatus
import kotlin.math.min

@Composable
fun StatusCard(status: ConversionStatus) {
    val result = status.takeIf { it is ConversionStatus.Saved || it is ConversionStatus.Failed }

    AnimatedContent(
        targetState = result,
        transitionSpec = {
            (fadeIn(tween(250)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = 300f), initialScale = 0.92f) +
                expandVertically()) togetherWith fadeOut(tween(150))
        },
        label = "status"
    ) { current ->
        if (current != null) {
            Column(Modifier.padding(top = 24.dp)) {
                StatusContent(current)
            }
        }
    }
}

@Composable
private fun StatusContent(status: ConversionStatus) {
    val colors = MaterialTheme.colorScheme
    val success = status is ConversionStatus.Saved
    val container = if (success) colors.tertiaryContainer else colors.errorContainer
    val content = if (success) colors.onTertiaryContainer else colors.onErrorContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusIcon(success, content)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (success) "Conversion complete" else "Something went wrong",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
                Text(
                    text = when (status) {
                        is ConversionStatus.Saved -> "Saved ${status.name}. ${status.detail}"
                        is ConversionStatus.Failed -> status.message
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = content
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(success: Boolean, tint: Color) {
    val progress = remember(success) { Animatable(0f) }

    LaunchedEffect(success) {
        progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    Canvas(Modifier.size(32.dp)) {
        val side = size.width
        val stroke = side * 0.1f
        val p = progress.value

        drawCircle(color = tint.copy(alpha = 0.18f), radius = side / 2 * min(1f, p * 2f))

        if (success) {
            val a = Offset(side * 0.26f, side * 0.53f)
            val b = Offset(side * 0.44f, side * 0.7f)
            val c = Offset(side * 0.75f, side * 0.34f)
            drawLine(tint, a, lerp(a, b, min(1f, p * 2f)), stroke, StrokeCap.Round)
            if (p > 0.5f) {
                drawLine(tint, b, lerp(b, c, (p - 0.5f) * 2f), stroke, StrokeCap.Round)
            }
        } else {
            val a1 = Offset(side * 0.32f, side * 0.32f)
            val b1 = Offset(side * 0.68f, side * 0.68f)
            val a2 = Offset(side * 0.68f, side * 0.32f)
            val b2 = Offset(side * 0.32f, side * 0.68f)
            drawLine(tint, a1, lerp(a1, b1, min(1f, p * 2f)), stroke, StrokeCap.Round)
            if (p > 0.5f) {
                drawLine(tint, a2, lerp(a2, b2, (p - 0.5f) * 2f), stroke, StrokeCap.Round)
            }
        }
    }
}
