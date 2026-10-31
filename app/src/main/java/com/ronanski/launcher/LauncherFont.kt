package com.ronanski.launcher

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import androidx.compose.ui.text.font.Typeface as ComposeTypeface

/** key -> display name. Keys other than "default"/"custom" are Android system font family names. */
val FONT_OPTIONS = listOf(
    "default" to "System default",
    "sans-serif-light" to "Light",
    "sans-serif-thin" to "Thin",
    "sans-serif-medium" to "Medium",
    "sans-serif-condensed" to "Condensed",
    "sans-serif-smallcaps" to "Small caps",
    "serif" to "Serif",
    "monospace" to "Monospace",
    "casual" to "Casual",
    "cursive" to "Cursive",
    "custom" to "My font file (.ttf / .otf)",
)

object FontStore {
    private fun file(context: Context) = File(context.filesDir, "custom_font")

    /** Copies a font picked by the user into app storage. Returns false if it isn't a valid font. */
    fun import(context: Context, uri: Uri): Boolean {
        val target = file(context)
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            input.use { inp -> target.outputStream().use { inp.copyTo(it) } }
            Typeface.createFromFile(target)
            true
        } catch (e: Exception) {
            target.delete()
            false
        }
    }

    fun typeface(context: Context, key: String): Typeface = try {
        when (key) {
            "default" -> Typeface.DEFAULT
            "custom" -> file(context).takeIf { it.exists() }?.let { Typeface.createFromFile(it) }
                ?: Typeface.DEFAULT
            else -> Typeface.create(key, Typeface.NORMAL)
        }
    } catch (e: Exception) {
        Typeface.DEFAULT
    }
}

@Composable
fun rememberLauncherFont(settings: LauncherSettings): FontFamily {
    val context = LocalContext.current
    val key = settings.font
    val version = settings.fontVersion
    return remember(key, version) {
        if (key == "default") FontFamily.Default
        else FontFamily(ComposeTypeface(FontStore.typeface(context, key)))
    }
}
