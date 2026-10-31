package com.ronanski.launcher

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

enum class GestureAction(val label: String) {
    NONE("Do nothing"),
    APP_DRAWER("Open Start menu"),
    NOTIFICATIONS("Notification panel"),
    QUICK_SETTINGS("Quick settings"),
    LOCK_SCREEN("Lock screen (accessibility)"),
    RECENTS("Recent apps (accessibility)"),
    SPLIT_SCREEN("Toggle split screen (accessibility)"),
    POWER_MENU("Power menu (accessibility)"),
    LAUNCHER_SETTINGS("Launcher settings");

    companion object {
        fun from(name: String): GestureAction = entries.firstOrNull { it.name == name } ?: NONE
    }
}

object Actions {
    fun perform(context: Context, action: GestureAction, openDrawer: () -> Unit) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.APP_DRAWER -> openDrawer()
            GestureAction.NOTIFICATIONS ->
                if (!global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)) {
                    statusBar(context, "expandNotificationsPanel")
                }
            GestureAction.QUICK_SETTINGS ->
                if (!global(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)) {
                    statusBar(context, "expandSettingsPanel")
                }
            GestureAction.LOCK_SCREEN ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    needsService(context) { global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) }
                } else {
                    toast(context, "Lock screen needs Android 9+")
                }
            GestureAction.RECENTS ->
                needsService(context) { global(AccessibilityService.GLOBAL_ACTION_RECENTS) }
            GestureAction.SPLIT_SCREEN ->
                needsService(context) { global(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) }
            GestureAction.POWER_MENU ->
                needsService(context) { global(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG) }
            GestureAction.LAUNCHER_SETTINGS ->
                LauncherUi.showSettings(context, null)
        }
    }

    private fun global(action: Int): Boolean = LauncherAccessibilityService.perform(action)

    private inline fun needsService(context: Context, block: () -> Boolean) {
        if (LauncherAccessibilityService.instance == null) {
            toast(context, "Enable the Pad Launcher accessibility service first")
            openAccessibilitySettings(context)
        } else if (!block()) {
            toast(context, "Your system blocked this action")
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    private fun statusBar(context: Context, method: String) {
        try {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod(method).invoke(service)
        } catch (e: Exception) {
            toast(context, "Enable the accessibility service for this gesture")
        }
    }

    fun toast(context: Context, text: String) =
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}
