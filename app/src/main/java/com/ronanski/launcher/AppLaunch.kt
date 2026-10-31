package com.ronanski.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Process

object AppLaunch {
    private fun launcherApps(context: Context) = context.getSystemService(LauncherApps::class.java)

    /** Opens the app, as a window if "Open apps in windows" is on. */
    fun open(context: Context, app: AppInfo) {
        if (context.launcherApp.settings.openInWindows) {
            WindowMode.open(context, app)
        } else {
            openFullscreen(context, app)
        }
    }

    fun openFullscreen(context: Context, app: AppInfo) {
        try {
            launcherApps(context).startMainActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            Actions.toast(context, "Can't open ${app.label}")
        }
    }

    /**
     * Asks the system to open the app next to the current one. Works when the device
     * is already in split screen, and on some ROMs from fullscreen too.
     */
    fun openSplit(context: Context, app: AppInfo) {
        if (app.user != Process.myUserHandle()) {
            openFullscreen(context, app)
            return
        }
        try {
            val intent = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT,
                )
            context.startActivity(intent)
        } catch (e: Exception) {
            openFullscreen(context, app)
        }
    }

    private fun isAppInFront(context: Context): Boolean {
        val fg = LauncherUi.foregroundPackage
        return fg != null && fg != context.packageName && fg != "com.android.systemui"
    }

    /** Taskbar Split button: splits the app in front; then pick the second app from the taskbar. */
    fun splitCurrent(context: Context) {
        if (LauncherAccessibilityService.instance == null) {
            Actions.toast(context, "Turn on the Pad Launcher accessibility service for split screen")
            Actions.openAccessibilitySettings(context)
            return
        }
        if (!isAppInFront(context)) {
            android.util.Log.d("PadSplit", "no app in front (foreground=${LauncherUi.foregroundPackage})")
            Actions.toast(context, "Open an app first, then tap Split")
            return
        }
        val ok = LauncherAccessibilityService.perform(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
        android.util.Log.d("PadSplit", "toggle split for ${LauncherUi.foregroundPackage}: $ok")
        if (ok) {
            Actions.toast(context, "Now pick the second app")
        } else {
            Actions.toast(context, "This device didn't allow split screen")
        }
    }

    /** Puts [app] next to the app in front, like dragging an app to the side on recent tablets. */
    fun splitWithCurrent(context: Context, app: AppInfo) {
        if (LauncherAccessibilityService.instance == null) {
            Actions.toast(context, "Turn on the Pad Launcher accessibility service for split screen")
            Actions.openAccessibilitySettings(context)
            return
        }
        if (!isAppInFront(context) || LauncherUi.foregroundPackage == app.component.packageName) {
            Actions.toast(context, "Open another app first, then choose Split with current app")
            openFullscreen(context, app)
            return
        }
        LauncherAccessibilityService.perform(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ openSplit(context, app) }, 700)
    }

    fun appInfo(context: Context, app: AppInfo) {
        try {
            launcherApps(context).startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
        }
    }

    /** Opens Pad Launcher's own App info page (e.g. to grant a permission). */
    fun openAppDetails(context: Context) {
        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
        }
    }

    fun uninstall(context: Context, app: AppInfo) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.component.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
        }
    }

    /** App shortcuts (the "long-press" actions apps provide). Needs us to be the default launcher. */
    fun shortcuts(context: Context, app: AppInfo): List<ShortcutInfo> {
        val la = launcherApps(context)
        return try {
            if (!la.hasShortcutHostPermission()) return emptyList()
            val query = LauncherApps.ShortcutQuery()
                .setPackage(app.component.packageName)
                .setActivity(app.component)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
                )
            la.getShortcuts(query, app.user)?.sortedBy { it.rank } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun startShortcut(context: Context, shortcut: ShortcutInfo) {
        try {
            launcherApps(context).startShortcut(shortcut, null, null)
        } catch (e: Exception) {
            Actions.toast(context, "Can't open shortcut")
        }
    }
}
