package com.flconverter.domain

class ProjectConverter(
    private val source: ProjectCodec,
    private val target: ProjectCodec
) {
    fun convert(input: ByteArray): ByteArray = target.encode(source.decode(input))
}

fun codecFor(format: FlFormat): ProjectCodec = when (format) {
    FlFormat.Flp -> FlpCodec
    FlFormat.Flm -> FlmCodec
}

fun converterFor(mode: ConversionMode): ProjectConverter =
    ProjectConverter(codecFor(mode.source), codecFor(mode.target))

fun summarize(mode: ConversionMode, input: ByteArray): SongSummary =
    codecFor(mode.source).decode(input).summary()
