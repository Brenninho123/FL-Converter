package com.flconverter.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import platform.posix.memcpy

private class PickerDelegate(private val onUrls: (List<NSURL>) -> Unit) :
    NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onUrls(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onUrls(emptyList())
    }
}

private class IosFileHandler : FileHandler {
    private var delegate: PickerDelegate? = null

    override fun pick(extension: String, onResult: (PickedFile?) -> Unit) {
        val types = listOfNotNull(UTType.typeWithFilenameExtension(extension), UTTypeData)
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
        present(picker) { urls ->
            val url = urls.firstOrNull()
            val data = url?.let { NSData.dataWithContentsOfURL(it) }
            onResult(if (url == null || data == null) null else PickedFile(url.lastPathComponent ?: "file", data.toByteArray()))
        }
    }

    override fun save(name: String, bytes: ByteArray, onResult: (Boolean) -> Unit) {
        val path = NSTemporaryDirectory() + name
        if (!bytes.toNSData().writeToFile(path, true)) {
            onResult(false)
            return
        }
        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(path)), asCopy = true)
        present(picker) { urls -> onResult(urls.isNotEmpty()) }
    }

    private fun present(picker: UIDocumentPickerViewController, onUrls: (List<NSURL>) -> Unit) {
        val pickerDelegate = PickerDelegate { urls ->
            delegate = null
            onUrls(urls)
        }
        delegate = pickerDelegate
        picker.delegate = pickerDelegate
        topViewController()?.presentViewController(picker, true, null)
    }

    private fun topViewController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (true) {
            val next = controller?.presentedViewController ?: break
            controller = next
        }
        return controller
    }
}

private fun NSData.toByteArray(): ByteArray {
    val result = ByteArray(length.toInt())
    if (result.isNotEmpty()) {
        result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
    return result
}

private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
}

@Composable
actual fun rememberFileHandler(): FileHandler = remember { IosFileHandler() }
