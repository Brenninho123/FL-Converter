package com.flconverter.domain

internal object FlpSongReader {
    const val NEW_CHANNEL = 64
    const val NEW_PATTERN = 65
    const val LEGACY_TEMPO = 66
    const val TEMPO = 156
    const val CHANNEL_NAME = 203
    const val NOTES = 224
    const val PLAYLIST = 233
    const val NOTE_SIZE = 24
    const val PLAYLIST_ITEM_SIZE = 60
    const val PATTERN_BASE = 0x5000
    const val TRACK_COUNT = 500
    private const val DEFAULT_TEMPO = 120.0

    fun read(events: List<FlpEvent>, ppq: Int): Song {
        var tempo = DEFAULT_TEMPO
        var current = 0
        val patterns = LinkedHashMap<Int, MutableList<Note>>()
        val names = ArrayList<String>()
        val placements = ArrayList<Placement>()

        for (event in events) {
            when (event.id) {
                NEW_CHANNEL -> names.add("")
                CHANNEL_NAME -> if (names.isNotEmpty()) names[names.size - 1] = event.data.utf16()
                NEW_PATTERN -> {
                    current = event.data.uint16(0)
                    patterns.getOrPut(current) { ArrayList() }
                }
                LEGACY_TEMPO -> tempo = event.data.uint16(0).toDouble()
                TEMPO -> tempo = event.data.uint32(0) / 1000.0
                NOTES -> if (current != 0) {
                    patterns.getOrPut(current) { ArrayList() }.addAll(readNotes(event.data))
                }
                PLAYLIST -> placements.addAll(readPlaylist(event.data))
            }
        }

        return Song(tempo, ppq, patterns.map { Pattern(it.key, it.value) }, names, placements)
    }

    private fun readNotes(data: ByteArray): List<Note> {
        val notes = ArrayList<Note>(data.size / NOTE_SIZE)
        var offset = 0

        while (offset + NOTE_SIZE <= data.size) {
            notes.add(
                Note(
                    position = data.uint32(offset),
                    channel = data.uint16(offset + 6),
                    length = data.uint32(offset + 8),
                    key = data.uint16(offset + 12),
                    pan = data.uint8(offset + 20),
                    velocity = data.uint8(offset + 21)
                )
            )
            offset += NOTE_SIZE
        }

        return notes
    }

    private fun readPlaylist(data: ByteArray): List<Placement> {
        if (data.size % PLAYLIST_ITEM_SIZE != 0) return emptyList()

        val placements = ArrayList<Placement>()
        var offset = 0

        while (offset + PLAYLIST_ITEM_SIZE <= data.size) {
            val index = data.uint16(offset + 6)
            if (index > PATTERN_BASE) {
                placements.add(Placement(index - PATTERN_BASE, data.uint32(offset), data.uint32(offset + 8)))
            }
            offset += PLAYLIST_ITEM_SIZE
        }

        return placements
    }
}
