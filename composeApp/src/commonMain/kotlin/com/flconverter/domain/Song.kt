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
}

class SongSummary(
    val tempo: Double,
    val ppq: Int,
    val channels: Int,
    val patternCount: Int,
    val noteCount: Int,
    val previewNotes: List<Note>
)
