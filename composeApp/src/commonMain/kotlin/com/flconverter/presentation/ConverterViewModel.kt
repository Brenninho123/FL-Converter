package com.flconverter.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flconverter.domain.ConversionException
import com.flconverter.domain.ConversionMode
import com.flconverter.domain.SongSummary
import com.flconverter.domain.converterFor
import com.flconverter.domain.summarize
import com.flconverter.platform.PickedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ConversionStatus {
    data object Idle : ConversionStatus
    data object Converting : ConversionStatus
    data class Saved(val name: String) : ConversionStatus
    data class Failed(val message: String) : ConversionStatus
}

data class ConverterState(
    val mode: ConversionMode = ConversionMode.FlpToFlm,
    val file: PickedFile? = null,
    val summary: SongSummary? = null,
    val status: ConversionStatus = ConversionStatus.Idle
)

class ConverterViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = mutableState.asStateFlow()

    fun selectMode(mode: ConversionMode) {
        mutableState.update { ConverterState(mode = mode) }
    }

    fun onFilePicked(file: PickedFile?) {
        if (file == null) return

        val expected = mutableState.value.mode.source.extension
        if (!file.name.endsWith(".$expected", ignoreCase = true)) {
            mutableState.update {
                it.copy(file = null, status = ConversionStatus.Failed("Select a .$expected file"))
            }
            return
        }

        mutableState.update { it.copy(file = file, summary = null, status = ConversionStatus.Idle) }
        analyze(file, mutableState.value.mode)
    }

    private fun analyze(file: PickedFile, mode: ConversionMode) {
        viewModelScope.launch {
            val summary = withContext(Dispatchers.Default) {
                try {
                    summarize(mode, file.bytes)
                } catch (error: ConversionException) {
                    null
                }
            }
            mutableState.update { if (it.file === file) it.copy(summary = summary) else it }
        }
    }

    fun convert(onConverted: (name: String, bytes: ByteArray) -> Unit) {
        val current = mutableState.value
        val file = current.file ?: return
        if (current.status is ConversionStatus.Converting) return

        mutableState.update { it.copy(status = ConversionStatus.Converting) }

        viewModelScope.launch {
            try {
                val output = withContext(Dispatchers.Default) {
                    converterFor(current.mode).convert(file.bytes)
                }
                val name = file.name.substringBeforeLast('.') + "." + current.mode.target.extension
                onConverted(name, output)
            } catch (error: ConversionException) {
                mutableState.update {
                    it.copy(status = ConversionStatus.Failed(error.message ?: "Conversion failed"))
                }
            }
        }
    }

    fun onSaved(name: String, saved: Boolean) {
        mutableState.update {
            it.copy(
                status = if (saved) ConversionStatus.Saved(name) else ConversionStatus.Failed("Save cancelled")
            )
        }
    }
}
