package com.ronanski.launcher

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * The taskbar drawn by the accessibility service in an overlay window, so it (and Start, Search,
 * Task View, notifications and Quick Settings) shows above every app — including floating windows.
 * Collapsed it only covers the taskbar strip; when a panel opens it grows to full screen.
 */
class OverlayTaskbar(private val service: AccessibilityService) : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private val wm = service.getSystemService(WindowManager::class.java)
    private val panel = mutableStateOf(Panel.NONE)
    private var root: FrameLayout? = null
    private var params: WindowManager.LayoutParams? = null

    fun show() {
        if (root != null || wm == null) return
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        val compose = ComposeView(service).apply { setContent { OverlayContent() } }
        val frame = BackAwareFrame(service) { if (panel.value != Panel.NONE) { panel.value = Panel.NONE; true } else false }
        frame.addView(compose, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        frame.setViewTreeLifecycleOwner(this)
        frame.setViewTreeSavedStateRegistryOwner(this)

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.BOTTOM }
        try {
            wm.addView(frame, p)
            root = frame
            params = p
            LauncherUi.overlayActive.value = true
            LauncherUi.overlayPanel = { panel.value = it }
        } catch (e: Exception) {
        }
    }

    fun hide() {
        root?.let {
            try {
                wm?.removeView(it)
            } catch (e: Exception) {
            }
        }
        root = null
        LauncherUi.overlayActive.value = false
        LauncherUi.overlayPanel = null
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    /** Full screen and focusable (for typing in Search) while a panel is open; a thin strip otherwise. */
    private fun setExpanded(expanded: Boolean) {
        val p = params ?: return
        val r = root ?: return
        p.height = if (expanded) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT
        p.flags = if (expanded) {
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        try {
            wm?.updateViewLayout(r, p)
        } catch (e: Exception) {
        }
    }

    @Composable
    private fun OverlayContent() {
        val app = service.applicationContext as LauncherApp
        val settings = app.settings
        val repo = app.repo
        val colors = launcherColorsFor(settings)
        val current = panel.value
        LaunchedEffect(current) {
            setExpanded(current != Panel.NONE)
            if (current == Panel.TASK_VIEW || current == Panel.NONE) repo.refreshRecents()
        }
        MaterialTheme(colorScheme = if (colors.isLight) lightColorScheme() else darkColorScheme()) {
            CompositionLocalProvider(LocalLauncherColors provides colors) {
                val font = rememberLauncherFont(settings)
                val close = { panel.value = Panel.NONE }
                Column(if (current != Panel.NONE) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
                    if (current != Panel.NONE) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (current) {
                                Panel.START, Panel.SEARCH -> StartMenu(
                                    settings = settings, repo = repo, font = font,
                                    focusSearch = current == Panel.SEARCH, onClose = close, onRename = { },
                                )
                                Panel.CALENDAR -> CalendarFlyout(settings, font, close)
                                Panel.QUICK -> QuickSettingsFlyout(settings, font, close)
                                Panel.TASK_VIEW -> TaskView(settings, repo, font, close)
                                else -> Unit
                            }
                        }
                    }
                    Taskbar(
                        settings = settings,
                        repo = repo,
                        font = font,
                        panel = current,
                        onPanel = { p ->
                            if (p == Panel.SETTINGS || p == Panel.WIDGETS) {
                                panel.value = Panel.NONE
                                LauncherUi.showSettings(service, null)
                            } else {
                                panel.value = p
                            }
                        },
                        onRename = { },
                        inOverlay = true,
                    )
                }
            }
        }
    }

    /** Closes the open panel on Back. */
    @SuppressLint("ViewConstructor")
    private class BackAwareFrame(context: Context, val onBack: () -> Boolean) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP && onBack()) return true
            return super.dispatchKeyEvent(event)
        }
    }
}
