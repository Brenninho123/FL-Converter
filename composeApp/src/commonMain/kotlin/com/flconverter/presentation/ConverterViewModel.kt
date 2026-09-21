package com.flconverter.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flconverter.domain.ConversionException
import com.flconverter.domain.ConversionMode
import com.flconverter.domain.ConversionResult
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
    data class Saved(val name: String, val detail: String) : ConversionStatus
    data class Failed(val message: String) : ConversionStatus
}

data class ConverterState(
    val mode: ConversionMode = ConversionMode.FlpToFlm,
    val file: PickedFile? = null,
    val base: PickedFile? = null,
    val summary: SongSummary? = null,
    val excluded: Set<Int> = emptySet(),
    val status: ConversionStatus = ConversionStatus.Idle
) {
    val hasSelection: Boolean
        get() = summary?.channels?.any { it.index !in excluded } ?: file != null

    val canConvert: Boolean
        get() = file != null && hasSelection && status !is ConversionStatus.Converting
}

class ConverterViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(ConverterState())
    val state: StateFlow<ConverterState> = mutableState.asStateFlow()

    private var detail = ""

    fun selectMode(mode: ConversionMode) {
        mutableState.update { ConverterState(mode = mode) }
    }

    fun onFilePicked(file: PickedFile?) {
        if (file == null) return

        val expected = mutableState.value.mode.source.extension
        if (!hasExtension(file, expected)) {
            mutableState.update {
                it.copy(file = null, summary = null, excluded = emptySet(), status = ConversionStatus.Failed("Select a .$expected file"))
            }
            return
        }

        mutableState.update { it.copy(file = file, summary = null, excluded = emptySet(), status = ConversionStatus.Idle) }
        analyze(file, mutableState.value.mode)
    }

    fun onBasePicked(file: PickedFile?) {
        if (file == null) return

        val expected = mutableState.value.mode.target.extension
        if (!hasExtension(file, expected)) {
            mutableState.update {
                it.copy(base = null, status = ConversionStatus.Failed("Select a .$expected base project"))
            }
            return
        }

        mutableState.update { it.copy(base = file, status = ConversionStatus.Idle) }
    }

    fun clearBase() {
        mutableState.update { it.copy(base = null, status = ConversionStatus.Idle) }
    }

    fun toggleChannel(index: Int) {
        mutableState.update {
            val excluded = if (index in it.excluded) it.excluded - index else it.excluded + index
            it.copy(excluded = excluded, status = ConversionStatus.Idle)
        }
    }

    fun convert(onConverted: (name: String, bytes: ByteArray) -> Unit) {
        val current = mutableState.value
        val file = current.file ?: return
        if (!current.canConvert) return

        mutableState.update { it.copy(status = ConversionStatus.Converting) }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    converterFor(current.mode).convert(file.bytes, current.base?.bytes, current.excluded)
                }
                val name = file.name.substringBeforeLast('.') + "." + current.mode.target.extension
                detail = describe(result)
                onConverted(name, result.bytes)
            } catch (error: ConversionException) {
                fail(error.message ?: "Conversion failed")
            } catch (error: IndexOutOfBoundsException) {
                fail("One of the files is corrupted")
            } catch (error: NoSuchElementException) {
                fail("The base project does not have the expected structure")
            }
        }
    }

    fun onSaved(name: String, saved: Boolean) {
        mutableState.update {
            it.copy(
                status = if (saved) ConversionStatus.Saved(name, detail) else ConversionStatus.Failed("Save cancelled")
            )
        }
    }

    private fun analyze(file: PickedFile, mode: ConversionMode) {
        viewModelScope.launch {
            val summary = withContext(Dispatchers.Default) {
                try {
                    summarize(mode, file.bytes)
                } catch (error: ConversionException) {
                    null
                } catch (error: IndexOutOfBoundsException) {
                    null
                }
            }
            mutableState.update { if (it.file === file) it.copy(summary = summary) else it }
        }
    }

    private fun describe(result: ConversionResult): String {
        val notes = "${result.notes} notes from ${result.sourceChannels} channels"
        val merged = if (result.sourceChannels > result.targetChannels) {
            ", merged into the ${result.targetChannels} channel" + (if (result.targetChannels == 1) "" else "s") + " of the base project"
        } else {
            ""
        }
        val fallback = if (result.usedDefaultBase) " A default base project was used, so assign instruments in the target app." else ""
        return "$notes$merged.$fallback"
    }

    private fun fail(message: String) {
        mutableState.update { it.copy(status = ConversionStatus.Failed(message)) }
    }

    private fun hasExtension(file: PickedFile, extension: String): Boolean =
        file.name.endsWith(".$extension", ignoreCase = true)
}
