package com.ronanski.launcher

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.provider.Settings

/**
 * Opens apps as resizable windows (Android "freeform" mode). The window's title bar with
 * minimize / maximize / close is drawn by the system, and only exists when the device
 * supports freeform windows (Developer options → "Enable freeform windows").
 */
object WindowMode {
    private var cascade = 0

    fun supported(context: Context): Boolean {
        val pm = context.packageManager
        val cr = context.contentResolver
        return pm.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT) ||
            Settings.Global.getInt(cr, "enable_freeform_support", 0) == 1
    }

    fun open(context: Context, app: AppInfo) {
        val dm = context.resources.displayMetrics
        val w = (dm.widthPixels * 0.6f).toInt()
        val h = (dm.heightPixels * 0.65f).toInt()
        val step = (36 * dm.density).toInt() * (cascade++ % 5)
        val left = (dm.widthPixels - w) / 2 - (72 * dm.density).toInt() + step
        val top = (dm.heightPixels - h) / 2 - (48 * dm.density).toInt() + step
        val bounds = Rect(left.coerceAtLeast(0), top.coerceAtLeast(0), left.coerceAtLeast(0) + w, top.coerceAtLeast(0) + h)

        val options = ActivityOptions.makeBasic().setLaunchBounds(bounds)
        try {
            // WINDOWING_MODE_FREEFORM = 5. Hidden API: works on some ROMs, ignored on others.
            ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType)
                .invoke(options, 5)
        } catch (e: Throwable) {
        }
        try {
            context.getSystemService(LauncherApps::class.java)
                .startMainActivity(app.component, app.user, bounds, options.toBundle())
        } catch (e: Exception) {
            AppLaunch.openFullscreen(context, app)
        }
    }

    fun openDeveloperOptions(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            Actions.toast(context, "Turn on Developer options first (tap MIUI/OS version 7 times)")
        }
    }
}
