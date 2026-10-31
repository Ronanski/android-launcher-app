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

    // ---- Layout ----
    var homeColumns by intPref("home_columns", 5)
    var drawerColumns by intPref("drawer_columns", 6)
    var iconSize by intPref("icon_size", 56)
    var labelSize by intPref("label_size", 12)
    var showHomeLabels by boolPref("home_labels", true)
    var showDrawerLabels by boolPref("drawer_labels", true)
    var labelShadow by boolPref("label_shadow", true)
    var labelColor by intPref("label_color", 0xFFFFFFFF.toInt())
    var drawerOpacity by intPref("drawer_opacity", 85)

    // ---- Clock ----
    var showClock by boolPref("show_clock", true)
    var clock24h by boolPref("clock_24h", false)

    // ---- Look ----
    var font by stringPref("font", "default")
    var fontVersion by intPref("font_version", 0)
    var iconPack by stringPref("icon_pack", "")
    var iconShape by stringPref("icon_shape", "original")

    // ---- Gestures ----
    var gestureSwipeUp by stringPref("gesture_up", GestureAction.APP_DRAWER.name)
    var gestureSwipeDown by stringPref("gesture_down", GestureAction.NOTIFICATIONS.name)
    var gestureDoubleTap by stringPref("gesture_double_tap", GestureAction.LOCK_SCREEN.name)

    // ---- Apps ----
    var pinned by listPref("pinned")
    var hidden by setPref("hidden")
    var renames by mapPref("renames")

    fun togglePin(key: String) {
        pinned = if (key in pinned) pinned - key else pinned + key
    }

    fun movePinned(key: String, delta: Int) {
        val list = pinned.toMutableList()
        val from = list.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        Collections.swap(list, from, to)
        pinned = list
    }

    fun toggleHidden(key: String) {
        if (key in hidden) {
            hidden = hidden - key
        } else {
            hidden = hidden + key
            pinned = pinned - key
        }
    }

    fun rename(key: String, name: String?) {
        renames = if (name.isNullOrBlank()) renames - key else renames + (key to name.trim())
    }
}
