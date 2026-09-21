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

internal class ByteWriter(size: Int) {
    private val bytes = ByteArray(size)
    private var position = 0

    fun writeAscii(value: String) = writeBytes(value.encodeToByteArray())

    fun writeUInt16(value: Int) {
        bytes[position++] = value.toByte()
        bytes[position++] = (value shr 8).toByte()
    }

    fun writeUInt32(value: Long) {
        for (index in 0 until 4) {
            bytes[position++] = (value shr (8 * index)).toByte()
        }
    }

    fun writeBytes(value: ByteArray) {
        value.copyInto(bytes, position)
        position += value.size
    }

    fun toByteArray(): ByteArray = bytes
}
