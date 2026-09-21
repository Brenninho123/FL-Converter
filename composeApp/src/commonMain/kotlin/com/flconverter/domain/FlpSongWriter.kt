package com.flconverter.domain

import kotlin.math.roundToLong

internal object FlpSongWriter {
    private const val CHANNEL_TYPE = 21
    private const val CURRENT_PATTERN = 67
    private const val PATTERN_NAME = 193
    private const val LAST_NOTE_CHANNEL_TYPE = 3
    private const val PLAYLIST_LENGTH_OFFSET = 8
    private const val PLAYLIST_TRACK_OFFSET = 12
    private const val PLAYLIST_END_OFFSET = 28
    private const val PLAYLIST_PATTERN_OFFSET = 32
    private const val PLAYLIST_INDEX_OFFSET = 6
    private const val NOTE_RACK_OFFSET = 6
    private const val NOTE_LENGTH_OFFSET = 8
    private const val NOTE_KEY_OFFSET = 12
    private const val NOTE_PAN_OFFSET = 20
    private const val NOTE_VELOCITY_OFFSET = 21

    private val PATTERN_BODY = setOf(FlpSongReader.NOTES, PATTERN_NAME, 150, 157, 158, 164)

    private val DEFAULT_NOTE = hexBytes("00000000 0040 0300 30000000 5600 0000 7800 4000 40 64 80 80")

    private val DEFAULT_ITEM = hexBytes(
        "00000000 0050 0150 00480000 F001 0000 7800 4000 40648080 00000000 00480000 01000000 " +
            "00000000 00000000 00000000 00000000 0000803F 00000000"
    )

    private class Block(val start: Int, val end: Int, val hasNotes: Boolean)

    fun write(song: Song, base: List<FlpEvent>, basePpq: Int): List<FlpEvent> {
        val targets = noteChannels(base)
        if (targets.isEmpty()) throw ConversionException("The base FLP has no channel that can hold notes")

        val blocks = patternBlocks(base)
        val firstNotes = blocks.firstOrNull { it.hasNotes }
        val firstMeta = blocks.firstOrNull { !it.hasNotes }
        if (firstNotes == null || firstMeta == null) {
            throw ConversionException("The base FLP needs at least one pattern")
        }

        val patterns = song.patterns.filter { it.notes.isNotEmpty() }.sortedBy { it.index }
        if (patterns.isEmpty()) throw ConversionException("The project has no notes to convert")
        if (patterns.size > MAX_PATTERNS) throw ConversionException("FL Studio supports up to $MAX_PATTERNS patterns")

        val mapping = mapChannels(song.noteChannels, targets.size)
        val newIds = patterns.mapIndexed { position, pattern -> pattern.index to position + 1 }.toMap()
        val prototype = notePrototype(base)
        val metaTemplate = base.subList(firstMeta.start, firstMeta.end)

        val out = ArrayList<FlpEvent>(base.size + patterns.size * 8)
        var index = 0
        while (index < base.size) {
            val event = base[index]
            val block = blocks.firstOrNull { it.start == index }

            when {
                block != null && block === firstNotes -> patterns.forEach { pattern ->
                    val id = newIds.getValue(pattern.index)
                    out.add(FlpEvent.word(FlpSongReader.NEW_PATTERN, id))
                    out.add(FlpEvent(FlpSongReader.NOTES, encodeNotes(pattern, mapping, targets, prototype, song.ppq, basePpq)))
                }
                block != null && block === firstMeta -> patterns.forEach { pattern ->
                    val id = newIds.getValue(pattern.index)
                    metaTemplate.forEach { template ->
                        out.add(
                            when (template.id) {
                                FlpSongReader.NEW_PATTERN -> FlpEvent.word(template.id, id)
                                PATTERN_NAME -> FlpEvent.text(PATTERN_NAME, patternName(song, pattern, id))
                                else -> template
                            }
                        )
                    }
                }
                block != null -> Unit
                event.id == FlpSongReader.TEMPO -> out.add(FlpEvent.dword(event.id, (song.tempo * 1000).roundToLong()))
                event.id == CURRENT_PATTERN -> out.add(FlpEvent.word(event.id, 1))
                event.id == FlpSongReader.PLAYLIST -> out.add(
                    FlpEvent(event.id, playlist(event.data, song, newIds, mapping, targets, basePpq))
                )
                else -> out.add(event)
            }

            index = block?.end ?: (index + 1)
        }

        if (base.none { it.id == FlpSongReader.TEMPO }) {
            out.add(minOf(1, out.size), FlpEvent.dword(FlpSongReader.TEMPO, (song.tempo * 1000).roundToLong()))
        }
        if (base.none { it.id == FlpSongReader.PLAYLIST }) {
            throw ConversionException("The base FLP has no playlist")
        }

        return out
    }

    fun capacity(base: List<FlpEvent>): Int = noteChannels(base).size

    private fun noteChannels(base: List<FlpEvent>): List<Int> {
        val channels = ArrayList<Int>()
        var current = -1
        for (event in base) {
            when (event.id) {
                FlpSongReader.NEW_CHANNEL -> current = event.data.uint16(0)
                CHANNEL_TYPE -> if (current >= 0 && event.data.uint8(0) <= LAST_NOTE_CHANNEL_TYPE) {
                    channels.add(current)
                    current = -1
                }
            }
        }
        return channels
    }

