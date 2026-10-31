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
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                )
            context.startActivity(intent)
        } catch (e: Exception) {
            openFullscreen(context, app)
        }
    }

    fun appInfo(context: Context, app: AppInfo) {
        try {
            launcherApps(context).startAppDetailsActivity(app.component, app.user, null, null)
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
