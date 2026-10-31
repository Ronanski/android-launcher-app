package com.ronanski.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.mutableStateOf

/**
 * Only used to run "global actions" (lock screen, recents, split screen...).
 * It never reads window content.
 */
class LauncherAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        connected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        clear()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        clear()
        super.onDestroy()
    }

    private fun clear() {
        if (instance === this) instance = null
        connected.value = false
    }

    companion object {
        @Volatile
        var instance: LauncherAccessibilityService? = null
            private set

        val connected = mutableStateOf(false)

        fun perform(action: Int): Boolean = instance?.performGlobalAction(action) ?: false
    }
}
