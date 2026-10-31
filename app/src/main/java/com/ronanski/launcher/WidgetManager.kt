package com.ronanski.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

data class WidgetProvider(val info: AppWidgetProviderInfo, val label: String, val appLabel: String) {
    val key: String = info.provider.flattenToString() + "#" + info.profile.hashCode()
}

/** Hosts app widgets for the desktop. */
class WidgetManager(private val context: Context) {
    val manager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val host = AppWidgetHost(context, HOST_ID)

    fun providers(): List<WidgetProvider> {
        val pm = context.packageManager
        val users = context.getSystemService(UserManager::class.java).userProfiles
        return users.flatMap { user ->
            try {
                manager.getInstalledProvidersForProfile(user)
            } catch (e: Exception) {
                emptyList()
            }
        }.map { info ->
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
            } catch (e: Exception) {
                info.provider.packageName
            }
            WidgetProvider(info, info.loadLabel(pm) ?: appLabel, appLabel)
        }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))
    }

    fun info(widgetId: Int): AppWidgetProviderInfo? = try {
        manager.getAppWidgetInfo(widgetId)
    } catch (e: Exception) {
        null
    }

    fun allocate(): Int = host.allocateAppWidgetId()

    fun delete(widgetId: Int) {
        try {
            host.deleteAppWidgetId(widgetId)
        } catch (e: Exception) {
        }
    }

    fun bindIfAllowed(widgetId: Int, info: AppWidgetProviderInfo): Boolean = try {
        manager.bindAppWidgetIdIfAllowed(widgetId, info.profile, info.provider, null)
    } catch (e: Exception) {
        false
    }

    /** Size in desktop cells for a new widget. */
    fun spanFor(info: AppWidgetProviderInfo): Pair<Int, Int> {
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            info.targetCellWidth > 0 && info.targetCellHeight > 0
        ) {
            info.targetCellWidth to info.targetCellHeight
        } else {
            DesktopGrid.cellsFor(info.minWidth) to DesktopGrid.cellsFor(info.minHeight)
        }
        return w.coerceIn(1, DesktopGrid.columns) to h.coerceIn(1, DesktopGrid.rows)
    }

    /** Frees widget ids that are no longer on the desktop. */
    fun cleanup(desktop: List<DesktopItem>) {
        val used = desktop.filter { it.isWidget }.mapNotNull { it.key.toIntOrNull() }.toSet()
        try {
            host.appWidgetIds.filter { it !in used }.forEach { delete(it) }
        } catch (e: Exception) {
        }
    }

    fun startListening() = try {
        host.startListening()
    } catch (e: Exception) {
    }

    fun stopListening() = try {
        host.stopListening()
    } catch (e: Exception) {
    }

    companion object {
        const val HOST_ID = 2048
    }
}

/** Shows a hosted widget. */
@Composable
fun WidgetView(widgetId: Int, widgets: WidgetManager, width: Dp, height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val info = remember(widgetId) { widgets.info(widgetId) }
    if (info == null) {
        Box(
            modifier.background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("Widget unavailable", color = Ui.text) }
        return
    }
    val hostView: AppWidgetHostView = remember(widgetId) { widgets.host.createView(context, widgetId, info) }
    LaunchedEffect(width, height) {
        try {
            val w = width.value.toInt()
            val h = height.value.toInt()
            @Suppress("DEPRECATION")
            hostView.updateAppWidgetSize(Bundle(), w, h, w, h)
        } catch (e: Exception) {
        }
    }
    AndroidView(
        factory = {
            (hostView.parent as? ViewGroup)?.removeView(hostView)
            hostView
        },
        modifier = modifier,
    )
}

/** Full-screen list of widgets to add. */
@Composable
fun WidgetPicker(widgets: WidgetManager, onPick: (AppWidgetProviderInfo) -> Unit, onClose: () -> Unit) {
    val providers by produceState<List<WidgetProvider>?>(null) {
        value = withContext(Dispatchers.IO) { widgets.providers() }
    }
    var query by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(Ui.window.copy(alpha = 0.97f))
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Add a widget", style = MaterialTheme.typography.headlineSmall, color = Ui.text, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Close") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search widgets") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        val list = providers
        if (list == null) {
            Text("Loading widgets…", color = Ui.subtle)
        } else {
            val q = query.trim()
            val filtered = list.filter {
                q.isEmpty() || it.label.contains(q, true) || it.appLabel.contains(q, true)
            }
            if (filtered.isEmpty()) Text("No widgets found", color = Ui.subtle)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(200.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filtered, key = { it.key }) { provider ->
                    WidgetCard(provider, widgets) { onPick(provider.info) }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(provider: WidgetProvider, widgets: WidgetManager, onClick: () -> Unit) {
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(null, provider.key) {
        value = withContext(Dispatchers.IO) {
            try {
                val d: Drawable? = provider.info.loadPreviewImage(context, 0) ?: provider.info.loadIcon(context, 0)
                d?.let { scaled(it, 360) }
            } catch (e: Exception) {
                null
            }
        }
    }
    val span = remember(provider.key) { widgets.spanFor(provider.info) }
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Ui.text.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
            preview?.let {
                Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(provider.label, color = Ui.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${provider.appLabel} · ${span.first}×${span.second}",
            color = Ui.subtle,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun scaled(d: Drawable, maxPx: Int): ImageBitmap {
    val w = d.intrinsicWidth.takeIf { it > 0 } ?: maxPx
    val h = d.intrinsicHeight.takeIf { it > 0 } ?: maxPx
    val scale = min(1f, maxPx.toFloat() / max(w, h))
    return d.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
}
