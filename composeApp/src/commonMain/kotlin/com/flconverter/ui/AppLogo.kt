package com.flconverter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

private val TopArrow = listOf(30f to 40f, 66f to 40f, 66f to 32f, 82f to 44f, 66f to 56f, 66f to 48f, 30f to 48f)
private val BottomArrow = listOf(78f to 60f, 42f to 60f, 42f to 52f, 26f to 64f, 42f to 76f, 42f to 68f, 78f to 68f)

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val side = size.minDimension
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E2F57), Color(0xFF0E1730)),
                start = Offset.Zero,
                end = Offset(side, side)
            ),
            cornerRadius = CornerRadius(side * 0.22f)
        )
        drawArrow(TopArrow, Color.White, side)
        drawArrow(BottomArrow, Color(0xFFFCA311), side)
    }
}

private fun DrawScope.drawArrow(points: List<Pair<Float, Float>>, color: Color, side: Float) {
    val scale = side / 108f * 1.15f
    val middle = side / 2f
    val path = Path()
    points.forEachIndexed { index, (x, y) ->
        val px = middle + (x - 54f) * scale
        val py = middle + (y - 54f) * scale
        if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}
