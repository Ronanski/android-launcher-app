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

    private var overlay: NavGestureOverlay? = null
    private var taskbar: OverlayTaskbar? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        connected.value = true
        refreshOverlay()
    }

    /** Shows/hides the navigation gesture strips according to the launcher settings. */
    fun refreshOverlay() = try {
        refreshOverlayUnsafe()
    } catch (e: Exception) {
    }

    private fun refreshOverlayUnsafe() {
        val settings = (applicationContext as LauncherApp).settings
        val o = overlay ?: NavGestureOverlay(this).also { overlay = it }
        o.apply(settings.navGestures, settings.navGestureSize)
        if (settings.overlayTaskbar) {
            if (taskbar == null) taskbar = OverlayTaskbar(this).also { it.show() }
        } else {
            taskbar?.hide()
            taskbar = null
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        taskbar?.hide()
        taskbar = null
        refreshOverlay()
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
        overlay?.remove()
        overlay = null
        taskbar?.hide()
        taskbar = null
        if (instance === this) instance = null
        connected.value = false
    }

    companion object {
        @Volatile
        var instance: LauncherAccessibilityService? = null
            private set

        val connected = mutableStateOf(false)

        fun perform(action: Int): Boolean = instance?.performGlobalAction(action) ?: false

        /** Takes a screenshot after the flyout has closed. */
        fun screenshotSoon(context: android.content.Context) {
            if (instance == null || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.P) {
                Actions.toast(context, "Screenshot needs the accessibility service")
                Actions.openAccessibilitySettings(context)
                return
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                perform(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            }, 500)
        }
    }
}
