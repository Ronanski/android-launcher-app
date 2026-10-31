package com.ronanski.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Simple vector glyphs drawn in code (no extra icon libraries needed).

/** Four rounded squares: the Start button logo. */
@Composable
fun StartGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val gap = s * 0.08f
        val cell = (s - gap) / 2
        for (i in 0..1) for (j in 0..1) {
            drawRoundRect(color, Offset(i * (cell + gap), j * (cell + gap)), Size(cell, cell), CornerRadius(s * 0.06f))
        }
    }
}

/** Two overlapping windows: Task View. */
@Composable
fun TaskViewGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.08f)
        drawRoundRect(color, Offset(s * 0.08f, s * 0.22f), Size(s * 0.6f, s * 0.56f), CornerRadius(s * 0.08f), style = stroke)
        drawRoundRect(color, Offset(s * 0.32f, s * 0.1f), Size(s * 0.6f, s * 0.56f), CornerRadius(s * 0.08f), style = stroke)
    }
}

@Composable
fun PowerGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.1f
        drawArc(color, -60f, 300f, false, Offset(s * 0.15f, s * 0.15f), Size(s * 0.7f, s * 0.7f), style = Stroke(w, cap = StrokeCap.Round))
        drawLine(color, Offset(s / 2, s * 0.06f), Offset(s / 2, s * 0.45f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun WifiGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.09f
        val cx = s / 2
        val cy = s * 0.85f
        for (r in listOf(0.75f, 0.5f, 0.25f)) {
            val rad = s * r
            drawArc(color, -135f, 90f, false, Offset(cx - rad, cy - rad), Size(rad * 2, rad * 2), style = Stroke(w, cap = StrokeCap.Round))
        }
        drawCircle(color, s * 0.07f, Offset(cx, cy))
    }
}

@Composable
fun VolumeGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val body = Path().apply {
            moveTo(s * 0.1f, s * 0.38f)
            lineTo(s * 0.28f, s * 0.38f)
            lineTo(s * 0.5f, s * 0.18f)
            lineTo(s * 0.5f, s * 0.82f)
            lineTo(s * 0.28f, s * 0.62f)
            lineTo(s * 0.1f, s * 0.62f)
            close()
        }
        drawPath(body, color)
        val w = s * 0.08f
        drawArc(color, -45f, 90f, false, Offset(s * 0.42f, s * 0.3f), Size(s * 0.4f, s * 0.4f), style = Stroke(w, cap = StrokeCap.Round))
        drawArc(color, -50f, 100f, false, Offset(s * 0.36f, s * 0.14f), Size(s * 0.72f, s * 0.72f), style = Stroke(w, cap = StrokeCap.Round))
    }
}

@Composable
fun BatteryGlyph(level: Int, charging: Boolean, color: Color, width: Dp = 22.dp, height: Dp = 12.dp) {
    Canvas(Modifier.size(width, height)) {
        val w = this.size.width
        val h = this.size.height
        val body = w * 0.88f
        val stroke = h * 0.12f
        drawRoundRect(color, Offset(stroke / 2, stroke / 2), Size(body - stroke, h - stroke), CornerRadius(h * 0.2f), style = Stroke(stroke))
        drawRect(color, Offset(body, h * 0.3f), Size(w - body, h * 0.4f))
        val inner = (body - stroke * 3) * (level.coerceIn(0, 100) / 100f)
        val fill = when {
            charging -> Color(0xFF6CCB5F)
            level <= 15 -> Color(0xFFFF6B6B)
            else -> color
        }
        drawRect(fill, Offset(stroke * 1.5f, stroke * 1.5f), Size(inner, h - stroke * 3))
    }
}

@Composable
fun BluetoothGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val p = Path().apply {
            moveTo(s * 0.28f, s * 0.3f)
            lineTo(s * 0.7f, s * 0.68f)
            lineTo(s * 0.5f, s * 0.86f)
            lineTo(s * 0.5f, s * 0.14f)
            lineTo(s * 0.7f, s * 0.32f)
            lineTo(s * 0.28f, s * 0.7f)
        }
        drawPath(p, color, style = Stroke(s * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun FlashlightGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val head = Path().apply {
            moveTo(s * 0.28f, s * 0.12f)
            lineTo(s * 0.72f, s * 0.12f)
            lineTo(s * 0.62f, s * 0.38f)
            lineTo(s * 0.38f, s * 0.38f)
            close()
        }
        drawPath(head, color)
        drawRoundRect(color, Offset(s * 0.38f, s * 0.42f), Size(s * 0.24f, s * 0.46f), CornerRadius(s * 0.04f))
    }
}

