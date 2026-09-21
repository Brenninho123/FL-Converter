package com.flconverter.domain

class Note(
    val position: Long,
    val length: Long,
    val key: Int,
    val velocity: Int,
    val pan: Int,
    val channel: Int
)

class Pattern(val index: Int, val notes: List<Note>)

class Placement(val pattern: Int, val start: Long, val length: Long)

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
    val channels: Int,
    val patternCount: Int,
    val noteCount: Int,
    val previewNotes: List<Note>
)

internal fun mapChannels(source: List<Int>, targetCount: Int): Map<Int, Int> =
    source.distinct().sorted().mapIndexed { index, channel -> channel to minOf(index, targetCount - 1) }.toMap()
