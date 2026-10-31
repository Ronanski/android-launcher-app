package com.ronanski.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil

/** Something placed on the desktop grid: an app icon (1x1) or a widget (w x h cells). */
data class DesktopItem(
    val type: String,
    /** App key for apps, appWidgetId for widgets. */
    val key: String,
    val x: Int,
    val y: Int,
    val w: Int = 1,
    val h: Int = 1,
) {
    val id: String get() = "$type:$key"
    val isWidget: Boolean get() = type == WIDGET

    fun overlaps(ox: Int, oy: Int, ow: Int, oh: Int): Boolean =
        x < ox + ow && ox < x + w && y < oy + oh && oy < y + h

    companion object {
        const val APP = "app"
        const val WIDGET = "widget"

        fun parseList(raw: String?): List<DesktopItem> {
            if (raw.isNullOrEmpty()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    DesktopItem(
                        o.getString("type"), o.getString("key"),
                        o.getInt("x"), o.getInt("y"), o.optInt("w", 1), o.optInt("h", 1),
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun toJson(items: List<DesktopItem>): String {
            val arr = JSONArray()
            items.forEach {
                arr.put(
                    JSONObject().put("type", it.type).put("key", it.key)
                        .put("x", it.x).put("y", it.y).put("w", it.w).put("h", it.h),
                )
            }
            return arr.toString()
        }
    }
}

/** Current desktop grid geometry (updated by the Desktop composable) and placement helpers. */
object DesktopGrid {
    var columns = 8
    var rows = 5
    var cellPx = 200f

    fun clampX(item: DesktopItem) = item.x.coerceIn(0, (columns - item.w).coerceAtLeast(0))
    fun clampY(item: DesktopItem) = item.y.coerceIn(0, (rows - item.h).coerceAtLeast(0))

    fun fits(items: List<DesktopItem>, x: Int, y: Int, w: Int, h: Int, ignoreId: String? = null): Boolean {
        if (x < 0 || y < 0 || x + w > columns || y + h > rows) return false
        return items.none { it.id != ignoreId && it.overlaps(x, y, w, h) }
    }

    /** First free spot, filling columns top-to-bottom like Windows. */
    fun findFree(items: List<DesktopItem>, w: Int, h: Int): Pair<Int, Int>? {
        for (x in 0..(columns - w)) {
            for (y in 0..(rows - h)) {
                if (fits(items, x, y, w, h)) return x to y
            }
        }
        return null
    }

    fun cellsFor(px: Int): Int = ceil(px / cellPx).toInt().coerceAtLeast(1)

    fun addApp(context: Context, settings: LauncherSettings, appKey: String) {
        if (settings.isOnDesktop(appKey)) {
            Actions.toast(context, "Already on the desktop")
            return
        }
        val pos = findFree(settings.desktop, 1, 1)
        if (pos == null) {
            Actions.toast(context, "The desktop is full")
            return
        }
        settings.addToDesktop(DesktopItem(DesktopItem.APP, appKey, pos.first, pos.second))
    }

    /** Re-arranges app icons alphabetically into free cells; widgets stay where they are. */
    fun sortApps(settings: LauncherSettings, repo: AppRepository) {
        val byKey = repo.apps.associateBy { it.key }
        var placed = settings.desktop.filter { it.isWidget }
        val apps = settings.desktop.filter { !it.isWidget }
            .sortedBy { item -> byKey[item.key]?.let { repo.label(it).lowercase() } ?: "~" }
        apps.forEach { item ->
            val pos = findFree(placed, 1, 1)
            placed = placed + if (pos != null) item.copy(x = pos.first, y = pos.second) else item
        }
        settings.desktop = placed
    }
}
