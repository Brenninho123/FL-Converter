package com.flconverter.domain

enum class ConversionMode(val source: FlFormat, val target: FlFormat) {
    FlpToFlm(FlFormat.Flp, FlFormat.Flm),
    FlmToFlp(FlFormat.Flm, FlFormat.Flp);

    val label: String
        get() = "${source.extension.uppercase()} → ${target.extension.uppercase()}"
}
