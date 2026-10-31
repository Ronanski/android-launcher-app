package com.ronanski.launcher

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/** Recently used apps for the taskbar (needs the "Usage access" special permission). */
object RecentApps {
    fun hasPermission(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Package -> last used time, most recent first, for apps used in the last [hours]. */
    fun recentPackages(context: Context, hours: Int = 12): List<Pair<String, Long>> {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val now = System.currentTimeMillis()
        val since = now - hours * 3600 * 1000L
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 2 * 24 * 3600 * 1000L, now)
            ?: return emptyList()
        return stats
            .filter { it.lastTimeUsed > since && it.totalTimeInForeground > 0 && it.packageName != context.packageName }
            .groupBy { it.packageName }
            .map { (pkg, list) -> pkg to list.maxOf { it.lastTimeUsed } }
            .sortedByDescending { it.second }
            .take(16)
    }

    fun openSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
        }
    }
}
