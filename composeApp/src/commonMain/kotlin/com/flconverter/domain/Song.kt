package com.flconverter.domain

class Note(
    val position: Long,
    val length: Long,
    val key: Int,
    val velocity: Int,
    val pan: Int,
    val channel: Int
) {
    fun moved(delta: Long) = Note(position + delta, length, key, velocity, pan, channel)
}

class Pattern(val index: Int, val notes: List<Note>)

class Placement(val pattern: Int, val start: Long, val length: Long)

class ChannelInfo(val index: Int, val name: String, val notes: Int)

class Song(
    val tempo: Double,
    val ppq: Int,
    val patterns: List<Pattern>,
    val channelNames: List<String> = emptyList(),
    val placements: List<Placement> = emptyList()
) {
    val noteCount: Int
        get() = patterns.sumOf { it.notes.size }

    val noteChannels: List<Int>
        get() = patterns.flatMap { pattern -> pattern.notes.map { it.channel } }.distinct().sorted()

    fun channelName(index: Int): String = channelNames.getOrNull(index).orEmpty().ifBlank { "Channel ${index + 1}" }

    fun channelInfos(): List<ChannelInfo> {
        val counts = patterns.flatMap { it.notes }.groupingBy { it.channel }.eachCount()
        return counts.keys.sorted().map { ChannelInfo(it, channelName(it), counts.getValue(it)) }
    }

    fun withoutChannels(excluded: Set<Int>): Song {
        if (excluded.isEmpty()) return this

        val kept = patterns
            .map { pattern -> Pattern(pattern.index, pattern.notes.filter { it.channel !in excluded }) }
            .filter { it.notes.isNotEmpty() }
        val indices = kept.map { it.index }.toSet()
        return Song(tempo, ppq, kept, channelNames, placements.filter { it.pattern in indices })
    }

    fun arrangement(): List<Placement> {
        val populated = patterns.filter { it.notes.isNotEmpty() }.map { it.index }.toSet()
        val placed = placements.filter { it.pattern in populated }
        if (placed.isNotEmpty()) return placed.sortedBy { it.start }

        val bar = ppq * 4L
        var cursor = 0L
        return patterns.filter { it.notes.isNotEmpty() }.map { pattern ->
            val end = pattern.notes.maxOf { it.position + it.length }
            val length = ((end + bar - 1) / bar) * bar
            Placement(pattern.index, cursor, length).also { cursor += length }
        }
    }
}

class SongSummary(
    val tempo: Double,
    val ppq: Int,
    val channels: List<ChannelInfo>,
    val patternCount: Int,
    val noteCount: Int,
    val previewNotes: List<Note>
)

internal fun mapChannels(source: List<Int>, targetCount: Int): Map<Int, Int> =
    source.distinct().sorted().mapIndexed { index, channel -> channel to minOf(index, targetCount - 1) }.toMap()
