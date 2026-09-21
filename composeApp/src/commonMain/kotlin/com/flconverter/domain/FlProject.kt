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
        val preview = song.noteChannels.flatMap { channel ->
            val densest = populated
                .map { pattern -> pattern.notes.filter { it.channel == channel } }
                .maxByOrNull { it.size }
            densest?.take(PREVIEW_LIMIT).orEmpty()
        }

        return SongSummary(
            tempo = song.tempo,
            ppq = ppq,
            channels = song.channelInfos(),
            patternCount = populated.size,
            noteCount = song.noteCount,
            previewNotes = preview
        )
    }

    private companion object {
        const val PREVIEW_LIMIT = 300
    }
}
