package com.ronanski.launcher

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun panelColor(settings: LauncherSettings): Color =
    Color(0xFF1F1F1F).copy(alpha = settings.panelOpacity.coerceIn(0, 100) / 100f)

/** Battery level (0-100) and charging state, refreshed every 30 s. */
@Composable
fun rememberBattery(): Pair<Int, Boolean>? {
    val context = LocalContext.current
    var battery by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
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
            delay(30_000)
        }
    }
    return battery
}

@Composable
fun Taskbar(
    settings: LauncherSettings,
    repo: AppRepository,
    font: FontFamily,
    panel: Panel,
    onPanel: (Panel) -> Unit,
    onRename: (AppInfo) -> Unit,
) {
    val apps = repo.apps
    val byKey = remember(apps) { apps.associateBy { it.key } }
    // Windows-style: pinned apps first, then other open/recent apps — one row, running ones underlined.
    val running = if (settings.showRecentApps) repo.recentKeys.toSet() else emptySet()
    val pinned = settings.taskbarPinned.mapNotNull { byKey[it] }
    val others = if (settings.showRecentApps) {
        repo.recentKeys.filter { it !in settings.taskbarPinned }.mapNotNull { byKey[it] }.take(8)
    } else {
        emptyList()
    }
    val accent = Color(settings.accentColor)
    fun toggle(p: Panel) = onPanel(if (panel == p) Panel.NONE else p)

    // Start / Search / Task View are always visible; the apps area takes what's left of the
    // width and scrolls, so nothing overlaps the tray (portrait tablets and phones).
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .background(panelColor(settings))
            .navigationBarsPadding(),
    ) {
        val compact = maxWidth < 600.dp
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = if (compact) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (settings.taskbarNavButtons && !compact) {
                NavButtons(onPanel)
                Spacer(Modifier.width(4.dp))
            }
            Box(
                Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = if (settings.taskbarCentered) Alignment.Center else Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TaskbarButton(selected = panel == Panel.START, onClick = { toggle(Panel.START) }) { StartGlyph(accent, 24.dp) }
                    TaskbarButton(selected = panel == Panel.SEARCH, onClick = { toggle(Panel.SEARCH) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                    TaskbarButton(selected = panel == Panel.TASK_VIEW, onClick = { toggle(Panel.TASK_VIEW) }) {
                        TaskViewGlyph(Color.White, 22.dp)
                    }
                    Row(
                        Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        (pinned + others).forEach { app ->
                            key(app.key) {
                                TaskbarApp(app, repo, settings, accent, app.key in running, onRename) { onPanel(Panel.NONE) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(if (compact) 4.dp else 8.dp))
            SystemTray(settings, font, panel, ::toggle, compact)
        }
    }
}

/** Android Back / Home / Recents, run through the accessibility service. */
@Composable
private fun NavButtons(onPanel: (Panel) -> Unit) {
    val context = LocalContext.current
    fun global(action: Int) {
        onPanel(Panel.NONE)
        if (!LauncherAccessibilityService.perform(action)) {
            Actions.toast(context, "Turn on the Pad Launcher accessibility service for these buttons")
            Actions.openAccessibilitySettings(context)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        TaskbarButton(onClick = { onPanel(Panel.NONE) }) { BackGlyph(Color.White, 20.dp) }
        TaskbarButton(onClick = { global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME) }) {
            HomeGlyph(Color.White, 20.dp)
        }
        TaskbarButton(onClick = { global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS) }) {
            RecentsGlyph(Color.White, 20.dp)
        }
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
    running: Boolean,
    onRename: (AppInfo) -> Unit,
    onLaunched: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    Box {
        TaskbarButton(
            selected = running && repo.recentKeys.firstOrNull() == app.key,
            onClick = {
                AppLaunch.open(context, app)
                onLaunched()
            },
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                menu = true
            },
        ) {
            AppIconImage(app, repo, settings, 30.dp)
            if (running) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(if (repo.recentKeys.firstOrNull() == app.key) 16.dp else 6.dp, 3.dp)
                        .background(if (repo.recentKeys.firstOrNull() == app.key) accent else Color.White.copy(alpha = 0.6f), RoundedCornerShape(2.dp)),
                )
            }
        }
        val entries = buildList {
            add(MenuEntry("Open in window") {
                WindowMode.open(context, app)
                onLaunched()
            })
            addAll(pinEntries(context, app, settings, onRename))
            if (app.key in settings.taskbarPinned) {
                add(MenuEntry("Move left") { settings.moveInTaskbar(app.key, -1) })
                add(MenuEntry("Move right") { settings.moveInTaskbar(app.key, 1) })
            }
            if (running) add(MenuEntry("Close") { repo.closeRecent(listOf(app.key)) })
        }
        AppContextMenu(menu, { menu = false }, app, entries, onLaunched)
    }
}

@Composable
private fun SystemTray(
    settings: LauncherSettings,
    font: FontFamily,
    panel: Panel,
    toggle: (Panel) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000 - now % 15_000 + 50)
        }
    }
    val battery = rememberBattery()
    val tick = LauncherUi.resumeTick.intValue
    val wifi = remember(tick, panel) { SystemControls.wifiOn(context) }
    val is24h = settings.clock24h
    val timeFormat = remember(is24h) { SimpleDateFormat(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("M/d/yyyy", Locale.getDefault()) }
    val notifCount = NotificationStore.items.value.size
    // Tray text is sized in dp so a large system font size can't push it out of the taskbar.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val trayText = with(density) { 12.dp.toSp() }
    val badgeText = with(density) { 10.dp.toSp() }
    val accent = Color(settings.accentColor)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Wi-Fi + volume + battery -> Quick Settings (like Windows 11)
        Row(
            Modifier
                .fillMaxHeight()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (panel == Panel.QUICK) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                .clickable { toggle(Panel.QUICK) }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!compact) {
                WifiGlyph(if (wifi) Color.White else Color.White.copy(alpha = 0.35f), 16.dp)
                VolumeGlyph(Color.White, 16.dp)
            }
            battery?.let { (level, charging) -> BatteryGlyph(level, charging, Color.White) }
        }
        // Clock + date -> Notifications & calendar
        Row(
            Modifier
                .fillMaxHeight()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (panel == Panel.CALENDAR) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                .clickable { toggle(Panel.CALENDAR) }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    timeFormat.format(Date(now)), color = Color.White, fontSize = trayText, lineHeight = trayText * 1.2f,
                    fontFamily = font, textAlign = TextAlign.End, maxLines = 1, softWrap = false,
                )
                if (!compact) {
                    Text(
                        dateFormat.format(Date(now)), color = Color.White, fontSize = trayText, lineHeight = trayText * 1.2f,
                        fontFamily = font, textAlign = TextAlign.End, maxLines = 1, softWrap = false,
                    )
                }
            }
            if (notifCount > 0) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(20.dp).background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (notifCount > 9) "9+" else notifCount.toString(), color = Color.Black, fontSize = badgeText)
                }
            }
        }
    }
}
