package com.flconverter.domain

import kotlin.math.ceil
import kotlin.math.roundToLong

internal object FlmSongWriter {
    private const val CHANNEL_HEAD = 8
    private const val CLIP_HEAD = 8
    private const val NOTE_GAP = 2
    private const val CLIP_ID_OFFSET = 24
    private const val DEFAULT_EXTRA = 0x7FFF
    private const val LOW_MASK = 0xFFFFL

    private class ClipSpec(val start: Long, val length: Long, val notes: List<Note>)

    fun capacity(base: ByteArray): Int = instrumentIndices(parseBase(base)).size

    fun write(song: Song, base: ByteArray): ByteArray {
        val top = parseBase(base)
        val instruments = instrumentIndices(top)
        if (instruments.isEmpty()) {
            throw ConversionException("The base FLM has no instrument channel with notes")
        }
        if (song.noteCount == 0) throw ConversionException("The project has no notes to convert")

        val mapping = mapChannels(song.noteChannels, instruments.size)
        val arrangement = song.arrangement()
        val patterns = song.patterns.associateBy { it.index }
        var nextId = maxClipId(top) + 1

        val out = ArrayList<FlmChunk>(top.size)
        top.forEachIndexed { index, chunk ->
            val order = instruments.indexOf(index)
            when {
                chunk.tag == "HEAD" -> out.add(FlmChunk(chunk.tag, withTempo(chunk.payload, song.tempo)))
                order >= 0 -> {
                    val sources = mapping.filterValues { it == order }.keys
                    val clips = mergeOverlaps(
                        arrangement.mapNotNull { placement ->
                            val notes = patterns[placement.pattern]?.notes.orEmpty().filter { it.channel in sources }
                            if (notes.isEmpty()) null else ClipSpec(placement.start, placement.length, notes)
                        }
                    )
                    val rebuilt = rebuildChannel(chunk, clips, song.ppq, nextId)
                    nextId += clips.size
                    out.add(rebuilt)
                }
                else -> out.add(chunk)
            }
        }

        val body = FlmChunks.serialize(out)
        val result = ByteBuilder(body.size + 4)
        result.ascii(FlmSongReader.MAGIC)
        result.bytes(body)
        return result.toByteArray()
    }

    private fun mergeOverlaps(clips: List<ClipSpec>): List<ClipSpec> {
        val merged = ArrayList<ClipSpec>()

        for (clip in clips.sortedBy { it.start }) {
            val last = merged.lastOrNull()
            if (last == null || clip.start >= last.start + last.length) {
                merged.add(clip)
                continue
            }

            val shift = clip.start - last.start
            val end = maxOf(last.start + last.length, clip.start + clip.length)
            merged[merged.lastIndex] = ClipSpec(last.start, end - last.start, last.notes + clip.notes.map { it.moved(shift) })
        }

        return merged
    }

    private fun parseBase(base: ByteArray): List<FlmChunk> {
        if (base.size < 8 || base.ascii(0, 4) != FlmSongReader.MAGIC) {
            throw ConversionException("The base file is not a valid FLM project")
        }
        return FlmChunks.parse(base, 4, base.size)
    }

    private fun instrumentIndices(top: List<FlmChunk>): List<Int> =
        top.indices.filter { top[it].tag == "CHNL" && noteTrackIndex(channelChunks(top[it])) >= 0 }

    private fun channelChunks(chunk: FlmChunk): List<FlmChunk> =
        FlmChunks.parse(chunk.payload, CHANNEL_HEAD, chunk.payload.size)

    private fun noteTrackIndex(chunks: List<FlmChunk>): Int = chunks.indexOfFirst { chunk ->
        chunk.tag == "TRKH" && FlmChunks.parse(chunk.payload, 0, chunk.payload.size).any(::isNoteClip)
    }

    private fun isNoteClip(chunk: FlmChunk): Boolean {
        if (chunk.tag != "CLIP" || chunk.payload.size < CLIP_HEAD) return false
        val events = FlmChunks.parse(chunk.payload, CLIP_HEAD, chunk.payload.size).firstOrNull { it.tag == "EVN2" }
        val data = events?.payload ?: return false
        return data.size >= 4 + NOTE_GAP + FlmSongReader.PARTIAL_NOTE_SIZE &&
            data.uint16(0) == FlmSongReader.NOTE_STRIDE &&
            data.float64(4 + NOTE_GAP) > 0.0
    }

