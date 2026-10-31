package com.ronanski.launcher

import androidx.compose.runtime.mutableIntStateOf

/** Small bridge between the activity and UI code. */
object LauncherUi {
    /** Incremented on every onResume so permission/status rows re-read their state. */
    val resumeTick = mutableIntStateOf(0)

    /** Set by MainActivity: opens the in-launcher Settings window. */
    var openSettings: ((SettingsPage?) -> Unit)? = null
}