@Composable
fun RotateGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.08f
        drawArc(color, 20f, 290f, false, Offset(s * 0.18f, s * 0.18f), Size(s * 0.64f, s * 0.64f), style = Stroke(w, cap = StrokeCap.Round))
        val tip = Path().apply {
            moveTo(s * 0.86f, s * 0.36f)
            lineTo(s * 0.68f, s * 0.42f)
            lineTo(s * 0.86f, s * 0.56f)
            close()
        }
        drawPath(tip, color)
    }
}

@Composable
fun MoonGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val full = Path().apply { addOval(Rect(Offset(s * 0.15f, s * 0.15f), Size(s * 0.7f, s * 0.7f))) }
        val cut = Path().apply { addOval(Rect(Offset(s * 0.38f, s * 0.05f), Size(s * 0.6f, s * 0.6f))) }
        val moon = Path().apply { op(full, cut, PathOperation.Difference) }
        drawPath(moon, color)
    }
}

@Composable
fun AirplaneGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val p = Path().apply {
            moveTo(s * 0.5f, s * 0.06f)
            lineTo(s * 0.57f, s * 0.38f)
            lineTo(s * 0.92f, s * 0.56f)
            lineTo(s * 0.92f, s * 0.64f)
            lineTo(s * 0.57f, s * 0.55f)
            lineTo(s * 0.56f, s * 0.78f)
            lineTo(s * 0.68f, s * 0.88f)
            lineTo(s * 0.68f, s * 0.94f)
            lineTo(s * 0.5f, s * 0.89f)
            lineTo(s * 0.32f, s * 0.94f)
            lineTo(s * 0.32f, s * 0.88f)
            lineTo(s * 0.44f, s * 0.78f)
            lineTo(s * 0.43f, s * 0.55f)
            lineTo(s * 0.08f, s * 0.64f)
            lineTo(s * 0.08f, s * 0.56f)
            lineTo(s * 0.43f, s * 0.38f)
            close()
        }
        drawPath(p, color)
    }
}

@Composable
fun SunGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        drawCircle(color, s * 0.18f, c)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val x = kotlin.math.cos(a).toFloat()
            val y = kotlin.math.sin(a).toFloat()
            drawLine(color, c + Offset(x, y) * (s * 0.3f), c + Offset(x, y) * (s * 0.44f), s * 0.07f, StrokeCap.Round)
        }
    }
}

@Composable
fun SaverGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.08f
        drawRoundRect(color, Offset(s * 0.3f, s * 0.2f), Size(s * 0.4f, s * 0.68f), CornerRadius(s * 0.06f), style = Stroke(w))
        drawRect(color, Offset(s * 0.42f, s * 0.1f), Size(s * 0.16f, s * 0.1f))
        drawLine(color, Offset(s * 0.5f, s * 0.38f), Offset(s * 0.5f, s * 0.7f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.34f, s * 0.54f), Offset(s * 0.66f, s * 0.54f), w, StrokeCap.Round)
    }
}

@Composable
fun BackGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val p = Path().apply {
            moveTo(s * 0.72f, s * 0.2f)
            lineTo(s * 0.26f, s * 0.5f)
            lineTo(s * 0.72f, s * 0.8f)
            close()
        }
        drawPath(p, color, style = Stroke(s * 0.08f, join = StrokeJoin.Round))
    }
}

@Composable
fun HomeGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        drawCircle(color, s * 0.3f, Offset(s / 2, s / 2), style = Stroke(s * 0.08f))
    }
}

@Composable
fun RecentsGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(color, Offset(s * 0.22f, s * 0.22f), Size(s * 0.56f, s * 0.56f), CornerRadius(s * 0.08f), style = Stroke(s * 0.08f))
    }
}
