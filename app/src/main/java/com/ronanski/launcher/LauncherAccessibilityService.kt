package com.ronanski.launcher

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.mutableStateOf

/**
 * Runs global actions (lock screen, recents, split screen...), hosts the overlay taskbar and
 * navigation gestures, and closes apps on request. It only reads window content while
 * AppCloser is closing an app the user picked.
 */
class LauncherAccessibilityService : AccessibilityService() {

    private var overlay: NavGestureOverlay? = null
    private var taskbar: OverlayTaskbar? = null

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            updateLockState()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        connected.value = true
        refreshOverlay()
        try {
            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            androidx.core.content.ContextCompat.registerReceiver(
                this, screenReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } catch (e: Exception) {
        }
        updateLockState()
    }

    /** No taskbar / Start on the lock screen. */
    private fun updateLockState() {
        val locked = try {
            getSystemService(android.app.KeyguardManager::class.java)?.isKeyguardLocked == true
        } catch (e: Exception) {
            false
        }
        taskbar?.setVisible(!locked)
    }

    /** Shows/hides the navigation gesture strips according to the launcher settings. */
    fun refreshOverlay() = try {
        refreshOverlayUnsafe()
    } catch (e: Exception) {
    }

    private fun refreshOverlayUnsafe() {
        val settings = (applicationContext as LauncherApp).settings
        val o = overlay ?: NavGestureOverlay(this).also { overlay = it }
        // With the overlay taskbar on, its Home button replaces the bottom swipe strip
        // (which would otherwise steal taps from the taskbar).
        o.apply(settings.navGestures, settings.navGestureSize, includeBottom = !settings.overlayTaskbar)
        if (settings.overlayTaskbar) {
            if (taskbar == null) taskbar = OverlayTaskbar(this).also { it.show() }
            updateLockState()
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Remember which app is in front (package name only) for split screen.
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (pkg != null && pkg != "com.android.systemui" && !pkg.contains("inputmethod")) {
                LauncherUi.foregroundPackage = pkg
            }
            updateLockState()
        }
        // Only reads screens while closing an app the user chose (see AppCloser).
        try {
            AppCloser.onEvent(this, event)
        } catch (e: Exception) {
        }
    }

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
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
        }
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
