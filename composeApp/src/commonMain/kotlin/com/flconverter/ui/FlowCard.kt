package com.flconverter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flconverter.domain.ConversionMode
import com.flconverter.domain.FlFormat

@Composable
fun FlowCard(mode: ConversionMode, enabled: Boolean, onSwap: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val rotation by animateFloatAsState(
        targetValue = if (mode == ConversionMode.FlpToFlm) 0f else 180f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 260f),
        label = "swapRotation"
    )

    Row(
        modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FormatBadge(label = "FROM", format = mode.source, modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(48.dp)
                .pressScale(interaction, 0.9f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onSwap
                ),
            contentAlignment = Alignment.Center
        ) {
            SwapIcon(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }
            )
        }

        FormatBadge(label = "TO", format = mode.target, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FormatBadge(label: String, format: FlFormat, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant
            )
            AnimatedContent(
                targetState = format,
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(260)) { -it / 2 }) togetherWith
                        (fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 2 })
                },
                label = "badge$label"
            ) { current ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = current.extension.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                    Text(
                        text = current.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun SwapIcon(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.width * 0.09f
        val head = size.width * 0.16f
        val left = size.width * 0.08f
        val right = size.width * 0.92f
        val top = size.height * 0.33f
        val bottom = size.height * 0.67f

        drawLine(color, Offset(left, top), Offset(right, top), stroke, StrokeCap.Round)
        drawLine(color, Offset(right - head, top - head), Offset(right, top), stroke, StrokeCap.Round)
        drawLine(color, Offset(right - head, top + head), Offset(right, top), stroke, StrokeCap.Round)

        drawLine(color, Offset(right, bottom), Offset(left, bottom), stroke, StrokeCap.Round)
        drawLine(color, Offset(left + head, bottom - head), Offset(left, bottom), stroke, StrokeCap.Round)
        drawLine(color, Offset(left + head, bottom + head), Offset(left, bottom), stroke, StrokeCap.Round)
    }
}
