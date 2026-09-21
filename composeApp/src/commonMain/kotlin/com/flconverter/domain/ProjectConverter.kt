package com.flconverter.domain

class ConversionResult(
    val bytes: ByteArray,
    val notes: Int,
    val sourceChannels: Int,
    val targetChannels: Int,
    val usedDefaultBase: Boolean
)

class ProjectConverter(
    private val source: ProjectCodec,
    private val target: ProjectCodec
) {
    fun convert(input: ByteArray, base: ByteArray?, excluded: Set<Int> = emptySet()): ConversionResult {
        val decoded = source.decode(input)
        val song = decoded.song.withoutChannels(excluded)
        if (song.noteCount == 0) throw ConversionException("Select at least one channel that has notes")

        val project = FlProject(decoded.format, decoded.channels, decoded.ppq, decoded.events, song)
        val sourceChannels = song.noteChannels.size
        return ConversionResult(
            bytes = target.encode(project, base),
            notes = song.noteCount,
            sourceChannels = sourceChannels,
            targetChannels = target.capacity(base, sourceChannels),
            usedDefaultBase = base == null
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
