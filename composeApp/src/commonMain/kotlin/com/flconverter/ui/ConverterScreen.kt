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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flconverter.domain.ConversionMode
import com.flconverter.platform.rememberFileHandler
import com.flconverter.presentation.ConversionStatus
import com.flconverter.presentation.ConverterViewModel

@Composable
fun ConverterScreen(viewModel: ConverterViewModel = viewModel { ConverterViewModel() }) {
    val state by viewModel.state.collectAsState()
    val fileHandler = rememberFileHandler()
    val converting = state.status is ConversionStatus.Converting
    val colors = MaterialTheme.colorScheme
    val source = state.mode.source
    val target = state.mode.target

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.surfaceVariant.copy(alpha = 0.55f), colors.background), endY = 900f))
    ) {
        AmbientNotes(Modifier.fillMaxSize())

        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Entrance(0) { Header(active = converting) }

                Entrance(1, Modifier.padding(top = 32.dp)) {
                    Section("1", "Direction") {
                        FlowCard(
                            mode = state.mode,
                            enabled = !converting,
                            onSwap = {
                                viewModel.selectMode(
                                    if (state.mode == ConversionMode.FlpToFlm) ConversionMode.FlmToFlp else ConversionMode.FlpToFlm
                                )
                            }
                        )
                    }
                }

                Entrance(2, Modifier.padding(top = 24.dp)) {
                    Section("2", "Project to convert") {
                        FileCard(
                            file = state.file,
                            badge = source.extension,
                            emptyTitle = "Select a .${source.extension} file",
                            emptyHint = "Tap to browse your files",
                            enabled = !converting,
                            onClick = { fileHandler.pick(source.extension, viewModel::onFilePicked) }
                        )
                    }
                }

                SummaryCard(state.summary)

                Entrance(3, Modifier.padding(top = 24.dp)) {
                    Section(
                        number = "3",
                        title = "Base project",
                        caption = "Receives the notes. Its instruments, mixer and effects are kept, its own notes are replaced."
                    ) {
                        FileCard(
                            file = state.base,
                            badge = target.extension,
                            emptyTitle = "Select a .${target.extension} base project",
                            emptyHint = "The ${target.description} that will receive the notes",
                            enabled = !converting,
                            onClick = { fileHandler.pick(target.extension, viewModel::onBasePicked) }
                        )
                    }
                }

                Entrance(4, Modifier.padding(top = 24.dp)) {
                    ConvertButton(
                        target = target.extension.uppercase(),
                        enabled = state.file != null && state.base != null,
                        converting = converting,
                        onClick = {
                            viewModel.convert { name, bytes ->
                                fileHandler.save(name, bytes) { saved -> viewModel.onSaved(name, saved) }
                            }
                        }
                    )
                }

                StatusCard(state.status)
            }
        }
    }
}

@Composable
private fun Header(active: Boolean) {
    val accent = MaterialTheme.colorScheme.primary

    Column(
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

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = accent)) { append("FL") }
                append(" Converter")
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
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
private fun Section(number: String, title: String, caption: String? = null, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
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