    private fun maxClipId(top: List<FlmChunk>): Long {
        var highest = 0L
        for (chunk in top.filter { it.tag == "CHNL" }) {
            for (track in channelChunks(chunk).filter { it.tag == "TRKH" }) {
                for (clip in FlmChunks.parse(track.payload, 0, track.payload.size).filter { it.tag == "CLIP" }) {
                    val header = FlmChunks.parse(clip.payload, CLIP_HEAD, clip.payload.size).firstOrNull { it.tag == "CLHd" }
                    if (header != null && header.payload.size >= CLIP_ID_OFFSET + 4) {
                        highest = maxOf(highest, header.payload.uint32(CLIP_ID_OFFSET))
                    }
                }
            }
        }
        return highest
    }

    private fun withTempo(payload: ByteArray, tempo: Double): ByteArray {
        if (payload.size < FlmSongReader.HEAD_TEMPO_OFFSET + 8) return payload

        val out = ByteBuilder(payload.size)
        out.bytes(payload.copyOfRange(0, FlmSongReader.HEAD_TEMPO_OFFSET))
        out.f64(tempo)
        out.bytes(payload.copyOfRange(FlmSongReader.HEAD_TEMPO_OFFSET + 8, payload.size))
        return out.toByteArray()
    }

    private fun rebuildChannel(chunk: FlmChunk, clips: List<ClipSpec>, ppq: Int, firstId: Long): FlmChunk {
        val chunks = channelChunks(chunk)
        val trackIndex = noteTrackIndex(chunks)
        val track = chunks[trackIndex]
        val inner = FlmChunks.parse(track.payload, 0, track.payload.size)
        val prototype = inner.first(::isNoteClip)

        val kept = inner.filter { !isNoteClip(it) }
        val built = clips.sortedBy { it.start }.mapIndexed { index, spec ->
            buildClip(prototype, spec, ppq, firstId + index)
        }

        val newChunks = chunks.toMutableList()
        newChunks[trackIndex] = FlmChunk(track.tag, FlmChunks.serialize(kept + built))

        val out = ByteBuilder(chunk.payload.size)
        out.bytes(chunk.payload.copyOfRange(0, CHANNEL_HEAD))
        out.bytes(FlmChunks.serialize(newChunks))
        return FlmChunk(chunk.tag, out.toByteArray())
    }

    private fun buildClip(prototype: FlmChunk, spec: ClipSpec, ppq: Int, id: Long): FlmChunk {
        val notes = spec.notes.sortedWith(compareBy({ it.position }, { it.key }))
        val last = notes.last()
        val content = (last.position + last.length).toDouble() / ppq
        val length = maxOf(spec.length.toDouble() / ppq, ceil(content))

        val subChunks = FlmChunks.parse(prototype.payload, CLIP_HEAD, prototype.payload.size).map { sub ->
            when (sub.tag) {
                "CLHd" -> FlmChunk(sub.tag, clipHeader(sub.payload, content, length, id))
                "EVN2" -> FlmChunk(sub.tag, encodeNotes(sub.payload, notes, ppq))
                else -> sub
            }
        }

        val start = (spec.start * FlmSongReader.CLIP_UNITS_PER_BEAT + ppq / 2) / ppq
        val out = ByteBuilder(prototype.payload.size)
        out.u32(start)
        out.bytes(prototype.payload.copyOfRange(4, CLIP_HEAD))
        out.bytes(FlmChunks.serialize(subChunks))
        return FlmChunk(prototype.tag, out.toByteArray())
    }

    private fun clipHeader(payload: ByteArray, content: Double, length: Double, id: Long): ByteArray {
        val out = ByteBuilder(payload.size)
        out.f64(content)
        out.f64(length)
        out.bytes(payload.copyOfRange(16, CLIP_ID_OFFSET))
        out.u32(id)
        out.bytes(payload.copyOfRange(CLIP_ID_OFFSET + 4, payload.size))
        return out.toByteArray()
    }

    private fun encodeNotes(prototype: ByteArray, notes: List<Note>, ppq: Int): ByteArray {
        val extra = if (prototype.size >= 4 + NOTE_GAP + 14) prototype.uint16(4 + NOTE_GAP + 12) else DEFAULT_EXTRA
        val out = ByteBuilder(4 + notes.size * FlmSongReader.NOTE_STRIDE)
        out.bytes(prototype.copyOfRange(0, 4))

        notes.forEachIndexed { index, note ->
            val position = (note.position.toDouble() / ppq * FlmSongReader.POSITION_SCALE).roundToLong()
            val velocity = (note.velocity.coerceIn(0, 127) * FlmSongReader.VELOCITY_SCALE + 63) / 127

            out.u16(0)
            out.f64(note.length.toDouble() / ppq)
            out.u16(note.key)
            out.u16(velocity.toInt())
            out.u16(extra)
            if (index == notes.lastIndex) out.u16((position and LOW_MASK).toInt()) else out.u32(position)
        }

        return out.toByteArray()
    }
}
