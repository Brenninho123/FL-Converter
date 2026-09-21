package com.flconverter.domain

interface ProjectCodec {
    fun decode(bytes: ByteArray): FlProject
    fun encode(project: FlProject, base: ByteArray?): ByteArray
    fun capacity(base: ByteArray?, sourceChannels: Int): Int
}
