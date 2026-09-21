package com.flconverter.domain

class ConversionResult(
    val bytes: ByteArray,
    val notes: Int,
    val sourceChannels: Int,
    val targetChannels: Int
)

class ProjectConverter(
    private val source: ProjectCodec,
    private val target: ProjectCodec
) {
    fun convert(input: ByteArray, base: ByteArray): ConversionResult {
        val project = source.decode(input)
        val output = target.encode(project, base)
        return ConversionResult(
            bytes = output,
            notes = project.song.noteCount,
            sourceChannels = project.song.noteChannels.size,
            targetChannels = target.capacity(base)
        )
    }
}

fun codecFor(format: FlFormat): ProjectCodec = when (format) {
    FlFormat.Flp -> FlpCodec
    FlFormat.Flm -> FlmCodec
}

fun converterFor(mode: ConversionMode): ProjectConverter =
    ProjectConverter(codecFor(mode.source), codecFor(mode.target))

fun summarize(mode: ConversionMode, input: ByteArray): SongSummary =
    codecFor(mode.source).decode(input).summary()
