package com.ronanski.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class SettingsPage(val title: String, val subtitle: String) {
    PERSONALIZATION("Personalization", "Background, colors, fonts"),
    DESKTOP("Desktop & icons", "Grid, icon pack, shapes"),
    TASKBAR("Taskbar", "Alignment, running apps, clock"),
    NOTIFICATIONS("Notifications", "Hide ongoing or per-app notifications"),
    START("Start", "Pinned apps layout"),
    MULTITASKING("Windows & multitasking", "Open apps in resizable windows"),
    NAVIGATION("Navigation", "Gestures, nav buttons, hide Xiaomi's nav bar"),
    GESTURES("Gestures", "Swipe and double-tap actions"),
    APPS("Apps", "Hidden apps"),
    PERMISSIONS("Permissions & setup", "Default home, access settings"),
    BACKUP("Backup & restore", "Save or load your layout and settings"),
    UPDATES("Updates & about", "Version, release notes"),
}

/** Windows-style Settings window shown inside the launcher. */
@Composable
fun SettingsWindow(
    settings: LauncherSettings,
    repo: AppRepository,
    updater: Updater,
    page: MutableState<SettingsPage>,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(12.dp)
                .widthIn(max = 1100.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(Ui.window)
                .border(1.dp, Ui.text.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            // Title bar
            Row(
                Modifier.fillMaxWidth().height(44.dp).padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StartGlyph(Color(settings.accentColor), 14.dp)
                Spacer(Modifier.width(10.dp))
                Text("Settings", color = Ui.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(width = 52.dp, height = 44.dp)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Ui.text, modifier = Modifier.size(18.dp)) }
            }
            SettingsContent(settings, repo, updater, page, Modifier.fillMaxSize())
        }
    }
}

