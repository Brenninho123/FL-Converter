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

    override fun capacity(base: ByteArray?, sourceChannels: Int): Int =
        FlmSongWriter.capacity(base ?: DefaultFlm.bytes())

    override fun encode(project: FlProject, base: ByteArray?): ByteArray {
        try {
            return FlmSongWriter.write(project.song, base ?: DefaultFlm.bytes())
        } catch (error: IndexOutOfBoundsException) {
            throw ConversionException("The base FLM file is corrupted")
        }
    }
}
