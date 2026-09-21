package com.flconverter.domain

internal class FlpEvent(val id: Int, val data: ByteArray) {
    companion object {
        fun word(id: Int, value: Int): FlpEvent {
            val out = ByteBuilder(2)
            out.u16(value)
            return FlpEvent(id, out.toByteArray())
        }

        fun dword(id: Int, value: Long): FlpEvent {
            val out = ByteBuilder(4)
            out.u32(value)
            return FlpEvent(id, out.toByteArray())
        }

        fun text(id: Int, value: String): FlpEvent = FlpEvent(id, value.toUtf16())
    }
}

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

    fun serialize(events: List<FlpEvent>): ByteArray {
        val out = ByteBuilder(events.sumOf { it.data.size + 3 })
        for (event in events) {
            out.u8(event.id)
            if (event.id >= DWORD_LIMIT) out.varLength(event.data.size)
            out.bytes(event.data)
        }
        return out.toByteArray()
    }
}
