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
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val startOpen = mutableStateOf(false)
    private val searchFocus = mutableStateOf(false)
    private val widgetPicker = mutableStateOf(false)

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
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                // Back never closes the launcher; it only closes Start / the widget picker.
                BackHandler {
                    startOpen.value = false
                    widgetPicker.value = false
                }
                HomeScreen(
                    settings = app.settings,
                    repo = app.repo,
                    widgets = app.widgets,
                    startOpen = startOpen,
                    searchFocus = searchFocus,
                    widgetPicker = widgetPicker,
                    onAddWidget = ::addWidget,
                )
            }
        }
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
        launcherApp.updater.checkIfDue()
        launcherApp.repo.refreshRecents()
    }

    /** Pressing Home while already on the launcher closes Start and the widget picker. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        startOpen.value = false
        widgetPicker.value = false
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
