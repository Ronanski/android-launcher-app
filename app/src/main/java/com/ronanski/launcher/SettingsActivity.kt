package com.ronanski.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Stand-alone Settings (used when the launcher itself isn't showing). */
class SettingsActivity : ComponentActivity() {
    private val page = mutableStateOf(SettingsPage.PERSONALIZATION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = launcherApp
        setContent {
            val colors = launcherColorsFor(app.settings)
            MaterialTheme(colorScheme = if (colors.isLight) lightColorScheme() else darkColorScheme()) {
                CompositionLocalProvider(LocalLauncherColors provides colors) {
                    Surface(Modifier.fillMaxSize(), color = colors.window) {
                        SettingsContent(app.settings, app.repo, app.updater, page, Modifier.fillMaxSize().systemBarsPadding())
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LauncherUi.resumeTick.intValue++
    }
}
