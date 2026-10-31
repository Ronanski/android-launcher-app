package com.ronanski.launcher

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject
import java.util.Collections
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * All launcher settings. Every property is backed by SharedPreferences *and*
 * Compose state, so changing a value in Settings instantly updates the home screen.
 */
class LauncherSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)

    private inner class Pref<T>(
        private val key: String,
        read: SharedPreferences.() -> T,
        private val write: SharedPreferences.Editor.(String, T) -> Unit,
    ) : ReadWriteProperty<Any?, T> {
        private val state = mutableStateOf(prefs.read())

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            state.value = value
            val editor = prefs.edit()
            editor.write(key, value)
            editor.apply()
        }
    }

    private fun intPref(key: String, def: Int) =
        Pref<Int>(key, { getInt(key, def) }, { k, v -> putInt(k, v) })

    private fun longPref(key: String, def: Long) =
        Pref<Long>(key, { getLong(key, def) }, { k, v -> putLong(k, v) })

    private fun boolPref(key: String, def: Boolean) =
        Pref<Boolean>(key, { getBoolean(key, def) }, { k, v -> putBoolean(k, v) })

    private fun stringPref(key: String, def: String) =
        Pref<String>(key, { getString(key, def) ?: def }, { k, v -> putString(k, v) })

    private fun listPref(key: String) = Pref<List<String>>(
        key,
        { (getString(key, null) ?: "").split('\n').filter { it.isNotEmpty() } },
        { k, v -> putString(k, v.joinToString("\n")) },
    )

    private fun setPref(key: String) = Pref<Set<String>>(
        key,
        { getStringSet(key, null)?.toSet() ?: emptySet() },
        { k, v -> putStringSet(k, v) },
    )

    private fun mapPref(key: String) = Pref<Map<String, String>>(
        key,
        {
            val result = mutableMapOf<String, String>()
            val raw = getString(key, null)
            if (raw != null) {
                try {
                    val obj = JSONObject(raw)
                    obj.keys().forEach { result[it] = obj.getString(it) }
                } catch (e: Exception) {
                }
            }
            result
        },
        { k, v -> putString(k, JSONObject(v).toString()) },
    )

    private fun desktopPref(key: String) = Pref<List<DesktopItem>>(
        key,
        { DesktopItem.parseList(getString(key, null)) },
        { k, v -> putString(k, DesktopItem.toJson(v)) },
    )

    // ---- Desktop ----
    var desktop by desktopPref("desktop")
    /** 0 = automatic (based on screen width). */
    var desktopColumns by intPref("desktop_columns", 0)
    var showDesktopLabels by boolPref("desktop_labels", true)
    var iconSize by intPref("icon_size", 52)
    var labelSize by intPref("label_size", 12)
    var labelShadow by boolPref("label_shadow", true)
    var labelColor by intPref("label_color", 0xFFFFFFFF.toInt())

    // ---- Taskbar & Start ----
    var taskbarPinned by listPref("taskbar_pinned")
    var startPinned by listPref("start_pinned")
    var taskbarCentered by boolPref("taskbar_centered", true)
    var showRecentApps by boolPref("taskbar_recents", true)
    var openInWindows by boolPref("open_in_windows", false)
    /** How long an app counts as "open" on the taskbar after it was last used. */
    var recentHours by intPref("recent_hours", 4)
    /** App key -> time the user closed it from Task View / taskbar (epoch ms as string). */
    var dismissedRecents by mapPref("dismissed_recents")
    var qsTiles by listPref("qs_tiles")
    var hideStatusBar by boolPref("hide_status_bar", true)
    var hideNavBar by boolPref("hide_nav_bar", false)
    var clock24h by boolPref("clock_24h", false)
    var startColumns by intPref("start_columns", 6)
    var panelOpacity by intPref("panel_opacity", 96)
    var accentColor by intPref("accent_color", 0xFF4CC2FF.toInt())

    // ---- Look ----
    var font by stringPref("font", "default")
    var fontVersion by intPref("font_version", 0)
    var iconPack by stringPref("icon_pack", "")
    var iconShape by stringPref("icon_shape", "original")

    // ---- Gestures ----
    var gestureSwipeUp by stringPref("gesture_up", GestureAction.APP_DRAWER.name)
    var gestureSwipeDown by stringPref("gesture_down", GestureAction.NOTIFICATIONS.name)
    var gestureDoubleTap by stringPref("gesture_double_tap", GestureAction.LOCK_SCREEN.name)

    // ---- Updates ----
    var autoUpdateCheck by boolPref("auto_update_check", true)
    var lastUpdateCheck by longPref("last_update_check", 0L)

    // ---- Apps ----
    /** Home apps from v0.1/v0.2; migrated to the desktop, Start and taskbar. */
    private var pinned by listPref("pinned")
    var hidden by setPref("hidden")
    var renames by mapPref("renames")

    init {
        if (!prefs.contains("desktop") && pinned.isNotEmpty()) {
            desktop = pinned.mapIndexed { i, key -> DesktopItem(DesktopItem.APP, key, i / 5, i % 5) }
            if (taskbarPinned.isEmpty()) taskbarPinned = pinned.take(6)
            if (startPinned.isEmpty()) startPinned = pinned
        }
    }

    private fun toggled(list: List<String>, key: String) = if (key in list) list - key else list + key

    fun toggleTaskbar(key: String) {
        taskbarPinned = toggled(taskbarPinned, key)
    }

    fun toggleStart(key: String) {
        startPinned = toggled(startPinned, key)
    }

    fun moveInTaskbar(key: String, delta: Int) {
        val list = taskbarPinned.toMutableList()
        val from = list.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        Collections.swap(list, from, to)
        taskbarPinned = list
    }

    fun toggleHidden(key: String) {
        hidden = if (key in hidden) hidden - key else hidden + key
    }

    fun isOnDesktop(appKey: String) = desktop.any { !it.isWidget && it.key == appKey }

    fun addToDesktop(item: DesktopItem) {
        desktop = desktop + item
    }

    fun updateDesktop(id: String, transform: (DesktopItem) -> DesktopItem) {
        desktop = desktop.map { if (it.id == id) transform(it) else it }
    }

    fun removeFromDesktop(id: String) {
        desktop = desktop.filterNot { it.id == id }
    }

    fun rename(key: String, name: String?) {
        renames = if (name.isNullOrBlank()) renames - key else renames + (key to name.trim())
    }
}
