package com.flconverter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flconverter.platform.PickedFile

@Composable
fun FileCard(file: PickedFile?, extension: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }

    val phase by rememberInfiniteTransition(label = "dash").animateFloat(
        initialValue = 0f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing)),
        label = "phase"
    )
    val dashAlpha by animateFloatAsState(if (file == null) 1f else 0f, tween(300), label = "dashAlpha")
    val solid by animateColorAsState(
        targetValue = if (file == null) Color.Transparent else colors.primary,
        animationSpec = tween(300),
        label = "solid"
    )
    val outline = colors.outline

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .pressScale(interaction)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .drawWithContent {
                drawContent()
                val radius = CornerRadius(20.dp.toPx())
                val width = 2.dp.toPx()
                if (dashAlpha > 0f) {
                    drawRoundRect(
                        color = outline.copy(alpha = dashAlpha),
                        cornerRadius = radius,
                        style = Stroke(
                            width = width,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), phase)
                        )
                    )
                }
                drawRoundRect(color = solid, cornerRadius = radius, style = Stroke(width = width))
            },
        shape = shape,
        color = colors.surface
    ) {
        AnimatedContent(
            targetState = file,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "fileContent"
        ) { current ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (current == null) "+" else extension.uppercase(),
                        style = if (current == null) {
                            MaterialTheme.typography.headlineSmall
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = current?.name ?: "Select a .$extension file",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (current == null) {
                            "Tap to browse your files"
                        } else {
                            "${formatSize(current.bytes.size)} · Tap to change"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatSize(bytes: Int): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> {
        val tenths = bytes * 10L / 1024
        "${tenths / 10}.${tenths % 10} KB"
    }
    else -> {
        val tenths = bytes * 10L / (1024 * 1024)
        "${tenths / 10}.${tenths % 10} MB"
    }
}
