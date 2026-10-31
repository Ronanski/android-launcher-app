package com.ronanski.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator

data class AppInfo(
    /** Stable id: "package/activity#userSerial" */
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val info: LauncherActivityInfo,
)

class AppRepository(private val context: Context, private val settings: LauncherSettings) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val iconCache = LruCache<String, ImageBitmap>(500)
    private var refreshJob: Job? = null

    val iconPacks = IconPackManager(context)

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    /** Keys of recently used apps (taskbar), most recent first. */
    var recentKeys by mutableStateOf<List<String>>(emptyList())
        private set

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(
            packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
        ) = refresh()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
        ) = refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            val list = withContext(Dispatchers.IO) { loadApps() }
            iconCache.evictAll()
            apps = list
        }
    }

    private fun loadApps(): List<AppInfo> {
        val collator = Collator.getInstance()
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user).map { info ->
                AppInfo(
                    key = "${info.componentName.flattenToString()}#$serial",
                    label = info.label.toString(),
                    component = info.componentName,
                    user = user,
                    info = info,
                )
            }
        }
            .filter { it.component.packageName != context.packageName }
            .sortedWith(compareBy(collator) { it.label })
    }

    fun refreshRecents() {
        if (!settings.showRecentApps || !RecentApps.hasPermission(context)) {
            recentKeys = emptyList()
            return
        }
        scope.launch {
            val packages = withContext(Dispatchers.IO) {
                try {
                    RecentApps.recentPackages(context)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val me = android.os.Process.myUserHandle()
            recentKeys = packages.mapNotNull { pkg ->
                apps.firstOrNull { it.component.packageName == pkg && it.user == me }?.key
            }
        }
    }

    /** Label after applying the user's rename, if any. */
    fun label(app: AppInfo): String = settings.renames[app.key] ?: app.label

    suspend fun icon(app: AppInfo, sizePx: Int, iconPack: String, fullBleed: Boolean): ImageBitmap? =
        withContext(Dispatchers.IO) {
            val size = sizePx.coerceAtLeast(1)
            val cacheKey = "${app.key}|$iconPack|$size|$fullBleed"
            iconCache.get(cacheKey) ?: try {
                val drawable = iconPacks.getIcon(iconPack, app.component)
                    ?: app.info.getBadgedIcon(context.resources.displayMetrics.densityDpi)
                renderIcon(drawable, size, fullBleed).asImageBitmap().also { iconCache.put(cacheKey, it) }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Draws the icon to a bitmap. With [fullBleed], adaptive icons are drawn without the
     * system mask so our own shape (circle, squircle, teardrop...) can be applied cleanly.
     */
    private fun renderIcon(drawable: Drawable, size: Int, fullBleed: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (fullBleed && drawable is AdaptiveIconDrawable) {
            // Adaptive layers are 108dp with the visible area being the middle 72dp.
            val inset = size / 4
            listOfNotNull(drawable.background, drawable.foreground).forEach { layer ->
                layer.setBounds(-inset, -inset, size + inset, size + inset)
                layer.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
        }
        return bitmap
    }
}
