package com.flconverter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flconverter.domain.ConversionMode
import com.flconverter.platform.FileHandler
import com.flconverter.platform.rememberFileHandler
import com.flconverter.presentation.ConversionStatus
import com.flconverter.presentation.ConverterState
import com.flconverter.presentation.ConverterViewModel

private val CompactWidth = 520.dp
private val WideWidth = 1120.dp
private val WideBreakpoint = 880.dp

@Composable
fun ConverterScreen(viewModel: ConverterViewModel = viewModel { ConverterViewModel() }) {
    val state by viewModel.state.collectAsState()
    val fileHandler = rememberFileHandler()
    var baseExpanded by remember { mutableStateOf(false) }
    val converting = state.status is ConversionStatus.Converting
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.surfaceVariant.copy(alpha = 0.55f), colors.background), endY = 900f))
    ) {
        val wide = maxWidth >= WideBreakpoint

        AmbientNotes(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (wide) {
                    WideLayout(state, viewModel, fileHandler, converting, baseExpanded) { baseExpanded = !baseExpanded }
                } else {
                    CompactLayout(state, viewModel, fileHandler, converting, baseExpanded) { baseExpanded = !baseExpanded }
                }
            }

            ActionBar(
                hint = hintFor(state),
                target = state.mode.target.extension.uppercase(),
                enabled = state.canConvert || converting,
                converting = converting,
                maxWidth = if (wide) WideWidth else CompactWidth,
                wide = wide,
                onConvert = {
                    viewModel.convert { name, bytes ->
                        fileHandler.save(name, bytes) { saved -> viewModel.onSaved(name, saved) }
                    }
                }
            )
        }
    }
}

@Composable
private fun CompactLayout(
    state: ConverterState,
    viewModel: ConverterViewModel,
    fileHandler: FileHandler,
    converting: Boolean,
    baseExpanded: Boolean,
    onToggleBase: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = CompactWidth)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Entrance(0) { Header(compact = state.file != null, active = converting) }
        Entrance(1, Modifier.padding(top = 28.dp)) { DirectionBlock(state, viewModel, converting) }
        Entrance(2, Modifier.padding(top = 24.dp)) { FileBlock(state, viewModel, fileHandler, converting) }
        SummaryCard(state.summary, state.excluded, viewModel::toggleChannel)
        Entrance(3, Modifier.padding(top = 24.dp)) {
            BaseBlock(state, viewModel, fileHandler, converting, baseExpanded, onToggleBase)
        }
        StatusCard(state.status)
    }
}

@Composable
private fun WideLayout(
    state: ConverterState,
    viewModel: ConverterViewModel,
    fileHandler: FileHandler,
    converting: Boolean,
    baseExpanded: Boolean,
    onToggleBase: () -> Unit
) {
    Row(
        modifier = Modifier.widthIn(max = WideWidth).fillMaxSize().padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 32.dp)
        ) {
            Entrance(0) { Header(compact = true, active = converting) }
            Entrance(1, Modifier.padding(top = 32.dp)) { DirectionBlock(state, viewModel, converting) }
            Entrance(2, Modifier.padding(top = 24.dp)) { FileBlock(state, viewModel, fileHandler, converting) }
            Entrance(3, Modifier.padding(top = 24.dp)) {
                BaseBlock(state, viewModel, fileHandler, converting, baseExpanded, onToggleBase)
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 32.dp)
        ) {
            if (state.summary == null) {
                Entrance(2, Modifier.padding(top = 24.dp)) { EmptyPreview() }
            } else {
                SummaryCard(state.summary, state.excluded, viewModel::toggleChannel)
            }
            StatusCard(state.status)
        }
    }
}

@Composable
private fun DirectionBlock(state: ConverterState, viewModel: ConverterViewModel, converting: Boolean) {
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

@Composable
private fun FileBlock(state: ConverterState, viewModel: ConverterViewModel, fileHandler: FileHandler, converting: Boolean) {
    val source = state.mode.source

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

@Composable
private fun BaseBlock(
    state: ConverterState,
    viewModel: ConverterViewModel,
    fileHandler: FileHandler,
    converting: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val target = state.mode.target
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val angle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "chevron"
    )

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onToggle
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionLabel("3", "Base project")
            Surface(shape = RoundedCornerShape(50), color = colors.surfaceVariant) {
                Text(
                    text = if (state.base != null) "CUSTOM" else "OPTIONAL",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.base != null) colors.primary else colors.onSurfaceVariant
                )
            }
            Box(Modifier.weight(1f))
            Chevron(colors.onSurfaceVariant, Modifier.size(18.dp).graphicsLayer { rotationZ = angle })
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Use one of your own projects to keep its instruments, mixer and effects. " +
                        "Without one, a default project is created and you assign the instruments afterwards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                FileCard(
                    file = state.base,
                    badge = target.extension,
                    emptyTitle = "Choose a .${target.extension} base project",
                    emptyHint = "Optional  ·  tap to browse",
                    enabled = !converting,
                    onClick = { fileHandler.pick(target.extension, viewModel::onBasePicked) }
                )
                if (state.base != null) {
                    TextButton(onClick = viewModel::clearBase, enabled = !converting) {
                        Text("Use the default base instead")
                    }
                }
            }
        }
    }
}

@Composable
private fun Chevron(color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.width * 0.12f
        drawLine(color, Offset(size.width * 0.15f, size.height * 0.35f), Offset(size.width * 0.5f, size.height * 0.68f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.68f), Offset(size.width * 0.85f, size.height * 0.35f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun Section(number: String, title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel(number, title)
        content()
    }
}

@Composable
private fun SectionLabel(number: String, title: String) {
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
}

private fun hintFor(state: ConverterState): String {
    val source = state.mode.source.extension
    val target = state.mode.target.extension.uppercase()
    val summary = state.summary

    return when {
        state.file == null -> "Choose a .$source file to start"
        summary == null -> "Ready to convert to $target"
        !state.hasSelection -> "Select at least one channel"
        else -> {
            val included = summary.channels.filter { it.index !in state.excluded }
            "${included.sumOf { it.notes }} notes from ${included.size} channels  →  $target"
        }
    }
}
