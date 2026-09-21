package com.flconverter.domain

class FlProject(
    val format: Int,
    val channels: Int,
    val ppq: Int,
    val events: ByteArray,
    val song: Song
) {
    fun summary(): SongSummary {
        val populated = song.patterns.filter { it.notes.isNotEmpty() }
        val densest = populated.maxByOrNull { it.notes.size }
        return SongSummary(
            tempo = song.tempo,
            ppq = ppq,
            channels = channels,
            patternCount = populated.size,
            noteCount = song.noteCount,
            previewNotes = densest?.notes?.take(PREVIEW_LIMIT).orEmpty()
        )
    }

    private companion object {
        const val PREVIEW_LIMIT = 1000
    }
}
