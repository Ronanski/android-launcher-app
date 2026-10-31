package com.ronanski.launcher

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors for the taskbar, Start, flyouts and Settings (Windows-style dark or light). */
@Immutable
data class LauncherColors(
    val isLight: Boolean,
    /** Taskbar / Start / flyout background (before opacity). */
    val panel: Color,
    /** Settings window background. */
    val window: Color,
    val text: Color,
    val subtle: Color,
)

val DarkLauncherColors = LauncherColors(
    isLight = false,
    panel = Color(0xFF1F1F1F),
    window = Color(0xFF202020),
    text = Color.White,
    subtle = Color(0xFF9E9E9E),
)

val LightLauncherColors = LauncherColors(
    isLight = true,
    panel = Color(0xFFF3F3F3),
    window = Color(0xFFF7F7F7),
    text = Color(0xFF1B1B1B),
    subtle = Color(0xFF616161),
)

val LocalLauncherColors = staticCompositionLocalOf { DarkLauncherColors }

@Composable
fun launcherColorsFor(settings: LauncherSettings): LauncherColors = when (settings.themeMode) {
    "light" -> LightLauncherColors
    "system" -> if (isSystemInDarkTheme()) DarkLauncherColors else LightLauncherColors
    else -> DarkLauncherColors
}

/** Shorthands used across the panels. */
object Ui {
    val text: Color
        @Composable @ReadOnlyComposable
        get() = LocalLauncherColors.current.text

    val subtle: Color
        @Composable @ReadOnlyComposable
        get() = LocalLauncherColors.current.subtle

    val window: Color
        @Composable @ReadOnlyComposable
        get() = LocalLauncherColors.current.window
}
