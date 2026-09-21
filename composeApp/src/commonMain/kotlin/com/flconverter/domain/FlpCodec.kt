package com.flconverter.domain

object FlpCodec : ProjectCodec {
    private const val HEADER_ID = "FLhd"
    private const val DATA_ID = "FLdt"
    private const val HEADER_SIZE = 6
    private const val CHUNK_PREFIX_SIZE = 8

    override fun decode(bytes: ByteArray): FlProject {
        val reader = ByteReader(bytes)

        if (reader.readAscii(4) != HEADER_ID) {
            throw ConversionException("Not a valid FLP file")
        }
        if (reader.readUInt32() != HEADER_SIZE.toLong()) {
            throw ConversionException("Unsupported FLP header")
        }

        val format = reader.readUInt16()
        val channels = reader.readUInt16()
        val ppq = reader.readUInt16()

        if (reader.readAscii(4) != DATA_ID) {
            throw ConversionException("Missing FLP data chunk")
        }

        val length = reader.readUInt32()
        if (length > reader.remaining) {
            throw ConversionException("FLP data chunk is truncated")
        }

        return FlProject(format, channels, ppq, reader.readBytes(length.toInt()))
    }

    override fun encode(project: FlProject): ByteArray {
        val writer = ByteWriter(CHUNK_PREFIX_SIZE + HEADER_SIZE + CHUNK_PREFIX_SIZE + project.events.size)

        writer.writeAscii(HEADER_ID)
        writer.writeUInt32(HEADER_SIZE.toLong())
        writer.writeUInt16(project.format)
        writer.writeUInt16(project.channels)
        writer.writeUInt16(project.ppq)
        writer.writeAscii(DATA_ID)
        writer.writeUInt32(project.events.size.toLong())
        writer.writeBytes(project.events)

        return writer.toByteArray()
    }
}
