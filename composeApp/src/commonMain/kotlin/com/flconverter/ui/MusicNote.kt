package com.flconverter.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate

fun DrawScope.drawEighthNote(center: Offset, height: Float, color: Color, angle: Float = 0f) {
    rotate(degrees = angle, pivot = center) {
        val headWidth = height * 0.44f
        val headHeight = height * 0.31f
        val head = Offset(center.x - height * 0.14f, center.y + height * 0.27f)
        val stemWidth = height * 0.075f
        val stemX = head.x + headWidth * 0.44f
        val stemTop = center.y - height * 0.5f

        rotate(degrees = -22f, pivot = head) {
            drawOval(
                color = color,
                topLeft = Offset(head.x - headWidth / 2, head.y - headHeight / 2),
                size = Size(headWidth, headHeight)
            )
        }

        drawRect(
            color = color,
            topLeft = Offset(stemX - stemWidth, stemTop),
            size = Size(stemWidth, head.y - stemTop)
        )

        val flag = Path().apply {
            moveTo(stemX - stemWidth, stemTop)
            cubicTo(
                stemX + height * 0.06f, stemTop + height * 0.16f,
                stemX + height * 0.36f, stemTop + height * 0.22f,
                stemX + height * 0.26f, stemTop + height * 0.56f
            )
            cubicTo(
                stemX + height * 0.24f, stemTop + height * 0.36f,
                stemX + height * 0.10f, stemTop + height * 0.30f,
                stemX - stemWidth, stemTop + height * 0.32f
            )
            close()
        }
        drawPath(flag, color)
    }
}
