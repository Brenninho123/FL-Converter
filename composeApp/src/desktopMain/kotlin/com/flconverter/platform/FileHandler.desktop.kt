package com.flconverter.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException

private class DesktopFileHandler : FileHandler {
    override fun pick(extension: String, onResult: (PickedFile?) -> Unit) {
        val file = showDialog(FileDialog.LOAD, "*.$extension") { name -> name.endsWith(".$extension", ignoreCase = true) }
        val picked = try {
            file?.let { PickedFile(it.name, it.readBytes()) }
        } catch (error: IOException) {
            null
        }
        onResult(picked)
    }

    override fun save(name: String, bytes: ByteArray, onResult: (Boolean) -> Unit) {
        val file = showDialog(FileDialog.SAVE, name) { true }
        val saved = try {
            file?.writeBytes(bytes) != null
        } catch (error: IOException) {
            false
        }
        onResult(saved)
    }

    private fun showDialog(mode: Int, initialName: String, accept: (String) -> Boolean): File? {
        val dialog = FileDialog(null as Frame?, "FL Converter", mode)
        dialog.file = initialName
        dialog.setFilenameFilter { _, name -> accept(name) }
        dialog.isVisible = true
        val name = dialog.file
        val directory = dialog.directory
        dialog.dispose()
        return if (name == null || directory == null) null else File(directory, name)
    }
}

@Composable
actual fun rememberFileHandler(): FileHandler = remember { DesktopFileHandler() }
