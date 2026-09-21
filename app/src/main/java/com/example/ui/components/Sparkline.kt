package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Primary

@Composable
fun Sparkline(
    points: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = Primary
) {
    if (points.size < 2) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val min = points.minOrNull() ?: 0.0
        val max = points.maxOrNull() ?: 1.0
        val range = (max - min).coerceAtLeast(0.1)
        
        val pad = 8.dp.toPx()
        val stepX = (width - pad * 2) / (points.size - 1)
        
        val path = Path()
        points.forEachIndexed { i, value ->
            val x = pad + i * stepX
            val y = height - pad - ((value - min) / range * (height - pad * 2)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        
        // Terminal dot matching mockup SVG
        if (points.isNotEmpty()) {
            val lastX = pad + (points.size - 1) * stepX
            val lastY = height - pad - ((points.last() - min) / range * (height - pad * 2)).toFloat()
            drawCircle(color = color, radius = 4.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(lastX, lastY))
        }
    }
}
