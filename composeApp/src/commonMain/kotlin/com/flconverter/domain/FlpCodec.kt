package com.flconverter.domain

object FlpCodec : ProjectCodec {
    private const val HEADER_ID = "FLhd"
    private const val DATA_ID = "FLdt"
    private const val HEADER_SIZE = 6

    private class Container(val format: Int, val channels: Int, val ppq: Int, val events: ByteArray)

    override fun decode(bytes: ByteArray): FlProject {
        val container = readContainer(bytes)
        val song = FlpSongReader.read(FlpEvents.parse(container.events), container.ppq)
        return FlProject(container.format, container.channels, container.ppq, container.events, song)
    }

    override fun encode(project: FlProject, base: ByteArray?): ByteArray {
        val container = if (base != null) readContainer(base) else defaultContainer(project.song)
        val events = FlpSongWriter.write(project.song, FlpEvents.parse(container.events), container.ppq)
        val data = FlpEvents.serialize(events)

        val out = ByteBuilder(data.size + 32)
        out.ascii(HEADER_ID)
        out.u32(HEADER_SIZE.toLong())
        out.u16(container.format)
        out.u16(container.channels)
        out.u16(container.ppq)
        out.ascii(DATA_ID)
        out.u32(data.size.toLong())
        out.bytes(data)
        return out.toByteArray()
    }

    override fun capacity(base: ByteArray?, sourceChannels: Int): Int =
        if (base == null) sourceChannels else FlpSongWriter.capacity(FlpEvents.parse(readContainer(base).events))

    private fun defaultContainer(song: Song): Container {
        val names = song.noteChannels.map { song.channelName(it) }
        val events = FlpEvents.serialize(FlpDefaults.baseEvents(names))
        return Container(0, names.size, FlpDefaults.PPQ, events)
    }

    private fun readContainer(bytes: ByteArray): Container {
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

        return Container(format, channels, ppq, reader.readBytes(length.toInt()))
    }
}
