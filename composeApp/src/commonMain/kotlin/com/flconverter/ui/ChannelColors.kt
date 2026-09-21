package com.flconverter.ui

import androidx.compose.ui.graphics.Color

private val Palette = listOf(
    Color(0xFFFCA311),
    Color(0xFF5CD68D),
    Color(0xFF6EA8FE),
    Color(0xFFF778BA),
    Color(0xFFB794F6),
    Color(0xFF4FD1C5),
    Color(0xFFF6E05E),
    Color(0xFFFF8A65)
)

fun channelColor(index: Int): Color = Palette[index % Palette.size]
