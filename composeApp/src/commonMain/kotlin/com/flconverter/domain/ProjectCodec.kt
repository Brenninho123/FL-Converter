package com.flconverter.domain

interface ProjectCodec {
    fun decode(bytes: ByteArray): FlProject
    fun encode(project: FlProject): ByteArray
}
