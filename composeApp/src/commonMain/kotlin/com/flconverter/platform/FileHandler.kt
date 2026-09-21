package com.flconverter.platform

import androidx.compose.runtime.Composable

class PickedFile(val name: String, val bytes: ByteArray)

interface FileHandler {
    fun pick(extension: String, onResult: (PickedFile?) -> Unit)
    fun save(name: String, bytes: ByteArray, onResult: (Boolean) -> Unit)
}

@Composable
expect fun rememberFileHandler(): FileHandler
