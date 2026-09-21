package com.flconverter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flconverter.domain.ConversionMode
import com.flconverter.platform.PickedFile
import com.flconverter.platform.rememberFileHandler
import com.flconverter.presentation.ConversionStatus
import com.flconverter.presentation.ConverterViewModel

@Composable
fun ConverterScreen(viewModel: ConverterViewModel = viewModel { ConverterViewModel() }) {
    val state by viewModel.state.collectAsState()
    val fileHandler = rememberFileHandler()
    val converting = state.status is ConversionStatus.Converting

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Header()

            Section("Conversion") {
                ModeSelector(
                    selected = state.mode,
                    enabled = !converting,
                    onSelect = viewModel::selectMode
                )
            }

            Section("File") {
                FileCard(
                    file = state.file,
                    extension = state.mode.source.extension,
                    enabled = !converting,
                    onClick = { fileHandler.pick(state.mode.source.extension, viewModel::onFilePicked) }
                )
            }

            ConvertButton(
                target = state.mode.target.extension.uppercase(),
                enabled = state.file != null && !converting,
                converting = converting,
                onClick = {
                    viewModel.convert { name, bytes ->
                        fileHandler.save(name, bytes) { saved -> viewModel.onSaved(name, saved) }
                    }
                }
            )

            StatusCard(state.status)
        }
    }
}

@Composable
private fun Header() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppLogo(Modifier.size(88.dp))
        Text(
            text = "FL Converter",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Convert FL Studio projects between FLP and FLM",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun ModeSelector(selected: ConversionMode, enabled: Boolean, onSelect: (ConversionMode) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ConversionMode.entries.forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .selectable(
                            selected = isSelected,
                            enabled = enabled,
                            role = Role.Tab,
                            onClick = { onSelect(mode) }
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileCard(file: PickedFile?, extension: String, enabled: Boolean, onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(20.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(
                if (file == null) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = outline,
                            cornerRadius = CornerRadius(20.dp.toPx()),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
                            )
                        )
                    }
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = if (file != null) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (file == null) "+" else extension.uppercase(),
                    style = if (file == null) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.labelLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = file?.name ?: "Select a .$extension file",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (file == null) "Tap to browse your files" else "${formatSize(file.bytes.size)} · Tap to change",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConvertButton(target: String, enabled: Boolean, converting: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled || converting,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (converting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(12.dp))
            Text("Converting...", style = MaterialTheme.typography.titleMedium)
        } else {
            Text("Convert to $target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusCard(status: ConversionStatus) {
    AnimatedVisibility(visible = status is ConversionStatus.Saved || status is ConversionStatus.Failed) {
        val success = status is ConversionStatus.Saved
        val container = if (success) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
        val content = if (success) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = container
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (success) "Conversion complete" else "Something went wrong",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
                Text(
                    text = when (status) {
                        is ConversionStatus.Saved -> "Saved ${status.name}"
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
