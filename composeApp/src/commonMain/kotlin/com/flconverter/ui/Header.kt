package com.flconverter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun Header(compact: Boolean, active: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = compact,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(180)) },
        contentAlignment = Alignment.TopCenter,
        label = "header"
    ) { isCompact ->
        if (isCompact) CompactHeader(active) else FullHeader(active)
    }
}

@Composable
private fun FullHeader(active: Boolean) {
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(160.dp).drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.3f), Color.Transparent),
                            center = center,
                            radius = size.minDimension / 2
                        )
                    )
                }
            )
            AppLogo(Modifier.size(92.dp), active = active)
        }

        Title(MaterialTheme.typography.headlineLarge)
        Text(
            text = "Move your notes between FL Studio and FL Studio Mobile",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Equalizer(active, Modifier.width(44.dp).height(20.dp))
    }
}

@Composable
private fun CompactHeader(active: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppLogo(Modifier.size(52.dp), active = active)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Title(MaterialTheme.typography.headlineSmall)
            Text(
                text = "Notes between FL Studio and FL Studio Mobile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        Equalizer(active, Modifier.width(32.dp).height(16.dp))
    }
}

@Composable
private fun Title(style: TextStyle) {
    val accent = MaterialTheme.colorScheme.primary

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = accent)) { append("FL") }
            append(" Converter")
        },
        style = style,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}
