package com.flconverter.domain

internal class FlmChunk(val tag: String, val payload: ByteArray)

internal object FlmChunks {
    private const val PREFIX = 8

    fun parse(bytes: ByteArray, from: Int, to: Int): List<FlmChunk> {
        val chunks = ArrayList<FlmChunk>()
        var offset = from

        while (offset + PREFIX <= to) {
            val size = bytes.uint32(offset + 4)
            val start = offset + PREFIX
            if (size > to - start) throw ConversionException("FLM chunk is truncated")
            chunks.add(FlmChunk(bytes.ascii(offset, 4), bytes.copyOfRange(start, start + size.toInt())))
            offset = start + size.toInt()
        }

        return chunks
    }

    fun serialize(chunks: List<FlmChunk>): ByteArray {
        val out = ByteBuilder(chunks.sumOf { it.payload.size + PREFIX })
        for (chunk in chunks) {
            out.ascii(chunk.tag)
            out.u32(chunk.payload.size.toLong())
            out.bytes(chunk.payload)
        }
        return out.toByteArray()
    }
}
