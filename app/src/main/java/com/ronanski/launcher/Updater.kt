package com.ronanski.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val notes: String, val apkUrl: String, val pageUrl: String)

/** Checks GitHub Releases for a newer version, downloads the APK and opens the system installer. */
class Updater(private val context: Context, private val settings: LauncherSettings) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var checking = false

    val currentVersion: String = BuildConfig.VERSION_NAME

    var available by mutableStateOf<UpdateInfo?>(null)
        private set

    /** Download progress 0..1, or null when not downloading. */
    var progress by mutableStateOf<Float?>(null)
        private set

    var status by mutableStateOf("")
        private set

    /** Called whenever the home screen is shown; checks at most every 6 hours. */
    fun checkIfDue() {
        if (!settings.autoUpdateCheck) return
        if (System.currentTimeMillis() - settings.lastUpdateCheck < CHECK_INTERVAL_MS) return
        check(userInitiated = false)
    }

    fun check(userInitiated: Boolean) {
        if (checking) return
        checking = true
        status = "Checking for updates…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
            checking = false
            settings.lastUpdateCheck = System.currentTimeMillis()
            result.onSuccess { info ->
                available = info?.takeIf { isNewer(it.version, currentVersion) }
                status = available?.let { "Update available: v${it.version}" }
                    ?: "You're on the latest version (v$currentVersion)"
            }.onFailure {
                status = "Couldn't check for updates (no internet?)"
            }
            if (userInitiated) Actions.toast(context, status)
        }
    }

    fun downloadAndInstall() {
        val info = available ?: return
        if (progress != null) return
        if (!context.packageManager.canRequestPackageInstalls()) {
            Actions.toast(context, "Allow Pad Launcher to install apps, then tap Update again")
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (e: Exception) {
            }
            return
        }
        progress = 0f
        scope.launch {
            val file = withContext(Dispatchers.IO) { runCatching { download(info.apkUrl) }.getOrNull() }
            progress = null
            if (file == null) {
                Actions.toast(context, "Download failed. Try again.")
            } else {
                install(file)
            }
        }
    }

    private fun fetchLatest(): UpdateInfo? {
        val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/releases/latest")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val version = json.getString("tag_name").removePrefix("v")
            val assets = json.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    return UpdateInfo(
                        version = version,
                        notes = json.optString("body"),
                        apkUrl = asset.getString("browser_download_url"),
                        pageUrl = json.optString("html_url"),
                    )
                }
            }
            return null
        } finally {
            conn.disconnect()
        }
    }

    private fun download(apkUrl: String): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "PadLauncher-update.apk")
        val conn = URL(apkUrl).openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) progress = done.toFloat() / total
                    }
                }
            }
            return target
        } finally {
            conn.disconnect()
        }
    }

    private fun install(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            Actions.toast(context, "Couldn't open the installer")
        }
    }

    fun openReleasePage() {
        val page = available?.pageUrl?.takeIf { it.isNotEmpty() }
            ?: "https://github.com/${BuildConfig.GITHUB_REPO}/releases"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(page)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
        }
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

        /** Compares semantic versions like "0.10.2" vs "0.9.9". */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split(".").map { it.toIntOrNull() ?: 0 }
            val b = current.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
