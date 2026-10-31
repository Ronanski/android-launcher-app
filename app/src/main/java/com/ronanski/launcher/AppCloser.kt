package com.ronanski.launcher

import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Closes apps the Windows way ("End task"): opens the app's App info page and lets the
 * accessibility service press "Force stop" and confirm, then returns. Android gives launchers
 * no other way to close another app's window. Works one app at a time from a queue.
 */
object AppCloser {
    private enum class Stage { IDLE, FIND_BUTTON, CONFIRM }

    private val queue = ArrayDeque<String>()
    private var current: String? = null
    private var stage = Stage.IDLE
    private var deadline = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val FORCE_STOP_TEXTS = listOf("force stop", "sapilitang ihinto", "forzar detención", "forçar parada", "beenden erzwingen")
    private val CONFIRM_TEXTS = listOf("ok", "force stop", "yes", "sige", "aceptar")

    val busy: Boolean get() = stage != Stage.IDLE
    private const val TAG = "PadCloser"

    fun close(context: Context, packages: Collection<String>) {
        val pkgs = packages.filter { it != context.packageName }.distinct()
        if (pkgs.isEmpty()) return
        // Background processes can be stopped without any UI.
        val am = context.getSystemService(ActivityManager::class.java)
        pkgs.forEach {
            try {
                am?.killBackgroundProcesses(it)
            } catch (e: Exception) {
            }
        }
        if (LauncherAccessibilityService.instance == null) {
            Actions.toast(context, "Turn on the Pad Launcher accessibility service to close apps")
            Actions.openAccessibilitySettings(context)
            return
        }
        pkgs.forEach { if (it !in queue && it != current) queue.addLast(it) }
        if (stage == Stage.IDLE) next(context)
    }

    private fun next(context: Context) {
        val pkg = queue.removeFirstOrNull()
        if (pkg == null) {
            current = null
            stage = Stage.IDLE
            return
        }
        current = pkg
        stage = Stage.FIND_BUTTON
        Log.d(TAG, "closing $pkg: opening App info")
        deadline = System.currentTimeMillis() + 6000
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                    ),
            )
        } catch (e: Exception) {
            finishCurrent(context, goBack = false)
            return
        }
        // Safety net if the expected screens never show up.
        handler.postDelayed({ if (current == pkg && stage != Stage.IDLE) finishCurrent(context, goBack = true) }, 6500)
    }

    /** Called by the accessibility service for every window event while closing. */
    fun onEvent(service: AccessibilityService, event: AccessibilityEvent?) {
        if (stage == Stage.IDLE) return
        if (System.currentTimeMillis() > deadline) {
            finishCurrent(service, goBack = true)
            return
        }
        // Look in every window (the dialog may not be the "active" one on some ROMs).
        val roots = buildList {
            service.rootInActiveWindow?.let { add(it) }
            try {
                service.windows.mapNotNullTo(this) { it.root }
            } catch (e: Exception) {
            }
        }
        if (roots.isEmpty()) return
        when (stage) {
            Stage.FIND_BUTTON -> {
                val button = roots.firstNotNullOfOrNull { findByTexts(it, FORCE_STOP_TEXTS) }
                if (button == null) {
                    Log.d(TAG, "no Force stop yet: ${roots.size} windows, $lastVisited nodes (event from ${event?.packageName})")
                    return
                }
                Log.d(TAG, "found '${button.text}' enabled=${button.isEnabled} clickable=${button.isClickable}")
                if (!button.isEnabled) {
                    // Already stopped.
                    finishCurrent(service, goBack = true)
                    return
                }
                val clicked = click(button)
                Log.d(TAG, "clicked Force stop: $clicked")
                if (clicked) {
                    stage = Stage.CONFIRM
                    deadline = System.currentTimeMillis() + 4000
                }
            }
            Stage.CONFIRM -> {
                // The confirmation dialog: press its positive button ("OK" / "Force stop").
                val confirm = roots.firstNotNullOfOrNull { findByTexts(it, CONFIRM_TEXTS, exact = true, preferLast = true) }
                if (confirm == null) {
                    Log.d(TAG, "no confirm button yet")
                    return
                }
                val clicked = click(confirm)
                Log.d(TAG, "clicked confirm '${confirm.text}': $clicked")
                if (clicked) {
                    handler.postDelayed({ finishCurrent(service, goBack = true) }, 400)
                    stage = Stage.IDLE
                }
            }
            Stage.IDLE -> Unit
        }
    }

    private fun finishCurrent(context: Context, goBack: Boolean) {
        Log.d(TAG, "finished $current (stage $stage)")
        if (goBack) LauncherAccessibilityService.perform(AccessibilityService.GLOBAL_ACTION_BACK)
        current = null
        stage = Stage.IDLE
        if (queue.isNotEmpty()) handler.postDelayed({ next(context) }, 500)
    }

    /**
     * Walks the whole node tree (findAccessibilityNodeInfosByText misses some Settings layouts)
     * and returns nodes whose text or description matches.
     */
    private fun findByTexts(
        root: AccessibilityNodeInfo,
        texts: List<String>,
        exact: Boolean = false,
        preferLast: Boolean = false,
    ): AccessibilityNodeInfo? {
        val found = mutableListOf<AccessibilityNodeInfo>()
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var visited = 0
        while (stack.isNotEmpty() && visited < 3000) {
            val node = stack.removeLast()
            visited++
            val label = (node.text ?: node.contentDescription)?.toString()?.trim()?.lowercase()
            if (label != null && label.isNotEmpty()) {
                val match = if (exact) label in texts else texts.any { label.contains(it) }
                if (match && (!exact || node.isEnabled)) found += node
            }
            for (i in node.childCount - 1 downTo 0) {
                try {
                    node.getChild(i)?.let { stack.addLast(it) }
                } catch (e: Exception) {
                }
            }
        }
        lastVisited = visited
        return if (preferLast) found.lastOrNull() else found.firstOrNull()
    }

    private var lastVisited = 0

    /** Clicks the node or its nearest clickable parent. */
    private fun click(node: AccessibilityNodeInfo): Boolean {
        var n: AccessibilityNodeInfo? = node
        var depth = 0
        while (n != null && depth < 6) {
            if (n.isClickable) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            n = n.parent
            depth++
        }
        return false
    }
}
