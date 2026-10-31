package com.ronanski.launcher

import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** A panel that pops up above the taskbar; tapping outside closes it. */
@Composable
fun Flyout(
    settings: LauncherSettings,
    alignment: Alignment,
    width: Dp,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Column(
            Modifier
                .align(alignment)
                .padding(12.dp)
                .width(width)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(panelColor(settings))
                .border(1.dp, Ui.text.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .pointerInput(Unit) { detectTapGestures { } },
            content = content,
        )
    }
}

// ---------------------------------------------------------------- Notifications + calendar

@Composable
fun CalendarFlyout(settings: LauncherSettings, font: FontFamily, onClose: () -> Unit) {
    val context = LocalContext.current
    val tick = LauncherUi.resumeTick.intValue
    val hasAccess = remember(tick) { NotificationStore.hasAccess(context) }
    val items = NotificationStore.visible(settings)
    val hiddenCount = NotificationStore.items.value.size - items.size
    val accent = Color(settings.accentColor)

    Flyout(settings, Alignment.BottomEnd, 380.dp, onClose) {
        // Notifications
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", color = Ui.text, fontWeight = FontWeight.SemiBold, fontFamily = font, modifier = Modifier.weight(1f))
            if (items.any { it.clearable }) {
                SmallButton("Clear all", font) { NotificationStore.clearAll() }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 80.dp, max = 380.dp)) {
            when {
                !hasAccess -> Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Turn on notification access so your notifications show here, like Windows.",
                        color = Ui.subtle, fontFamily = font,
                    )
                    Spacer(Modifier.height(8.dp))
                    SmallButton("Turn on", font) { NotificationStore.openSettings(context) }
                }
                items.isEmpty() -> Text(
                    if (hiddenCount > 0) "No new notifications ($hiddenCount hidden)" else "No new notifications",
                    color = Ui.subtle,
                    fontFamily = font,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
                else -> LazyColumn(Modifier.padding(horizontal = 12.dp)) {
                    items(items, key = { it.key }) { item ->
                        NotificationCard(
                            item, font,
                            onHideApp = { settings.hiddenNotificationApps = settings.hiddenNotificationApps + item.pkg },
                        ) {
                            NotificationStore.open(context, item)
                            onClose()
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.text.copy(alpha = 0.08f)))
        MonthCalendar(accent, font)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NotificationCard(item: NotifItem, font: FontFamily, onHideApp: () -> Unit, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appInfo = remember(item.pkg) {
        val pm = context.packageManager
        try {
            val ai = pm.getApplicationInfo(item.pkg, 0)
            pm.getApplicationLabel(ai).toString() to pm.getApplicationIcon(ai).toBitmap(48, 48).asImageBitmap()
        } catch (e: Exception) {
            item.pkg to null
        }
    }
    val (appName, icon: ImageBitmap?) = appInfo
    Box {
    androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
        androidx.compose.material3.DropdownMenuItem(text = { Text("Hide notifications from $appName") }, onClick = {
            menu = false
            onHideApp()
        })
        androidx.compose.material3.DropdownMenuItem(text = { Text("Notification settings for $appName") }, onClick = {
            menu = false
            NotificationStore.openAppNotificationSettings(context, item.pkg)
        })
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Ui.text.copy(alpha = 0.06f))
            .combinedClickable(onClick = onClick, onLongClick = { menu = true })
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "$appName · ${DateUtils.getRelativeTimeSpanString(item.time)}",
                color = Ui.subtle, fontSize = 12.sp, fontFamily = font,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (item.clearable) {
                Icon(
                    Icons.Default.Close, contentDescription = "Dismiss", tint = Ui.subtle,
                    modifier = Modifier.size(20.dp).clip(CircleShape).clickable { NotificationStore.dismiss(item) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        if (item.title.isNotBlank()) {
            Text(item.title, color = Ui.text, fontWeight = FontWeight.SemiBold, fontFamily = font, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (item.text.isNotBlank()) {
            Text(item.text, color = Ui.text.copy(alpha = 0.85f), fontSize = 13.sp, fontFamily = font, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
    }
}

@Composable
private fun MonthCalendar(accent: Color, font: FontFamily) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            color = Ui.text, fontWeight = FontWeight.SemiBold, fontFamily = font,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                color = Ui.text, fontFamily = font, modifier = Modifier.weight(1f),
            )
            SmallButton("‹", font) { month = month.minusMonths(1) }
            Spacer(Modifier.width(4.dp))
            SmallButton("›", font) { month = month.plusMonths(1) }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                Text(it, color = Ui.subtle, fontSize = 12.sp, fontFamily = font, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val offset = month.atDay(1).dayOfWeek.value % 7
        val days = month.lengthOfMonth()
        for (week in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (dow in 0 until 7) {
                    val day = week * 7 + dow - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1.3f), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val isToday = month.atDay(day) == today
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .background(if (isToday) accent else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.toString(),
                                    color = if (isToday) Color.Black else Ui.text,
                                    fontSize = 13.sp, fontFamily = font,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Quick settings

/** All available Quick Settings tiles; the user picks which show, and their order. */
val ALL_QS_TILES = listOf(
    "wifi" to "Wi-Fi", "bt" to "Bluetooth", "torch" to "Flashlight", "rotate" to "Auto-rotate",
    "dnd" to "Do not disturb", "saver" to "Battery saver", "air" to "Airplane mode", "location" to "Location",
    "hotspot" to "Hotspot", "display" to "Display", "sound" to "Sound", "screenshot" to "Screenshot",
    "lock" to "Lock", "split" to "Split screen", "settings" to "All settings",
)
private val DEFAULT_QS_TILES = listOf("wifi", "bt", "torch", "rotate", "dnd", "saver", "air", "location", "settings")

@Composable
fun QuickSettingsFlyout(settings: LauncherSettings, font: FontFamily, onClose: () -> Unit) {
    val context = LocalContext.current
    val accent = Color(settings.accentColor)
    val battery = rememberBattery()
    var refresh by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    val tick = LauncherUi.resumeTick.intValue
    var brightness by remember { mutableFloatStateOf(SystemControls.brightness(context)) }
    var volume by remember { mutableFloatStateOf(SystemControls.volume(context)) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        SystemControls.initTorch(context)
        while (true) {
            delay(1500)
            refresh++
            if (!dragging) {
                brightness = SystemControls.brightness(context)
                volume = SystemControls.volume(context)
            }
        }
    }
    val state = remember(refresh, tick) {
        mapOf(
            "wifi" to SystemControls.wifiOn(context),
            "bt" to SystemControls.bluetoothOn(context),
            "air" to SystemControls.airplaneOn(context),
            "saver" to SystemControls.batterySaverOn(context),
            "dnd" to SystemControls.dndOn(context),
            "rotate" to SystemControls.autoRotateOn(context),
            "location" to SystemControls.locationOn(context),
        )
    }
    val chosen = settings.qsTiles.ifEmpty { DEFAULT_QS_TILES }

    fun tile(id: String): QsTile? = when (id) {
        "wifi" -> QsTile("Wi-Fi", state["wifi"] == true, { c -> WifiGlyph(c, 20.dp) }) { SystemControls.openWifi(context) }
        "bt" -> QsTile("Bluetooth", state["bt"] == true, { c -> BluetoothGlyph(c, 20.dp) }) { SystemControls.openBluetooth(context) }
        "torch" -> QsTile("Flashlight", SystemControls.torchOn.value, { c -> FlashlightGlyph(c, 20.dp) }) { SystemControls.toggleTorch(context) }
        "rotate" -> QsTile("Auto-rotate", state["rotate"] == true, { c -> RotateGlyph(c, 20.dp) }) {
            SystemControls.toggleAutoRotate(context)
            refresh++
        }
        "dnd" -> QsTile("Do not disturb", state["dnd"] == true, { c -> MoonGlyph(c, 20.dp) }) {
            SystemControls.toggleDnd(context)
            refresh++
        }
        "saver" -> QsTile("Battery saver", state["saver"] == true, { c -> SaverGlyph(c, 20.dp) }) {
            SystemControls.openSettings(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
        }
        "air" -> QsTile("Airplane mode", state["air"] == true, { c -> AirplaneGlyph(c, 20.dp) }) {
            SystemControls.openSettings(context, Settings.ACTION_AIRPLANE_MODE_SETTINGS)
        }
        "location" -> QsTile("Location", state["location"] == true, { c ->
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = c, modifier = Modifier.size(20.dp))
        }) { SystemControls.openSettings(context, Settings.ACTION_LOCATION_SOURCE_SETTINGS) }
        "hotspot" -> QsTile("Hotspot", false, { c -> WifiGlyph(c, 20.dp) }) {
            SystemControls.openSettings(context, Settings.ACTION_WIRELESS_SETTINGS)
        }
        "display" -> QsTile("Display", false, { c -> SunGlyph(c, 20.dp) }) {
            SystemControls.openSettings(context, Settings.ACTION_DISPLAY_SETTINGS)
        }
        "sound" -> QsTile("Sound", false, { c -> VolumeGlyph(c, 20.dp) }) {
            SystemControls.openSettings(context, Settings.ACTION_SOUND_SETTINGS)
        }
        "screenshot" -> QsTile("Screenshot", false, { c -> TaskViewGlyph(c, 20.dp) }) {
            onClose()
            LauncherAccessibilityService.screenshotSoon(context)
        }
        "lock" -> QsTile("Lock", false, { c ->
            Icon(Icons.Default.Lock, contentDescription = null, tint = c, modifier = Modifier.size(20.dp))
        }) {
            onClose()
            Actions.perform(context, GestureAction.LOCK_SCREEN) {}
        }
        "split" -> QsTile("Split screen", false, { c -> TaskViewGlyph(c, 20.dp) }) {
            onClose()
            Actions.perform(context, GestureAction.SPLIT_SCREEN) {}
        }
        "settings" -> QsTile("All settings", false, { c ->
            Icon(Icons.Default.Settings, contentDescription = null, tint = c, modifier = Modifier.size(20.dp))
        }) { SystemControls.openSettings(context, Settings.ACTION_SETTINGS) }
        else -> null
    }

    Flyout(settings, Alignment.BottomEnd, 380.dp, onClose) {
        if (editing) {
            QsEditor(chosen, font, accent) { settings.qsTiles = it }
        } else {
            Column(Modifier.padding(16.dp)) {
                val tiles = chosen.mapNotNull { tile(it) }
                tiles.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t ->
                            Column(
                                Modifier.weight(1f).padding(bottom = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (t.on) accent else Ui.text.copy(alpha = 0.08f))
                                        .clickable { t.action() },
                                    contentAlignment = Alignment.Center,
                                ) { t.glyph(if (t.on) Color.Black else Ui.text) }
                                Spacer(Modifier.height(6.dp))
                                Text(t.label, color = Ui.text, fontSize = 12.sp, fontFamily = font, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                SliderRow({ SunGlyph(Ui.text, 20.dp) }, brightness, accent, { dragging = it }) {
                    brightness = it
                    SystemControls.setBrightness(context, it)
                }
                SliderRow({ VolumeGlyph(Ui.text, 20.dp) }, volume, accent, { dragging = it }) {
                    volume = it
                    SystemControls.setVolume(context, it)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ui.text.copy(alpha = 0.08f)))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            battery?.let { (level, charging) ->
                BatteryGlyph(level, charging, Ui.text)
                Spacer(Modifier.width(8.dp))
                Text(if (charging) "$level% · Charging" else "$level%", color = Ui.text, fontFamily = font, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            SmallButton(if (editing) "Done" else "Edit", font) { editing = !editing }
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.Settings, contentDescription = "Android quick settings", tint = Ui.text,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        onClose()
                        Actions.perform(context, GestureAction.QUICK_SETTINGS) {}
                    }
                    .padding(6.dp),
            )
        }
    }
}

/** Pick which tiles show and their order. */
@Composable
private fun QsEditor(chosen: List<String>, font: FontFamily, accent: Color, onChange: (List<String>) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Edit quick settings", color = Ui.text, fontWeight = FontWeight.SemiBold, fontFamily = font)
        Text("Tick tiles to show them; use the arrows to reorder.", color = Ui.subtle, fontSize = 12.sp, fontFamily = font)
        Spacer(Modifier.height(8.dp))
        val order = chosen + ALL_QS_TILES.map { it.first }.filter { it !in chosen }
        LazyColumn(Modifier.heightIn(max = 420.dp)) {
            items(order, key = { it }) { id ->
                val label = ALL_QS_TILES.firstOrNull { it.first == id }?.second ?: id
                val on = id in chosen
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = on,
                        onCheckedChange = { checked -> onChange(if (checked) chosen + id else chosen - id) },
                        colors = CheckboxDefaults.colors(checkedColor = accent, checkmarkColor = Color.Black),
                    )
                    Text(label, color = Ui.text, fontFamily = font, modifier = Modifier.weight(1f))
                    if (on) {
                        val i = chosen.indexOf(id)
                        SmallButton("▲", font) {
                            if (i > 0) onChange(chosen.toMutableList().apply { add(i - 1, removeAt(i)) })
                        }
                        Spacer(Modifier.width(4.dp))
                        SmallButton("▼", font) {
                            if (i < chosen.size - 1) onChange(chosen.toMutableList().apply { add(i + 1, removeAt(i)) })
                        }
                    }
                }
            }
        }
    }
}

private class QsTile(
    val label: String,
    val on: Boolean,
    val glyph: @Composable (Color) -> Unit,
    val action: () -> Unit,
)

@Composable
private fun SliderRow(
    icon: @Composable () -> Unit,
    value: Float,
    accent: Color,
    onDragging: (Boolean) -> Unit,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(12.dp))
        Slider(
            value = value,
            onValueChange = {
                onDragging(true)
                onChange(it)
            },
            onValueChangeFinished = { onDragging(false) },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        )
    }
}

@Composable
fun SmallButton(text: String, font: FontFamily, onClick: () -> Unit) {
    Text(
        text,
        color = Ui.text,
        fontSize = 13.sp,
        fontFamily = font,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Ui.text.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

// ---------------------------------------------------------------- Task view

@Composable
fun TaskView(settings: LauncherSettings, repo: AppRepository, font: FontFamily, onClose: () -> Unit) {
    val context = LocalContext.current
    val tick = LauncherUi.resumeTick.intValue
    val hasPermission = remember(tick) { RecentApps.hasPermission(context) }
    LaunchedEffect(tick) { repo.refreshRecents() }
    val byKey = remember(repo.apps) { repo.apps.associateBy { it.key } }
    val recent = repo.recentKeys.mapNotNull { byKey[it] }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ui.window.copy(alpha = 0.95f))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .padding(32.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent apps", color = Ui.text, fontSize = 22.sp, fontFamily = font, modifier = Modifier.weight(1f))
                if (recent.isNotEmpty()) {
                    SmallButton("Clear list", font) { repo.closeRecent(recent.map { it.key }) }
                    Spacer(Modifier.width(8.dp))
                }
                SmallButton("Android recents", font) {
                    onClose()
                    Actions.perform(context, GestureAction.RECENTS) {}
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                !settings.showRecentApps || !hasPermission -> Column {
                    Text("To list your recent apps here, allow Usage access for Pad Launcher.", color = Ui.text, fontFamily = font)
                    Spacer(Modifier.height(8.dp))
                    SmallButton("Allow usage access", font) {
                        settings.showRecentApps = true
                        RecentApps.openSettings(context)
                    }
                }
                recent.isEmpty() -> Text("No recent apps", color = Ui.text, fontFamily = font)
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(recent, key = { it.key }) { app ->
                        TaskCard(
                            app, repo, settings, font, repo.recentTimes[app.key] ?: 0L,
                            onClose = { repo.closeRecent(listOf(app.key)) },
                        ) {
                            AppLaunch.open(context, app)
                            onClose()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    app: AppInfo,
    repo: AppRepository,
    settings: LauncherSettings,
    font: FontFamily,
    time: Long,
    onClose: () -> Unit,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Box {
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(panelColor(settings))
            .border(1.dp, Ui.text.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIconImage(app, repo, settings, 56.dp)
        Spacer(Modifier.height(10.dp))
        Text(repo.label(app), color = Ui.text, fontFamily = font, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (time > 0) {
            Text(DateUtils.getRelativeTimeSpanString(time).toString(), color = Ui.subtle, fontSize = 12.sp, fontFamily = font)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallButton("Window", font) { WindowMode.open(context, app) }
            SmallButton("Split", font) { AppLaunch.openSplit(context, app) }
        }
    }
    Icon(
        Icons.Default.Close, contentDescription = "Close", tint = Ui.text,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(28.dp)
            .clip(CircleShape)
            .clickable(onClick = onClose)
            .padding(4.dp),
    )
    }
}
