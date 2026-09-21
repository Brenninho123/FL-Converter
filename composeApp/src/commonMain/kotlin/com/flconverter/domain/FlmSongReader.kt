package com.flconverter.domain

import kotlin.math.roundToLong

internal object FlmSongReader {
    private const val MAGIC = "10LF"
    private const val PPQ = 96
    private const val DEFAULT_TEMPO = 120.0
    private const val HEAD_TEMPO_OFFSET = 296
    private const val CHUNK_PREFIX = 8
    private const val SUB_CHUNK_OFFSET = 8
    private const val NOTE_STRIDE = 20
    private const val NOTE_SIZE = 18
    private const val POSITION_SCALE = 8388608L
    private const val VELOCITY_SCALE = 32767L
    private const val NAME_LIMIT = 256

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
        val position = bytes.uint32(start)
        var length = 0L
        var notes = emptyList<Note>()

        forEachChunk(bytes, start + SUB_CHUNK_OFFSET, end) { tag, chunkStart, chunkEnd ->
            when (tag) {
                "CLHd" -> length = (bytes.float64(chunkStart + 8) * PPQ).roundToLong()
                "EVN2" -> notes = readNotes(bytes, chunkStart, chunkEnd, channel)
            }
        }

        if (notes.isEmpty()) return

        val index = patterns.size + 1
        patterns.add(Pattern(index, notes))
        placements.add(Placement(index, position, length))
    }

    private fun readNotes(bytes: ByteArray, start: Int, end: Int, channel: Int): List<Note> {
        if (start + 4 > end || bytes.uint16(start) != NOTE_STRIDE) return emptyList()

        val notes = ArrayList<Note>()
        var offset = start + 4 + 2

        while (offset + NOTE_SIZE <= end) {
            val duration = bytes.float64(offset)
            if (duration <= 0.0) {
                offset += NOTE_STRIDE
                continue
            }

            val length = (duration * PPQ).roundToLong().coerceAtLeast(1L)
            val velocity = ((bytes.uint16(offset + 10) * 127L + VELOCITY_SCALE / 2) / VELOCITY_SCALE).toInt()
            val position = (bytes.uint32(offset + 14) * PPQ + POSITION_SCALE / 2) / POSITION_SCALE

            notes.add(
                Note(
                    position = position,
                    length = length,
                    key = bytes.uint16(offset + 8),
                    velocity = velocity.coerceIn(0, 127),
                    pan = 64,
                    channel = channel
                )
            )
            offset += NOTE_STRIDE
        }

        return notes
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
