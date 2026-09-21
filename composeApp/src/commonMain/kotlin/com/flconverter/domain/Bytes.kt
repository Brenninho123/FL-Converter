package com.flconverter.domain

internal class ByteReader(private val bytes: ByteArray) {
    private var position = 0

    val remaining: Long
        get() = (bytes.size - position).toLong()

    fun readAscii(length: Int): String = readBytes(length).decodeToString()

    fun readUInt8(): Int {
        ensure(1)
        return bytes[position++].toInt() and 0xFF
    }

    fun readVarLength(): Int {
        var value = 0
        var shift = 0
        while (true) {
            val next = readUInt8()
            value = value or ((next and 0x7F) shl shift)
            if (next and 0x80 == 0) return value
            shift += 7
            if (shift > 28) throw ConversionException("Invalid event length")
        }
    }

    fun readUInt16(): Int {
        ensure(2)
        val value = (bytes[position].toInt() and 0xFF) or ((bytes[position + 1].toInt() and 0xFF) shl 8)
        position += 2
        return value
    }

    fun readUInt32(): Long {
        ensure(4)
        var value = 0L
        for (index in 0 until 4) {
            value = value or ((bytes[position + index].toLong() and 0xFF) shl (8 * index))
        }
        position += 4
        return value
    }

    fun readBytes(length: Int): ByteArray {
        ensure(length)
        val result = bytes.copyOfRange(position, position + length)
        position += length
        return result
    }

    private fun ensure(length: Int) {
        if (length > bytes.size - position) {
            throw ConversionException("Unexpected end of file")
        }
    }
}

internal class ByteBuilder(capacity: Int = 256) {
    private var buffer = ByteArray(capacity)

    var size = 0
        private set

    fun u8(value: Int) {
        ensure(1)
        buffer[size++] = value.toByte()
    }

    fun u16(value: Int) {
        u8(value)
        u8(value shr 8)
    }

    fun u32(value: Long) {
        for (index in 0 until 4) u8((value shr (8 * index)).toInt())
    }

    fun f64(value: Double) {
        val bits = value.toRawBits()
        for (index in 0 until 8) u8((bits shr (8 * index)).toInt())
    }

    fun ascii(value: String) = bytes(value.encodeToByteArray())

    fun bytes(value: ByteArray) {
        ensure(value.size)
        value.copyInto(buffer, size)
        size += value.size
    }

    fun varLength(value: Int) {
        var rest = value
        while (true) {
            val low = rest and 0x7F
            rest = rest ushr 7
            if (rest == 0) {
                u8(low)
                return
            }
            u8(low or 0x80)
        }
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)

    private fun ensure(extra: Int) {
        if (size + extra > buffer.size) {
            buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
        }
    }
}
