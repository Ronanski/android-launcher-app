package com.ronanski.launcher

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

/**
 * System-wide navigation gestures, drawn by the accessibility service as thin invisible
 * strips on the screen edges (works inside every app, not only on the desktop):
 *  - swipe up from the bottom            -> Home
 *  - swipe up from the bottom and hold    -> Recents
 *  - swipe in from the left or right edge -> Back
 */
class NavGestureOverlay(private val service: AccessibilityService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val views = mutableListOf<View>()

    fun apply(enabled: Boolean, edgeDp: Int) {
        remove()
        if (!enabled || wm == null) return
        val dm = service.resources.displayMetrics
        val thick = (edgeDp.coerceIn(6, 40) * dm.density).toInt()
        val sideHeight = (dm.heightPixels * 0.6f).toInt()
        add(Edge.BOTTOM, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, (dm.widthPixels * 0.7f).toInt(), thick)
        add(Edge.LEFT, Gravity.START or Gravity.CENTER_VERTICAL, thick, sideHeight)
        add(Edge.RIGHT, Gravity.END or Gravity.CENTER_VERTICAL, thick, sideHeight)
    }

    fun remove() {
        views.forEach {
            try {
                wm?.removeView(it)
            } catch (e: Exception) {
            }
        }
        views.clear()
    }

    private fun add(edge: Edge, gravity: Int, width: Int, height: Int) {
        val view = EdgeView(service, edge) { action -> service.performGlobalAction(action) }
        val params = WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = gravity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) params.fitInsetsTypes = 0
        try {
            wm?.addView(view, params)
            views += view
        } catch (e: Exception) {
        }
    }

    enum class Edge { BOTTOM, LEFT, RIGHT }

    @SuppressLint("ViewConstructor")
    private class EdgeView(context: Context, val edge: Edge, val onAction: (Int) -> Unit) : View(context) {
        private val threshold = 48 * resources.displayMetrics.density
        private val handler = Handler(Looper.getMainLooper())
        private var downX = 0f
        private var downY = 0f
        private var done = false
        private var holdArmed = false
        private val recents = Runnable {
            if (holdArmed && !done) {
                done = true
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    done = false
                    holdArmed = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (edge == Edge.BOTTOM && !done) {
                        val up = downY - event.rawY
                        if (up > threshold && !holdArmed) {
                            holdArmed = true
                            handler.postDelayed(recents, 350)
                        } else if (holdArmed && kotlin.math.abs(event.rawY - lastY) > 12 * resources.displayMetrics.density) {
                            // Still moving: restart the hold timer.
                            handler.removeCallbacks(recents)
                            handler.postDelayed(recents, 350)
                        }
                    }
                    lastY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(recents)
                    if (!done) {
                        val dx = event.rawX - downX
                        val dy = downY - event.rawY
                        val action = when (edge) {
                            Edge.BOTTOM -> if (dy > threshold) AccessibilityService.GLOBAL_ACTION_HOME else null
                            Edge.LEFT -> if (dx > threshold) AccessibilityService.GLOBAL_ACTION_BACK else null
                            Edge.RIGHT -> if (-dx > threshold) AccessibilityService.GLOBAL_ACTION_BACK else null
                        }
                        if (action != null) {
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onAction(action)
                        }
                    }
                }
                MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(recents)
            }
            return true
        }

        private var lastY = 0f
    }
}

/**
 * Xiaomi hides its 3-button bar when the global setting "force_fsg_nav_bar" is 1. Writing it
 * needs WRITE_SECURE_SETTINGS, which can only be granted once from a computer with adb.
 */
object XiaomiNavBar {
    const val ADB_COMMAND = "adb shell pm grant com.ronanski.launcher android.permission.WRITE_SECURE_SETTINGS"

    fun canChange(context: Context): Boolean =
        context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

    fun isHidden(context: Context): Boolean =
        Settings.Global.getInt(context.contentResolver, "force_fsg_nav_bar", 0) == 1

    fun setHidden(context: Context, hidden: Boolean): Boolean = try {
        Settings.Global.putInt(context.contentResolver, "force_fsg_nav_bar", if (hidden) 1 else 0)
        true
    } catch (e: Exception) {
        false
    }

    fun copyCommand(context: Context) {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("adb command", ADB_COMMAND))
        Actions.toast(context, "Command copied")
    }
}
