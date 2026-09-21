package com.flconverter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ActionBar(
    hint: String,
    target: String,
    enabled: Boolean,
    converting: Boolean,
    maxWidth: Dp,
    wide: Boolean,
    onConvert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth().background(colors.surface.copy(alpha = 0.96f))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.35f)))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val hintContent: @Composable (Modifier) -> Unit = { hintModifier ->
                AnimatedContent(
                    targetState = hint,
                    modifier = hintModifier,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    contentAlignment = if (wide) Alignment.CenterStart else Alignment.Center,
                    label = "hint"
                ) { text ->
                    Text(
                        text = text,
                        modifier = Modifier.fillMaxWidth(),
                        style = if (wide) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = if (wide) TextAlign.Start else TextAlign.Center
                    )
                }
            }

            if (wide) {
                Row(
                    modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth().padding(horizontal = 32.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    hintContent(Modifier.weight(1f))
                    Box(Modifier.width(320.dp)) { ConvertButton(target, enabled, converting, onConvert) }
                }
            } else {
                Column(
                    modifier = Modifier.widthIn(max = maxWidth).fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    hintContent(Modifier.fillMaxWidth())
                    ConvertButton(target, enabled, converting, onConvert)
                }
            }
        }
    }
}

@Composable
private fun ConvertButton(target: String, enabled: Boolean, converting: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().height(56.dp).pressScale(interaction),
        shape = RoundedCornerShape(16.dp)
    ) {
        AnimatedContent(
            targetState = converting,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "convertLabel"
        ) { isConverting ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isConverting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Converting...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(
                        text = "Convert to $target",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
