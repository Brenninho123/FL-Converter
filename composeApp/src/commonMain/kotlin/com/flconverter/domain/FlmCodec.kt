package com.flconverter.domain

object FlmCodec : ProjectCodec {
    override fun decode(bytes: ByteArray): FlProject {
        throw ConversionException("FLM reading is not implemented yet")
    }

    override fun encode(project: FlProject): ByteArray {
        throw ConversionException("FLM writing is not implemented yet")
    }
}