@Composable
fun SettingsContent(
    settings: LauncherSettings,
    repo: AppRepository,
    updater: Updater,
    page: MutableState<SettingsPage>,
    modifier: Modifier = Modifier,
) {
    val accent = Color(settings.accentColor)
    BoxWithConstraints(modifier) {
        if (maxWidth >= 700.dp) {
            Row(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.width(260.dp).fillMaxHeight(), contentPadding = PaddingValues(8.dp)) {
                    items(SettingsPage.entries) { p ->
                        NavItem(p, p == page.value, accent) { page.value = p }
                    }
                }
                SettingsPageContent(settings, repo, updater, page.value, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            var showingPage by remember { mutableStateOf(false) }
            if (!showingPage) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                    items(SettingsPage.entries) { p ->
                        NavItem(p, false, accent) {
                            page.value = p
                            showingPage = true
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        "‹ All settings",
                        color = Ui.text,
                        modifier = Modifier.clickable { showingPage = false }.padding(16.dp),
                    )
                    SettingsPageContent(settings, repo, updater, page.value, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun NavItem(p: SettingsPage, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Ui.text.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(3.dp, 18.dp).background(if (selected) accent else Color.Transparent, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Text(p.title, color = Ui.text, fontSize = 14.sp)
    }
}

private enum class Dialog { ICON_PACK, ICON_SHAPE, FONT, SWIPE_UP, SWIPE_DOWN, DOUBLE_TAP }

private val LABEL_COLORS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFBDBDBD, 0xFFFFC857, 0xFF7FD1FF, 0xFFFF8FAB, 0xFF9BE7A0, 0xFFC3A6FF,
).map { it.toInt() }

private val ACCENT_COLORS = listOf(
    0xFF4CC2FF, 0xFF0078D4, 0xFFB794F6, 0xFF6CCB5F, 0xFFFF9349, 0xFFFF7AB6, 0xFFFF6B6B, 0xFFFFC857,
).map { it.toInt() }

@Composable
private fun SettingsPageContent(
    s: LauncherSettings,
    repo: AppRepository,
    updater: Updater,
    page: SettingsPage,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    val tick = LauncherUi.resumeTick.intValue

    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (FontStore.import(context, uri)) {
                s.font = "custom"
                s.fontVersion += 1
            } else {
                Actions.toast(context, "That file isn't a valid font")
            }
        }
    }

    fun open(action: String) = SystemControls.openSettings(context, action)

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(s.exportJson().toByteArray()) } != null
            } catch (e: Exception) {
                false
            }
            Actions.toast(context, if (ok) "Backup saved" else "Couldn't save the backup")
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            } catch (e: Exception) {
                null
            }
            val ok = text != null && s.importJson(text)
            if (ok) repo.refreshRecents()
            Actions.toast(context, if (ok) "Backup restored" else "That file isn't a Pad Launcher backup")
        }
    }

    LazyColumn(modifier, contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 48.dp)) {
        item {
            Text(
                page.title,
                color = Ui.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
        }
        when (page) {
            SettingsPage.PERSONALIZATION -> {
                card {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Theme", color = Ui.text)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("dark" to "Dark", "light" to "Light", "system" to "Same as system").forEach { (key, label) ->
                                ThemeChip(label, s.themeMode == key) { s.themeMode = key }
                            }
                        }
                    }
                }
                card { WallpaperGallery(s) }
                card { ClickRow("Background", "Choose your own picture as wallpaper") {
                    try {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
                    } catch (e: Exception) {
                    }
                } }
                card {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Accent color", color = Ui.text)
                        Spacer(Modifier.height(8.dp))
                        ColorRow(ACCENT_COLORS, s.accentColor) { s.accentColor = it }
                    }
                }
                card { IntSlider("Taskbar, Start & panel opacity %", s.panelOpacity, 30..100) { s.panelOpacity = it } }
                card {
                    ClickRow("Font", FONT_OPTIONS.firstOrNull { it.first == s.font }?.second ?: "") { dialog = Dialog.FONT }
                }
                card {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text("Desktop label color", color = Ui.text)
                        Spacer(Modifier.height(8.dp))
                        ColorRow(LABEL_COLORS, s.labelColor) { s.labelColor = it }
                    }
                }
                card { IntSlider("Label size (sp)", s.labelSize, 8..20) { s.labelSize = it } }
                card { SwitchRow("Label shadow", s.labelShadow) { s.labelShadow = it } }
            }

            SettingsPage.DESKTOP -> {
                card {
                    val cols = s.desktopColumns
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text("Desktop grid columns: ${if (cols == 0) "Auto" else cols.toString()}", color = Ui.text)
                        Slider(
                            value = cols.toFloat(),
                            onValueChange = { s.desktopColumns = it.roundToInt() },
                            valueRange = 0f..16f,
                            steps = 15,
                        )
                    }
                }
                card { SwitchRow("Show icon labels", s.showDesktopLabels) { s.showDesktopLabels = it } }
                card { ClickRow("Sort desktop icons by name", "Widgets stay in place") { DesktopGrid.sortApps(s, repo) } }
                card {
                    val packs = remember { repo.iconPacks.installedPacks() }
                    val current = packs.firstOrNull { it.packageName == s.iconPack }?.label ?: "None (system icons)"
                    ClickRow("Icon pack", current) { dialog = Dialog.ICON_PACK }
                }
                card {
                    ClickRow("Icon shape", ICON_SHAPES.firstOrNull { it.first == s.iconShape }?.second ?: "") {
                        dialog = Dialog.ICON_SHAPE
                    }
                }
                card { IntSlider("Icon size (dp)", s.iconSize, 32..96) { s.iconSize = it } }
            }

            SettingsPage.TASKBAR -> {
                card {
                    SwitchRow("Show the taskbar over all apps (Start and panels open above any app or floating window)", s.overlayTaskbar) {
                        s.overlayTaskbar = it
                        if (it && LauncherAccessibilityService.instance == null) {
                            Actions.toast(context, "Turn on the Pad Launcher accessibility service")
                            Actions.openAccessibilitySettings(context)
                        }
                        LauncherAccessibilityService.instance?.refreshOverlay()
                    }
                }
                card {
                    Text(
                        "Apps don't resize around it, so the taskbar covers the bottom strip of the app you're using.",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                card { SwitchRow("Center taskbar icons (off = left, like Windows 10)", s.taskbarCentered) { s.taskbarCentered = it } }
                card {
                    SwitchRow("Show open & recent apps on the taskbar", s.showRecentApps) {
                        s.showRecentApps = it
                        if (it && !RecentApps.hasPermission(context)) {
                            Actions.toast(context, "Allow \"Usage access\" for Pad Launcher")
                            RecentApps.openSettings(context)
                        }
                        repo.refreshRecents()
                    }
                }
                card {
                    IntSlider("Keep apps on the taskbar after last use (hours)", s.recentHours, 1..24) {
                        s.recentHours = it
                        repo.refreshRecents()
                    }
                }
                card { SwitchRow("24-hour clock", s.clock24h) { s.clock24h = it } }
                card { SwitchRow("Hide Android status bar on the desktop (swipe down from the top to show it)", s.hideStatusBar) { s.hideStatusBar = it } }
                card { SwitchRow("Hide Android navigation bar on the desktop", s.hideNavBar) { s.hideNavBar = it } }
                card {
                    Text(
                        "Tip: tap the clock for notifications & calendar, and the Wi-Fi/volume/battery icons for quick settings.",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            SettingsPage.NOTIFICATIONS -> {
                card {
                    SwitchRow("Hide ongoing notifications (\"… is running\", media, downloads)", s.hideOngoingNotifications) {
                        s.hideOngoingNotifications = it
                    }
                }
                card {
                    Text(
                        "Tip: long-press a notification in the clock flyout → \"Hide notifications from this app\".",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                val hiddenPkgs = s.hiddenNotificationApps.sorted()
                if (hiddenPkgs.isEmpty()) {
                    card { Text("No apps hidden from the notification center.", color = Ui.subtle, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                items(hiddenPkgs, key = { "nh-$it" }) { pkg ->
                    CardBox {
                        val label = remember(pkg) {
                            try {
                                context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
                            } catch (e: Exception) {
                                pkg
                            }
                        }
                        ClickRow(label, "Hidden — tap to show its notifications again") {
                            s.hiddenNotificationApps = s.hiddenNotificationApps - pkg
                        }
                    }
                }
            }

            SettingsPage.BACKUP -> {
                card {
                    ClickRow("Back up", "Save your desktop, taskbar, Start and all settings to a file") {
                        backupLauncher.launch("pad-launcher-backup.json")
                    }
                }
                card {
                    ClickRow("Restore", "Load a backup file (widgets need to be added again)") {
                        restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                }
            }

            SettingsPage.START -> {
                card { IntSlider("Pinned apps per row", s.startColumns, 3..10) { s.startColumns = it } }
                card {
                    Text(
                        "Pin apps from Start → All apps: long-press an app → Pin to Start.",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            SettingsPage.MULTITASKING -> {
                card { SwitchRow("Open apps in windows", s.openInWindows) { s.openInWindows = it } }
                card {
                    val supported = remember(tick) { WindowMode.supported(context) }
                    StatusRow(
                        "Resizable windows (freeform)",
                        if (supported) "Enabled on this device" else "Not enabled — see steps below",
                        supported,
                    ) { WindowMode.openDeveloperOptions(context) }
                }
                card {
                    Text(
                        "Windows get their title bar with minimize, maximize and close from Android itself. " +
                            "To turn it on:\n" +
                            "1. Settings → About tablet → tap the OS version 7 times to enable Developer options.\n" +
                            "2. Developer options → turn on \"Enable freeform windows\" and \"Force activities to be resizable\".\n" +
                            "3. Restart the tablet.\n\n" +
                            "On Xiaomi HyperOS, Xiaomi's own floating windows can't be opened by other launchers, " +
                            "so some apps may still open full screen. Long-press any app → \"Open in window\" to try it.",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                card { ClickRow("Open Developer options", "") { WindowMode.openDeveloperOptions(context) } }
            }

            SettingsPage.NAVIGATION -> {
                card {
                    SwitchRow("Navigation gestures in all apps", s.navGestures) {
                        s.navGestures = it
                        if (it && LauncherAccessibilityService.instance == null) {
                            Actions.toast(context, "Turn on the Pad Launcher accessibility service")
                            Actions.openAccessibilitySettings(context)
                        }
                        LauncherAccessibilityService.instance?.refreshOverlay()
                    }
                }
                card {
                    Text(
                        "• Swipe up from the bottom → Home\n" +
                            "• Swipe up from the bottom and hold → Recents\n" +
                            "• Swipe in from the left or right edge → Back",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                card {
                    IntSlider("Gesture area size (dp)", s.navGestureSize, 6..40) {
                        s.navGestureSize = it
                        LauncherAccessibilityService.instance?.refreshOverlay()
                    }
                }
                card { SwitchRow("Back / Home / Recents buttons on the taskbar", s.taskbarNavButtons) { s.taskbarNavButtons = it } }
                card {
                    val canChange = remember(tick) { XiaomiNavBar.canChange(context) }
                    var hidden by remember(tick) { mutableStateOf(XiaomiNavBar.isHidden(context)) }
                    Column(Modifier.padding(vertical = 8.dp)) {
                        if (canChange) {
                            SwitchRow("Hide Xiaomi navigation bar (everywhere)", hidden) {
                                if (XiaomiNavBar.setHidden(context, it)) hidden = it
                                else Actions.toast(context, "The system refused the change")
                            }
                            Text(
                                "Turn on \"Navigation gestures in all apps\" above so you can still go Back / Home / Recents.",
                                color = Ui.subtle,
                            )
                        } else {
                            Text("Hide Xiaomi navigation bar (everywhere)", color = Ui.text)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Xiaomi only lets the system launcher hide the 3-button bar. Pad Launcher can do it after a one-time " +
                                    "permission from a computer:\n" +
                                    "1. Developer options → turn on USB debugging (and \"USB debugging (Security settings)\" on Xiaomi).\n" +
                                    "2. Connect the tablet to a PC with Android platform-tools (adb) installed.\n" +
                                    "3. Run this command on the PC, then come back here:",
                                color = Ui.subtle,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                XiaomiNavBar.ADB_COMMAND,
                                color = Ui.text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(10.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            TextButton(onClick = { XiaomiNavBar.copyCommand(context) }) { Text("Copy command") }
                            Text(
                                "Experimental: some HyperOS versions ignore or undo this setting.",
                                color = Ui.subtle,
                            )
                        }
                    }
                }
            }

            SettingsPage.GESTURES -> {
                card { ClickRow("Swipe up on desktop", GestureAction.from(s.gestureSwipeUp).label) { dialog = Dialog.SWIPE_UP } }
                card { ClickRow("Swipe down on desktop", GestureAction.from(s.gestureSwipeDown).label) { dialog = Dialog.SWIPE_DOWN } }
                card { ClickRow("Double tap on desktop", GestureAction.from(s.gestureDoubleTap).label) { dialog = Dialog.DOUBLE_TAP } }
            }

            SettingsPage.APPS -> {
                val hiddenApps = repo.apps.filter { it.key in s.hidden }
                if (hiddenApps.isEmpty()) {
                    card { Text("No hidden apps. In Start → All apps, long-press an app → Hide.", color = Ui.subtle, modifier = Modifier.padding(vertical = 8.dp)) }
                }
                items(hiddenApps, key = { it.key }) { app ->
                    CardBox { ClickRow(repo.label(app), "Hidden — tap to show again") { s.toggleHidden(app.key) } }
                }
            }

            SettingsPage.PERMISSIONS -> {
                card {
                    val isHome = remember(tick) {
                        context.packageManager.resolveActivity(
                            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY,
                        )?.activityInfo?.packageName == context.packageName
                    }
                    StatusRow(
                        "Default home app",
                        if (isHome) "Pad Launcher is your home app" else "Tap to choose Pad Launcher",
                        isHome,
                    ) { open(Settings.ACTION_HOME_SETTINGS) }
                }
                card {
                    val on = LauncherAccessibilityService.connected.value
                    StatusRow("Accessibility service", "Lock screen, recents, split screen, power menu", on) { Actions.openAccessibilitySettings(context) }
                }
                card {
                    val on = remember(tick) { RecentApps.hasPermission(context) }
                    StatusRow("Usage access", "Open & recent apps on the taskbar and Task View", on) { RecentApps.openSettings(context) }
                }
                card {
                    val on = remember(tick) { NotificationStore.hasAccess(context) }
                    StatusRow("Notification access", "Notifications in the clock flyout", on) { NotificationStore.openSettings(context) }
                }
                card {
                    val on = remember(tick) { SystemControls.canWrite(context) }
                    StatusRow("Modify system settings", "Brightness slider and auto-rotate tile", on) { SystemControls.requestWrite(context) }
                }
                card {
                    val on = remember(tick) { SystemControls.hasDndAccess(context) }
                    StatusRow("Do Not Disturb access", "Do not disturb tile", on) { open(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) }
                }
                card {
                    Text(
                        "Accessibility or Notification access greyed out? That's Android's \"Restricted settings\" for apps " +
                            "not installed from the Play Store. Tap the greyed-out Pad Launcher entry once, then go to " +
                            "Settings → Apps → Pad Launcher → ⋮ (top right) → Allow restricted settings, and try again.",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                card {
                    Text(
                        "On Xiaomi: Settings → Apps → Pad Launcher → turn on Autostart and set Battery saver to \"No restrictions\", " +
                            "otherwise HyperOS may stop the accessibility and notification services.",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            SettingsPage.UPDATES -> {
                card {
                    val info = updater.available
                    val progress = updater.progress
                    when {
                        progress != null -> ClickRow("Downloading v${info?.version}…", "${(progress * 100).roundToInt()}%") {}
                        info != null -> ClickRow("Install update v${info.version}", "You have v${updater.currentVersion}") {
                            updater.downloadAndInstall()
                        }
                        else -> ClickRow(
                            "Check for updates",
                            updater.status.ifEmpty { "Current version: v${updater.currentVersion}" },
                        ) { updater.check(userInitiated = true) }
                    }
                }
                card {
                    val notes = updater.available?.notes.orEmpty()
                    ClickRow("Release notes", notes.ifEmpty { "Open the GitHub releases page" }) { updater.openReleasePage() }
                }
                card { SwitchRow("Check for updates automatically", s.autoUpdateCheck) { s.autoUpdateCheck = it } }
                card {
                    Text(
                        "Pad Launcher v${updater.currentVersion}\n\nOn Xiaomi devices, the system recents screen and gesture " +
                            "navigation belong to HyperOS and can't be replaced by any launcher.",
                        color = Ui.subtle,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }

    when (dialog) {
        Dialog.ICON_PACK -> {
            val packs = remember { repo.iconPacks.installedPacks() }
            ChoiceDialog(
                "Icon pack",
                listOf("" to "None (system icons)") + packs.map { it.packageName to it.label },
                s.iconPack, { s.iconPack = it }, { dialog = null },
            )
        }
        Dialog.ICON_SHAPE -> ChoiceDialog("Icon shape", ICON_SHAPES, s.iconShape, { s.iconShape = it }, { dialog = null })
        Dialog.FONT -> ChoiceDialog(
            "Font", FONT_OPTIONS, s.font,
            { if (it == "custom") fontPicker.launch(arrayOf("*/*")) else s.font = it },
            { dialog = null },
            preview = { key -> FontFamily(androidx.compose.ui.text.font.Typeface(FontStore.typeface(context, key))) },
        )
        Dialog.SWIPE_UP -> GestureDialog("Swipe up", s.gestureSwipeUp, { s.gestureSwipeUp = it }) { dialog = null }
        Dialog.SWIPE_DOWN -> GestureDialog("Swipe down", s.gestureSwipeDown, { s.gestureSwipeDown = it }) { dialog = null }
        Dialog.DOUBLE_TAP -> GestureDialog("Double tap", s.gestureDoubleTap, { s.gestureDoubleTap = it }) { dialog = null }
        null -> Unit
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = Ui.text,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Ui.text.copy(alpha = 0.16f) else Ui.text.copy(alpha = 0.05f))
            .border(1.dp, if (selected) Ui.text.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Built-in wallpapers, previewed in the current accent color and theme. */
@Composable
private fun WallpaperGallery(s: LauncherSettings) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val accent = s.accentColor
    val light = LocalLauncherColors.current.isLight
    var applying by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(vertical = 8.dp)) {
        Text("Built-in wallpapers", color = Ui.text)
        Text("Original designs in your accent color. Tap one to set it.", color = Ui.subtle, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Wallpapers.STYLES.forEach { (key, label) ->
                val preview by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, key, accent, light) {
                    value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                        Wallpapers.render(key, accent, light, 320, 200).asImageBitmap()
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(160.dp, 100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                if (s.wallpaperStyle == key) 3.dp else 1.dp,
                                if (s.wallpaperStyle == key) Color(accent) else Ui.subtle,
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(enabled = applying == null) {
                                applying = key
                                scope.launch {
                                    val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                        Wallpapers.applyWallpaper(context, key, accent, light)
                                    }
                                    if (ok) s.wallpaperStyle = key
                                    Actions.toast(context, if (ok) "Wallpaper set" else "Couldn't set the wallpaper")
                                    applying = null
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        preview?.let {
                            androidx.compose.foundation.Image(it, contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        }
                        if (applying == key) Text("Setting…", color = Color.White)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, color = Ui.text, fontSize = 12.sp)
                }
            }
        }
    }
}

/** One settings row in a rounded "card", like Windows 11 Settings. */
private fun LazyListScope.card(content: @Composable () -> Unit) {
    item { CardBox(content) }
}

@Composable
private fun CardBox(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Ui.text.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) { content() }
}

@Composable
private fun StatusRow(title: String, subtitle: String, ok: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ui.text)
            Text(subtitle, color = Ui.subtle, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            if (ok) "On" else "Off",
            color = if (ok) Color(0xFF6CCB5F) else Color(0xFFFF9349),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ColorRow(colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color(c), CircleShape)
                    .border(
                        width = if (c == selected) 3.dp else 1.dp,
                        color = if (c == selected) Ui.text else Ui.subtle,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(c) },
            )
        }
    }
}

@Composable
private fun GestureDialog(title: String, current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) =
    ChoiceDialog(title, GestureAction.entries.map { it.name to it.label }, current, onSelect, onDismiss)

@Composable
private fun ClickRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(title, color = Ui.text)
        if (subtitle.isNotEmpty()) Text(subtitle, color = Ui.subtle, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Ui.text, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun IntSlider(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text("$title: $value", color = Ui.text)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    preview: ((String) -> FontFamily)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options) { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label, fontFamily = preview?.invoke(value))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
