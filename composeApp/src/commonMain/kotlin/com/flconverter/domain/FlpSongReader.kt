package com.flconverter.domain

internal object FlpSongReader {
    private const val NEW_PATTERN = 65
    private const val LEGACY_TEMPO = 66
    private const val TEMPO = 156
    private const val NOTES = 224
    private const val NOTE_SIZE = 24
    private const val DEFAULT_TEMPO = 120.0

    fun read(events: List<FlpEvent>, ppq: Int): Song {
        var tempo = DEFAULT_TEMPO
        var current = 0
        val patterns = LinkedHashMap<Int, MutableList<Note>>()

        for (event in events) {
            when (event.id) {
                NEW_PATTERN -> {
                    current = event.data.uint16(0)
                    patterns.getOrPut(current) { ArrayList() }
                }
                LEGACY_TEMPO -> tempo = event.data.uint16(0).toDouble()
                TEMPO -> tempo = event.data.uint32(0) / 1000.0
                NOTES -> if (current != 0) {
                    patterns.getOrPut(current) { ArrayList() }.addAll(readNotes(event.data))
                }
            }
        }

        return Song(tempo, ppq, patterns.map { Pattern(it.key, it.value) })
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
}
