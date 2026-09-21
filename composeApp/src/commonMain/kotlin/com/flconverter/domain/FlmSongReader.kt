package com.flconverter.domain

import kotlin.math.roundToLong

internal object FlmSongReader {
    const val MAGIC = "10LF"
    const val PPQ = 96
    const val HEAD_TEMPO_OFFSET = 264
    const val NOTE_STRIDE = 20
    const val NOTE_SIZE = 18
    const val PARTIAL_NOTE_SIZE = 16
    const val POSITION_SCALE = 8388608L
    const val VELOCITY_SCALE = 32767L
    const val CLIP_UNITS_PER_BEAT = 64L
    private const val DEFAULT_TEMPO = 120.0
    private const val CHUNK_PREFIX = 8
    private const val SUB_CHUNK_OFFSET = 8
    private const val NAME_LIMIT = 256
    private const val LOW_MASK = 0xFFFFL
    private const val HALF_LOW = 0x8000L

    fun read(bytes: ByteArray): Song {
        if (bytes.size < 8 || bytes.ascii(0, 4) != MAGIC) {
            throw ConversionException("Not a valid FLM file")
        }

        var tempo = DEFAULT_TEMPO
        val names = ArrayList<String>()
        val patterns = ArrayList<Pattern>()
        val placements = ArrayList<Placement>()

        forEachChunk(bytes, 4, bytes.size) { tag, start, end ->
            when (tag) {
                "HEAD" -> tempo = readTempo(bytes, start, end)
                "CHNL" -> {
                    val channel = names.size
                    names.add(readChannel(bytes, start, end, channel, patterns, placements))
                }
            }
        }

        return Song(tempo, PPQ, patterns, names, placements)
    }

    private fun readTempo(bytes: ByteArray, start: Int, end: Int): Double {
        if (start + HEAD_TEMPO_OFFSET + 8 > end) return DEFAULT_TEMPO
        val value = bytes.float64(start + HEAD_TEMPO_OFFSET)
        return if (value in 20.0..999.0) value else DEFAULT_TEMPO
    }

    private fun readChannel(
        bytes: ByteArray,
        start: Int,
        end: Int,
        channel: Int,
        patterns: MutableList<Pattern>,
        placements: MutableList<Placement>
    ): String {
        var name = ""

        forEachChunk(bytes, start + SUB_CHUNK_OFFSET, end) { tag, chunkStart, chunkEnd ->
            when (tag) {
                "CHHD" -> name = readName(bytes, chunkStart, chunkEnd)
                "TRKH" -> forEachChunk(bytes, chunkStart, chunkEnd) { trackTag, clipStart, clipEnd ->
                    if (trackTag == "CLIP") {
                        readClip(bytes, clipStart, clipEnd, channel, patterns, placements)
                    }
                }
            }
        }

        return name
    }

    private fun readClip(
        bytes: ByteArray,
        start: Int,
        end: Int,
        channel: Int,
        patterns: MutableList<Pattern>,
        placements: MutableList<Placement>
    ) {
        val position = bytes.uint32(start) * PPQ / CLIP_UNITS_PER_BEAT
        var length = 0L
        var content = 0.0
        var notes = emptyList<Note>()

        forEachChunk(bytes, start + SUB_CHUNK_OFFSET, end) { tag, chunkStart, chunkEnd ->
            when (tag) {
                "CLHd" -> {
                    content = bytes.float64(chunkStart)
                    length = (bytes.float64(chunkStart + 8) * PPQ).roundToLong()
                }
                "EVN2" -> notes = readNotes(bytes, chunkStart, chunkEnd, channel, content)
            }
        }

        if (notes.isEmpty()) return

        val index = patterns.size + 1
        patterns.add(Pattern(index, notes))
        placements.add(Placement(index, position, length))
    }

    private fun readNotes(bytes: ByteArray, start: Int, end: Int, channel: Int, content: Double): List<Note> {
        if (start + 4 > end || bytes.uint16(start) != NOTE_STRIDE) return emptyList()

        val notes = ArrayList<Note>()
        var offset = start + 4 + 2

        while (offset + PARTIAL_NOTE_SIZE <= end) {
            val duration = bytes.float64(offset)
            if (duration > 0.0) {
                val raw = if (offset + NOTE_SIZE <= end) {
                    bytes.uint32(offset + 14)
                } else {
                    recoverPosition(bytes.uint16(offset + 14).toLong(), content - duration)
                }
                val velocity = ((bytes.uint16(offset + 10) * 127L + VELOCITY_SCALE / 2) / VELOCITY_SCALE).toInt()

                notes.add(
                    Note(
                        position = (raw * PPQ + POSITION_SCALE / 2) / POSITION_SCALE,
                        length = (duration * PPQ).roundToLong().coerceAtLeast(1L),
                        key = bytes.uint16(offset + 8),
                        velocity = velocity.coerceIn(0, 127),
                        pan = 64,
                        channel = channel
                    )
                )
            }
            offset += NOTE_STRIDE
        }

        return notes
    }

    private fun recoverPosition(low: Long, estimateBeats: Double): Long {
        val estimate = (estimateBeats * POSITION_SCALE).roundToLong().coerceAtLeast(0L)
        var high = estimate and LOW_MASK.inv()
        val difference = low - (estimate and LOW_MASK)
        if (difference < -HALF_LOW) high += LOW_MASK + 1 else if (difference > HALF_LOW) high -= LOW_MASK + 1
        return high or low
    }

    private fun readName(bytes: ByteArray, start: Int, end: Int): String {
        val limit = minOf(end, start + NAME_LIMIT)
        var length = 0
        while (start + length < limit && bytes[start + length] != 0.toByte()) length++
        return bytes.copyOfRange(start, start + length).decodeToString()
    }

    private fun forEachChunk(bytes: ByteArray, from: Int, to: Int, action: (String, Int, Int) -> Unit) {
        var offset = from
        while (offset + CHUNK_PREFIX <= to) {
            val size = bytes.uint32(offset + 4)
            val start = offset + CHUNK_PREFIX
            if (size > to - start) throw ConversionException("FLM chunk is truncated")
            action(bytes.ascii(offset, 4), start, start + size.toInt())
            offset = start + size.toInt()
        }
    }
}
