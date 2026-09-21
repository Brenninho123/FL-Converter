package com.flconverter.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage

private const val CURSOR_SIZE = 32

private val musicIcon: PointerIcon by lazy {
    if (GraphicsEnvironment.isHeadless()) PointerIcon.Default else PointerIcon(createNoteCursor())
}

actual fun Modifier.musicCursor(): Modifier = pointerHoverIcon(musicIcon)

private fun createNoteCursor(): Cursor {
    val toolkit = Toolkit.getDefaultToolkit()
    val best = toolkit.getBestCursorSize(CURSOR_SIZE, CURSOR_SIZE)
    val size = maxOf(best.width, CURSOR_SIZE)
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val scale = size / 32.0
    g.scale(scale, scale)

    val head = Ellipse2D.Double(5.0, 21.0, 12.0, 8.5)
    val headShape = AffineTransform.getRotateInstance(Math.toRadians(-22.0), 11.0, 25.25).createTransformedShape(head)

    val flag = Path2D.Double().apply {
        moveTo(16.0, 3.0)
        curveTo(18.0, 8.0, 26.0, 9.0, 23.0, 19.0)
        curveTo(23.0, 13.0, 19.0, 12.0, 16.0, 12.0)
        closePath()
    }

    g.color = Color.WHITE
    g.stroke = BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g.draw(headShape)
    g.draw(flag)
    g.fillRect(14, 3, 3, 24)

    g.color = Color(0xFC, 0xA3, 0x11)
    g.fill(headShape)
    g.fill(flag)
    g.fillRect(15, 3, 2, 24)
    g.dispose()

    return toolkit.createCustomCursor(image, Point((4 * scale).toInt(), (6 * scale).toInt()), "music-note")
}
