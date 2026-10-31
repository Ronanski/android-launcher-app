package com.ronanski.launcher

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** Small bridge between the activity and UI code. */
object LauncherUi {
    /** Incremented on every onResume so permission/status rows re-read their state. */
    val resumeTick = mutableIntStateOf(0)

    /** Set by MainActivity: opens the in-launcher Settings window. */
    var openSettings: ((SettingsPage?) -> Unit)? = null

    /** True while the accessibility service shows the taskbar over all apps. */
    val overlayActive = mutableStateOf(false)

    /** Set by the overlay taskbar: opens one of its panels (Start, etc.) above every app. */
    var overlayPanel: ((Panel) -> Unit)? = null

    const val EXTRA_SETTINGS_PAGE = "open_settings_page"

    /** Brings the launcher to the front and opens its Settings window (works from anywhere). */
    fun showSettings(context: Context, page: SettingsPage?) {
        try {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .putExtra(EXTRA_SETTINGS_PAGE, page?.name ?: "")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        } catch (e: Exception) {
        }
    }
}
