package com.flconverter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("FL Converter", style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConversionMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.mode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        enabled = !converting,
                        label = { Text(mode.label) }
                    )
                }
            }

            OutlinedButton(
                onClick = { fileHandler.pick(state.mode.source.extension, viewModel::onFilePicked) },
                enabled = !converting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select .${state.mode.source.extension} file")
            }

            Text(
                text = state.file?.name ?: "No file selected",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = {
                    viewModel.convert { name, bytes ->
                        fileHandler.save(name, bytes) { saved -> viewModel.onSaved(name, saved) }
                    }
                },
                enabled = state.file != null && !converting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (converting) "Converting..." else "Convert")
            }

            StatusMessage(state.status)
        }
    }
}

@Composable
private fun StatusMessage(status: ConversionStatus) {
    when (status) {
        is ConversionStatus.Saved -> Text(
            text = "Saved ${status.name}",
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        is ConversionStatus.Failed -> Text(
            text = status.message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        else -> Unit
    }
}
