package com.flconverter.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private class PendingRequest {
    var onPick: ((PickedFile?) -> Unit)? = null
    var onSave: ((Boolean) -> Unit)? = null
    var bytes: ByteArray? = null
}

@Composable
actual fun rememberFileHandler(): FileHandler {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = remember { PendingRequest() }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val callback = pending.onPick
        pending.onPick = null
        scope.launch {
            val file = uri?.let { withContext(Dispatchers.IO) { readFile(context, it) } }
            callback?.invoke(file)
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val callback = pending.onSave
        val bytes = pending.bytes
        pending.onSave = null
        pending.bytes = null
        scope.launch {
            val saved = uri != null && bytes != null &&
                withContext(Dispatchers.IO) { writeFile(context, uri, bytes) }
            callback?.invoke(saved)
        }
    }

    return remember(openLauncher, saveLauncher) {
        object : FileHandler {
            override fun pick(extension: String, onResult: (PickedFile?) -> Unit) {
                pending.onPick = onResult
                openLauncher.launch(arrayOf("*/*"))
            }

            override fun save(name: String, bytes: ByteArray, onResult: (Boolean) -> Unit) {
                pending.onSave = onResult
                pending.bytes = bytes
                saveLauncher.launch(name)
            }
        }
    }
}

private fun readFile(context: Context, uri: Uri): PickedFile? {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        PickedFile(displayName(context, uri), bytes)
    } catch (error: IOException) {
        null
    } catch (error: SecurityException) {
        null
    }
}

private fun writeFile(context: Context, uri: Uri, bytes: ByteArray): Boolean {
    return try {
        val stream = context.contentResolver.openOutputStream(uri) ?: return false
        stream.use { it.write(bytes) }
        true
    } catch (error: IOException) {
        false
    } catch (error: SecurityException) {
        false
    }
}

private fun displayName(context: Context, uri: Uri): String {
    val name = context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    return name ?: uri.lastPathSegment ?: "file"
}