    private fun patternBlocks(base: List<FlpEvent>): List<Block> {
        val blocks = ArrayList<Block>()
        var index = 0
        while (index < base.size) {
            if (base[index].id != FlpSongReader.NEW_PATTERN) {
                index++
                continue
            }
            var end = index + 1
            var hasNotes = false
            while (end < base.size && base[end].id in PATTERN_BODY) {
                if (base[end].id == FlpSongReader.NOTES) hasNotes = true
                end++
            }
            blocks.add(Block(index, end, hasNotes))
            index = end
        }
        return blocks
    }

    private fun notePrototype(base: List<FlpEvent>): ByteArray {
        val event = base.firstOrNull { it.id == FlpSongReader.NOTES && it.data.size >= FlpSongReader.NOTE_SIZE }
        return event?.data?.copyOfRange(0, FlpSongReader.NOTE_SIZE) ?: DEFAULT_NOTE
    }

    private fun encodeNotes(
        pattern: Pattern,
        mapping: Map<Int, Int>,
        targets: List<Int>,
        prototype: ByteArray,
        sourcePpq: Int,
        basePpq: Int
    ): ByteArray {
        val notes = pattern.notes.sortedWith(compareBy({ it.position }, { it.key }))
        val out = ByteBuilder(notes.size * FlpSongReader.NOTE_SIZE)

        for (note in notes) {
            val bytes = prototype.copyOf()
            val builder = ByteBuilder(FlpSongReader.NOTE_SIZE)
            builder.u32(rescale(note.position, sourcePpq, basePpq))
            builder.bytes(bytes.copyOfRange(4, NOTE_RACK_OFFSET))
            builder.u16(targets[mapping.getValue(note.channel)])
            builder.u32(maxOf(1L, rescale(note.length, sourcePpq, basePpq)))
            builder.u16(note.key)
            builder.bytes(bytes.copyOfRange(NOTE_KEY_OFFSET + 2, NOTE_PAN_OFFSET))
            builder.u8(note.pan)
            builder.u8(note.velocity)
            builder.bytes(bytes.copyOfRange(NOTE_VELOCITY_OFFSET + 1, FlpSongReader.NOTE_SIZE))
            out.bytes(builder.toByteArray())
        }

        return out.toByteArray()
    }

    private fun playlist(
        data: ByteArray,
        song: Song,
        newIds: Map<Int, Int>,
        mapping: Map<Int, Int>,
        targets: List<Int>,
        basePpq: Int
    ): ByteArray {
        val size = FlpSongReader.PLAYLIST_ITEM_SIZE
        if (data.size % size != 0) throw ConversionException("Unsupported playlist format in the base FLP")

        val items = List(data.size / size) { data.copyOfRange(it * size, (it + 1) * size) }
        val kept = items.filter { it.uint16(PLAYLIST_INDEX_OFFSET) <= FlpSongReader.PATTERN_BASE }
        val prototype = items.firstOrNull { it.uint16(PLAYLIST_INDEX_OFFSET) > FlpSongReader.PATTERN_BASE } ?: DEFAULT_ITEM
        val usedTracks = kept.maxOfOrNull { FlpSongReader.TRACK_COUNT - it.uint16(PLAYLIST_TRACK_OFFSET) } ?: 0

        val channelByPattern = song.patterns.associate { pattern ->
            pattern.index to pattern.notes.first().channel
        }
        val channels = song.noteChannels
        val out = ByteBuilder(data.size + song.patterns.size * size)
        kept.forEach { out.bytes(it) }

        for (placement in song.arrangement()) {
            val id = newIds[placement.pattern] ?: continue
            val channel = channelByPattern.getValue(placement.pattern)
            val track = usedTracks + 1 + channels.indexOf(channel)
            if (track > FlpSongReader.TRACK_COUNT) throw ConversionException("The playlist has no free track left")

            val length = maxOf(1L, rescale(placement.length, song.ppq, basePpq))
            val item = ByteBuilder(size)
            item.u32(rescale(placement.start, song.ppq, basePpq))
            item.bytes(prototype.copyOfRange(4, PLAYLIST_INDEX_OFFSET))
            item.u16(FlpSongReader.PATTERN_BASE + id)
            item.u32(length)
            item.u16(FlpSongReader.TRACK_COUNT - track)
            item.bytes(prototype.copyOfRange(PLAYLIST_TRACK_OFFSET + 2, PLAYLIST_END_OFFSET))
            item.u32(length)
            item.u32(id.toLong())
            item.bytes(prototype.copyOfRange(PLAYLIST_PATTERN_OFFSET + 4, size))
            out.bytes(item.toByteArray())
        }

        return out.toByteArray()
    }

    private fun patternName(song: Song, pattern: Pattern, id: Int): String {
        val channel = pattern.notes.first().channel
        val name = song.channelNames.getOrNull(channel).orEmpty()
        return if (name.isBlank()) "Pattern $id" else "$name $id"
    }

    private fun rescale(value: Long, from: Int, to: Int): Long = if (from == to) value else value * to / from

    private const val MAX_PATTERNS = 999
}
