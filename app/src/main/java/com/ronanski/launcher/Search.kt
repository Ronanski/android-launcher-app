package com.ronanski.launcher

import android.Manifest
import android.app.SearchManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings

/** Extra search sources for Start search (Windows-style "best match" across more than apps). */
object Search {
    data class SettingsHit(val title: String, val action: String, val keywords: String)

    val ANDROID_SETTINGS = listOf(
        SettingsHit("Wi-Fi", Settings.ACTION_WIFI_SETTINGS, "wifi wireless internet network"),
        SettingsHit("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, "bluetooth pair headphones"),
        SettingsHit("Display & brightness", Settings.ACTION_DISPLAY_SETTINGS, "display brightness screen dark mode font"),
        SettingsHit("Sound & vibration", Settings.ACTION_SOUND_SETTINGS, "sound volume ringtone vibration"),
        SettingsHit("Battery", Intent.ACTION_POWER_USAGE_SUMMARY, "battery power saver"),
        SettingsHit("Apps", Settings.ACTION_APPLICATION_SETTINGS, "apps applications uninstall"),
        SettingsHit("Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "storage space memory"),
        SettingsHit("Notifications", Settings.ACTION_SETTINGS, "notifications alerts"),
        SettingsHit("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS, "accessibility"),
        SettingsHit("Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS, "location gps"),
        SettingsHit("Security & lock screen", Settings.ACTION_SECURITY_SETTINGS, "security lock password fingerprint face"),
        SettingsHit("Date & time", Settings.ACTION_DATE_SETTINGS, "date time clock timezone"),
        SettingsHit("Language & input", Settings.ACTION_LOCALE_SETTINGS, "language keyboard input"),
        SettingsHit("Developer options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, "developer usb debugging freeform"),
        SettingsHit("About tablet", Settings.ACTION_DEVICE_INFO_SETTINGS, "about phone tablet version model"),
        SettingsHit("Wallpaper", Intent.ACTION_SET_WALLPAPER, "wallpaper background"),
        SettingsHit("Airplane mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS, "airplane flight"),
        SettingsHit("Hotspot & tethering", Settings.ACTION_WIRELESS_SETTINGS, "hotspot tethering share"),
        SettingsHit("All settings", Settings.ACTION_SETTINGS, "settings system android"),
    )

    fun settingsHits(query: String): List<SettingsHit> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return ANDROID_SETTINGS.filter { it.title.lowercase().contains(q) || it.keywords.contains(q) }.take(4)
    }

    fun launcherPageHits(query: String): List<SettingsPage> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return SettingsPage.entries.filter { it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q) }.take(3)
    }

    fun openSetting(context: Context, hit: SettingsHit) {
        try {
            context.startActivity(Intent(hit.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            SystemControls.openSettings(context, Settings.ACTION_SETTINGS)
        }
    }

    fun webSearch(context: Context, query: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (e2: Exception) {
            }
        }
    }

    // ---- Contacts ----
    data class ContactHit(val id: Long, val name: String)

    fun hasContactsPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun contacts(context: Context, query: String): List<ContactHit> {
        if (query.trim().length < 2 || !hasContactsPermission(context)) return emptyList()
        val result = mutableListOf<ContactHit>()
        try {
            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?",
                arrayOf("%${query.trim()}%"),
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC",
            )?.use { cursor ->
                while (cursor.moveToNext() && result.size < 5) {
                    val name = cursor.getString(1)
                    if (name != null) result += ContactHit(cursor.getLong(0), name)
                }
            }
        } catch (e: Exception) {
        }
        return result
    }

    fun openContact(context: Context, hit: ContactHit) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, hit.id))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
        }
    }

    // ---- Calculator ----

    /** Evaluates simple math like "12*(3+4)/2" or "2^10". Returns null if it isn't a math expression. */
    fun calculate(input: String): String? {
        val expr = input.replace(" ", "").replace("×", "*").replace("÷", "/").replace(",", "")
        if (expr.isEmpty() || !expr.any { it in "+-*/^%" } || !expr.all { it.isDigit() || it in "+-*/^%()." }) return null
        return try {
            val parser = Parser(expr)
            val v = parser.parseExpression()
            if (parser.pos != expr.length || v.isNaN() || v.isInfinite()) null
            else if (v == Math.floor(v) && kotlin.math.abs(v) < 1e15) v.toLong().toString()
            else "%.8f".format(v).trimEnd('0').trimEnd('.')
        } catch (e: Exception) {
            null
        }
    }

    private class Parser(val s: String) {
        var pos = 0
        fun peek() = if (pos < s.length) s[pos] else '\u0000'

        fun parseExpression(): Double {
            var v = parseTerm()
            while (peek() == '+' || peek() == '-') {
                val op = s[pos++]
                val r = parseTerm()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        fun parseTerm(): Double {
            var v = parseFactor()
            while (peek() == '*' || peek() == '/' || peek() == '%') {
                val op = s[pos++]
                val r = parseFactor()
                v = when (op) {
                    '*' -> v * r
                    '/' -> v / r
                    else -> v % r
                }
            }
            return v
        }

        fun parseFactor(): Double {
            val base = parseUnary()
            if (peek() == '^') {
                pos++
                return Math.pow(base, parseFactor())
            }
            return base
        }

        fun parseUnary(): Double {
            if (peek() == '-') {
                pos++
                return -parseUnary()
            }
            if (peek() == '(') {
                pos++
                val v = parseExpression()
                if (peek() != ')') error("missing )")
                pos++
                return v
            }
            val start = pos
            while (peek().isDigit() || peek() == '.') pos++
            if (start == pos) error("number expected")
            return s.substring(start, pos).toDouble()
        }
    }
}
