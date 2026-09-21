package com.flconverter.domain

internal class FlpEvent(val id: Int, val data: ByteArray)

internal object FlpEvents {
    private const val BYTE_LIMIT = 64
    private const val WORD_LIMIT = 128
    private const val DWORD_LIMIT = 192

    fun parse(bytes: ByteArray): List<FlpEvent> {
        val reader = ByteReader(bytes)
        val events = ArrayList<FlpEvent>()

        while (reader.remaining > 0) {
            val id = reader.readUInt8()
            val size = when {
                id < BYTE_LIMIT -> 1
                id < WORD_LIMIT -> 2
                id < DWORD_LIMIT -> 4
                else -> reader.readVarLength()
            }
            events.add(FlpEvent(id, reader.readBytes(size)))
        }

        return events
    }
}
