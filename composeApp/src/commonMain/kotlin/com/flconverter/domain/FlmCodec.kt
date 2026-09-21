package com.flconverter.domain

object FlmCodec : ProjectCodec {
    override fun decode(bytes: ByteArray): FlProject {
        val song = try {
            FlmSongReader.read(bytes)
        } catch (error: IndexOutOfBoundsException) {
            throw ConversionException("FLM file is corrupted")
        }
        return FlProject(0, song.channelNames.size, song.ppq, ByteArray(0), song)
    }

    override fun encode(project: FlProject): ByteArray {
        throw ConversionException("FLM writing is not implemented yet")
    }
}
