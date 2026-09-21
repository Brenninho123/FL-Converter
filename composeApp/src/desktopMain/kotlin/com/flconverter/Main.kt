package com.flconverter

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    val icon = remember { BitmapPainter(useResource("icon.png", ::loadImageBitmap)) }

    Window(onCloseRequest = ::exitApplication, title = "FL Converter", icon = icon) {
        App()
    }
}
