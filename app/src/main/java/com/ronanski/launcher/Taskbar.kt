package com.ronanski.launcher

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun panelColor(settings: LauncherSettings): Color =
    Color(0xFF1F1F1F).copy(alpha = settings.panelOpacity.coerceIn(0, 100) / 100f)

@Composable
fun Taskbar(
    settings: LauncherSettings,
    repo: AppRepository,
    font: FontFamily,
    startOpen: Boolean,
    onStartClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRename: (AppInfo) -> Unit,
    onAppLaunched: () -> Unit,
) {
    val apps = repo.apps
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val pinned = settings.taskbarPinned.mapNotNull { byKey[it] }
    val recents = if (settings.showRecentApps) {
        repo.recentKeys.filter { it !in settings.taskbarPinned }.mapNotNull { byKey[it] }.take(6)
    } else {
        emptyList()
    }
    val accent = Color(settings.accentColor)

    Box(
        Modifier
            .fillMaxWidth()
            .background(panelColor(settings))
            .navigationBarsPadding()
            .height(52.dp)
            .padding(horizontal = 8.dp),
    ) {
        Row(
            Modifier.align(if (settings.taskbarCentered) Alignment.Center else Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TaskbarButton(selected = startOpen, onClick = onStartClick) { StartGlyph(accent, 24.dp) }
            TaskbarButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
            pinned.forEach { app ->
                key(app.key) { TaskbarApp(app, repo, settings, accent, false, onRename, onAppLaunched) }
            }
            if (recents.isNotEmpty()) {
                Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
            }
            recents.forEach { app ->
                key(app.key) { TaskbarApp(app, repo, settings, accent, true, onRename, onAppLaunched) }
            }
        }
        SystemTray(settings, font, Modifier.align(Alignment.CenterEnd))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskbarButton(
    selected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun TaskbarApp(
    app: AppInfo,
    repo: AppRepository,
    settings: LauncherSettings,
    accent: Color,
    recent: Boolean,
    onRename: (AppInfo) -> Unit,
    onAppLaunched: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    Box {
        TaskbarButton(
            onClick = {
                AppLaunch.open(context, app)
                onAppLaunched()
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                menu = true
            },
        ) {
            AppIconImage(app, repo, settings, 30.dp)
            if (recent) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(10.dp, 3.dp)
                        .background(accent, RoundedCornerShape(2.dp)),
                )
            }
        }
        val entries = buildList {
            addAll(pinEntries(context, app, settings, onRename))
            if (app.key in settings.taskbarPinned) {
                add(MenuEntry("Move left") { settings.moveInTaskbar(app.key, -1) })
                add(MenuEntry("Move right") { settings.moveInTaskbar(app.key, 1) })
            }
        }
        AppContextMenu(menu, { menu = false }, app, entries, onAppLaunched)
    }
}

@Composable
private fun SystemTray(settings: LauncherSettings, font: FontFamily, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var battery by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            battery = try {
                val intent: Intent? = ContextCompat.registerReceiver(
                    context, null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                if (level >= 0 && scale > 0) (level * 100 / scale) to charging else null
            } catch (e: Exception) {
                null
            }
            delay(30_000 - now % 30_000 + 50)
        }
    }
    val is24h = settings.clock24h
    val timeFormat = remember(is24h) { SimpleDateFormat(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("M/d/yyyy", Locale.getDefault()) }
    fun run(action: GestureAction) = Actions.perform(context, action) {}

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { run(GestureAction.QUICK_SETTINGS) }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            battery?.let { (level, charging) ->
                BatteryGlyph(level, charging, Color.White)
                Spacer(Modifier.width(4.dp))
                Text("$level%", color = Color.White, fontSize = 12.sp, fontFamily = font)
            }
        }
        Column(
            Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { run(GestureAction.NOTIFICATIONS) }
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(timeFormat.format(Date(now)), color = Color.White, fontSize = 12.sp, fontFamily = font, textAlign = TextAlign.End)
            Text(dateFormat.format(Date(now)), color = Color.White, fontSize = 12.sp, fontFamily = font, textAlign = TextAlign.End)
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable { run(GestureAction.NOTIFICATIONS) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/** Four rounded squares: the Start button logo. */
@Composable
fun StartGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val gap = s * 0.08f
        val cell = (s - gap) / 2
        val r = CornerRadius(s * 0.06f)
        for (i in 0..1) for (j in 0..1) {
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (cell + gap), j * (cell + gap)),
                size = Size(cell, cell),
                cornerRadius = r,
            )
        }
    }
}

@Composable
fun PowerGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = s * 0.1f
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(s * 0.15f, s * 0.15f),
            size = Size(s * 0.7f, s * 0.7f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(color, Offset(s / 2, s * 0.06f), Offset(s / 2, s * 0.45f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun BatteryGlyph(level: Int, charging: Boolean, color: Color) {
    Canvas(Modifier.size(22.dp, 12.dp)) {
        val w = size.width
        val h = size.height
        val body = w * 0.88f
        val stroke = h * 0.12f
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(body - stroke, h - stroke),
            cornerRadius = CornerRadius(h * 0.2f),
            style = Stroke(width = stroke),
        )
        drawRect(color, topLeft = Offset(body, h * 0.3f), size = Size(w - body, h * 0.4f))
        val inner = (body - stroke * 3) * (level.coerceIn(0, 100) / 100f)
        val fill = when {
            charging -> Color(0xFF6CCB5F)
            level <= 15 -> Color(0xFFFF6B6B)
            else -> color
        }
        drawRect(fill, topLeft = Offset(stroke * 1.5f, stroke * 1.5f), size = Size(inner, h - stroke * 3))
    }
}
