package com.ronanski.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration

class MainActivity : ComponentActivity() {
    private val panel = mutableStateOf(Panel.NONE)
    private val settingsPage = mutableStateOf(SettingsPage.PERSONALIZATION)

    private var pendingWidgetId = -1
    private var pendingWidgetInfo: AppWidgetProviderInfo? = null

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) configureWidget() else cancelWidget()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val app = launcherApp
        app.widgets.cleanup(app.settings.desktop)
        handleSettingsExtra(intent)
        LauncherUi.openSettings = { page ->
            if (page != null) settingsPage.value = page
            panel.value = Panel.SETTINGS
        }
        setContent {
            val colors = launcherColorsFor(app.settings)
            MaterialTheme(colorScheme = if (colors.isLight) lightColorScheme() else darkColorScheme()) {
              CompositionLocalProvider(LocalLauncherColors provides colors) {
                // Shorter long-press than the system default, so app menus appear quickly.
                val base = LocalViewConfiguration.current
                val fast = remember(base) {
                    object : ViewConfiguration by base {
                        override val longPressTimeoutMillis: Long get() = 320L
                    }
                }
                // Hide Android's own status / navigation bars on the desktop if the user wants
                // (the taskbar already shows the time, battery and notifications).
                val hideStatus = app.settings.hideStatusBar
                val hideNav = app.settings.hideNavBar
                val tick = LauncherUi.resumeTick.intValue
                LaunchedEffect(hideStatus, hideNav, tick) { applySystemBars(hideStatus, hideNav) }
                CompositionLocalProvider(LocalViewConfiguration provides fast) {
                    // Back never closes the launcher; it only closes the open panel.
                    BackHandler { panel.value = Panel.NONE }
                    HomeScreen(
                        settings = app.settings,
                        repo = app.repo,
                        widgets = app.widgets,
                        panel = panel,
                        settingsPage = settingsPage,
                        onAddWidget = ::addWidget,
                    )
                }
              }
            }
        }
    }

    private fun applySystemBars(hideStatus: Boolean, hideNav: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hideStatus) controller.hide(WindowInsetsCompat.Type.statusBars()) else controller.show(WindowInsetsCompat.Type.statusBars())
        if (hideNav) controller.hide(WindowInsetsCompat.Type.navigationBars()) else controller.show(WindowInsetsCompat.Type.navigationBars())
    }

    override fun onStart() {
        super.onStart()
        launcherApp.widgets.startListening()
    }

    override fun onStop() {
        super.onStop()
        launcherApp.widgets.stopListening()
    }

    override fun onResume() {
        super.onResume()
        LauncherUi.resumeTick.intValue++
        launcherApp.updater.checkIfDue()
        launcherApp.repo.refreshRecents()
    }

    /** Pressing Home while already on the launcher closes Start and the widget picker. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        panel.value = Panel.NONE
        handleSettingsExtra(intent)
    }

    private fun handleSettingsExtra(intent: Intent?) {
        val page = intent?.getStringExtra(LauncherUi.EXTRA_SETTINGS_PAGE) ?: return
        intent.removeExtra(LauncherUi.EXTRA_SETTINGS_PAGE)
        SettingsPage.entries.firstOrNull { it.name == page }?.let { settingsPage.value = it }
        panel.value = Panel.SETTINGS
    }

    override fun onDestroy() {
        LauncherUi.openSettings = null
        super.onDestroy()
    }

    // ---- Adding widgets: allocate id -> bind permission -> optional configure -> place ----

    private fun addWidget(info: AppWidgetProviderInfo) {
        val widgets = launcherApp.widgets
        val id = widgets.allocate()
        pendingWidgetId = id
        pendingWidgetInfo = info
        if (widgets.bindIfAllowed(id, info)) {
            configureWidget()
        } else {
            try {
                bindWidget.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile),
                )
            } catch (e: Exception) {
                Actions.toast(this, "Can't add this widget")
                cancelWidget()
            }
        }
    }

    private fun configureWidget() {
        val info = pendingWidgetInfo ?: return
        if (info.configure != null) {
            try {
                launcherApp.widgets.host.startAppWidgetConfigureActivityForResult(
                    this, pendingWidgetId, 0, REQUEST_CONFIGURE_WIDGET, null,
                )
                return
            } catch (e: Exception) {
                // Fall through and add it without configuration.
            }
        }
        placeWidget()
    }

    @Deprecated("Needed for AppWidgetHost configuration results")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE_WIDGET) {
            if (resultCode == RESULT_OK) placeWidget() else cancelWidget()
        }
    }

    private fun placeWidget() {
        val info = pendingWidgetInfo ?: return
        val id = pendingWidgetId
        val app = launcherApp
        val (w, h) = app.widgets.spanFor(info)
        val pos = DesktopGrid.findFree(app.settings.desktop, w, h)
            ?: DesktopGrid.findFree(app.settings.desktop, 1, 1)
        if (pos == null) {
            Actions.toast(this, "No space on the desktop for this widget")
            cancelWidget()
            return
        }
        val fits = DesktopGrid.fits(app.settings.desktop, pos.first, pos.second, w, h)
        app.settings.addToDesktop(
            DesktopItem(
                DesktopItem.WIDGET, id.toString(), pos.first, pos.second,
                if (fits) w else 1, if (fits) h else 1,
            ),
        )
        if (!fits) Actions.toast(this, "Not enough space — long-press the widget → Resize")
        pendingWidgetInfo = null
        pendingWidgetId = -1
    }

    private fun cancelWidget() {
        if (pendingWidgetId != -1) launcherApp.widgets.delete(pendingWidgetId)
        pendingWidgetInfo = null
        pendingWidgetId = -1
    }

    companion object {
        private const val REQUEST_CONFIGURE_WIDGET = 4201
    }
}
