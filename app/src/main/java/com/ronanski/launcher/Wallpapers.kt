package com.ronanski.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Original Windows-inspired wallpapers, drawn in code from the user's accent color
 * (no Microsoft artwork is included).
 */
object Wallpapers {
    val STYLES = listOf(
        "bloom" to "Bloom",
        "glow" to "Glow",
        "waves" to "Waves",
        "ribbon" to "Ribbon",
        "aurora" to "Aurora",
        "solid" to "Solid",
    )

    private fun shade(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun mix(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt(),
    )

    private fun withAlpha(color: Int, alpha: Int) = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    fun render(style: String, accent: Int, light: Boolean, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val w = width.toFloat()
        val h = height.toFloat()
        val m = max(w, h)
        val bgTop = if (light) mix(Color.WHITE, accent, 0.10f) else shade(accent, 0.10f)
        val bgBottom = if (light) mix(Color.WHITE, accent, 0.30f) else shade(accent, 0.22f)
        val bg = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, bgTop, bgBottom, Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, w, h, bg)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        when (style) {
            "bloom" -> {
                // Petals arranged around a center, overlapping with soft transparency.
                val cx = w * 0.5f
                val cy = h * 0.58f
                val petal = m * 0.22f
                for (i in 0 until 7) {
                    val angle = Math.toRadians(-90.0 + (i - 3) * 26.0)
                    c.save()
                    c.translate(cx, cy)
                    c.rotate(Math.toDegrees(angle).toFloat() + 90f)
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        cubicTo(-petal * 0.45f, -petal * 0.35f, -petal * 0.35f, -petal * 1.25f, 0f, -petal * 1.6f)
                        cubicTo(petal * 0.35f, -petal * 1.25f, petal * 0.45f, -petal * 0.35f, 0f, 0f)
                        close()
                    }
                    val t = i / 6f
                    p.shader = LinearGradient(
                        0f, 0f, 0f, -petal * 1.6f,
                        withAlpha(shade(accent, 0.6f + 0.2f * t), 230), withAlpha(mix(accent, Color.WHITE, 0.45f), 200),
                        Shader.TileMode.CLAMP,
                    )
                    c.drawPath(path, p)
                    c.restore()
                }
            }
            "glow" -> {
                p.shader = RadialGradient(w * 0.3f, h * 0.35f, m * 0.6f, withAlpha(accent, 200), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w, h, p)
                p.shader = RadialGradient(w * 0.8f, h * 0.75f, m * 0.5f, withAlpha(mix(accent, Color.MAGENTA, 0.35f), 160), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, w, h, p)
            }
            "waves" -> {
                for (i in 0 until 6) {
                    val path = Path()
                    val base = h * (0.45f + i * 0.09f)
                    val amp = h * 0.05f
                    path.moveTo(0f, h)
                    path.lineTo(0f, base)
                    var x = 0f
                    while (x <= w) {
                        path.lineTo(x, base + amp * sin((x / w) * 2 * Math.PI * 1.3 + i * 0.7).toFloat())
                        x += w / 60f
                    }
                    path.lineTo(w, h)
                    path.close()
                    p.shader = null
                    p.color = withAlpha(shade(accent, 0.45f + i * 0.1f), 120)
                    c.drawPath(path, p)
                }
            }
            "ribbon" -> {
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                for (i in 0 until 14) {
                    val path = Path()
                    path.moveTo(-w * 0.1f, h * (0.85f - i * 0.012f))
                    path.cubicTo(w * 0.3f, h * (0.2f + i * 0.02f), w * 0.6f, h * (1.0f - i * 0.02f), w * 1.1f, h * (0.25f + i * 0.015f))
                    p.strokeWidth = m * 0.012f
                    p.color = withAlpha(mix(accent, Color.WHITE, i / 20f), 90 + i * 8)
                    c.drawPath(path, p)
                }
                p.style = Paint.Style.FILL
            }
            "aurora" -> {
                p.maskFilter = BlurMaskFilter(m * 0.08f, BlurMaskFilter.Blur.NORMAL)
                val colors = listOf(accent, mix(accent, Color.GREEN, 0.5f), mix(accent, Color.MAGENTA, 0.5f))
                colors.forEachIndexed { i, col ->
                    val path = Path()
                    path.moveTo(0f, h * (0.3f + i * 0.12f))
                    var x = 0f
                    while (x <= w) {
                        path.lineTo(x, h * (0.3f + i * 0.12f) + h * 0.08f * cos((x / w) * Math.PI * 2 + i).toFloat())
                        x += w / 40f
                    }
                    path.lineTo(w, h * (0.45f + i * 0.12f))
                    path.lineTo(0f, h * (0.45f + i * 0.12f))
                    path.close()
                    p.color = withAlpha(col, 150)
                    c.drawPath(path, p)
                }
                p.maskFilter = null
            }
            else -> Unit // "solid": just the gradient background
        }
        return bmp
    }

    /** Draws the wallpaper at screen size and applies it to home and lock screen. */
    fun applyWallpaper(context: Context, style: String, accent: Int, light: Boolean): Boolean = try {
        val dm = context.resources.displayMetrics
        val size = max(dm.widthPixels, dm.heightPixels)
        // Square so it looks right in both orientations.
        val bmp = render(style, accent, light, size, size)
        WallpaperManager.getInstance(context).setBitmap(
            bmp, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
        )
        true
    } catch (e: Exception) {
        false
    }
}
