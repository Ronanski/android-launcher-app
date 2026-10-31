package com.ronanski.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val drawerOpen = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val app = launcherApp
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                // Back never closes the launcher; it only closes the drawer.
                BackHandler { drawerOpen.value = false }
                HomeScreen(app.settings, app.repo, drawerOpen)
            }
        }
    }

    /** Pressing Home while already on the launcher closes the drawer. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerOpen.value = false
    }
}
