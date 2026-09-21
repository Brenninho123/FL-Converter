package com.flconverter.domain

internal fun ByteArray.uint8(offset: Int): Int = this[offset].toInt() and 0xFF

internal fun ByteArray.uint16(offset: Int): Int = uint8(offset) or (uint8(offset + 1) shl 8)

internal fun ByteArray.uint32(offset: Int): Long {
    var value = 0L
    for (index in 0 until 4) {
        value = value or (uint8(offset + index).toLong() shl (8 * index))
    }
    return value
}

internal fun ByteArray.float64(offset: Int): Double {
    val bits = (uint32(offset + 4) shl 32) or uint32(offset)
    return Double.fromBits(bits)
}

internal fun ByteArray.ascii(offset: Int, length: Int): String {
    val chars = CharArray(length) { this[offset + it].toInt().toChar() }
    return chars.concatToString()
}

internal fun ByteArray.utf16(): String {
    val length = size / 2
    val chars = CharArray(length) { uint16(it * 2).toChar() }
    return chars.concatToString().substringBefore('\u0000')
}

internal fun String.toUtf16(): ByteArray {
    val out = ByteBuilder(length * 2 + 2)
    forEach { out.u16(it.code) }
    out.u16(0)
    return out.toByteArray()
}

internal fun hexBytes(hex: String): ByteArray {
    val clean = hex.filter { !it.isWhitespace() }
    return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
