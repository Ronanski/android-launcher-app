package com.ronanski.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = launcherApp
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    SettingsScreen(app.settings, app.repo, app.updater)
                }
            }
        }
    }
}

private val LABEL_COLORS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFBDBDBD, 0xFFFFC857, 0xFF7FD1FF, 0xFFFF8FAB, 0xFF9BE7A0, 0xFFC3A6FF,
).map { it.toInt() }

private val ACCENT_COLORS = listOf(
    0xFF4CC2FF, 0xFF0078D4, 0xFFB794F6, 0xFF6CCB5F, 0xFFFF9349, 0xFFFF7AB6, 0xFFFF6B6B, 0xFFFFC857,
).map { it.toInt() }

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
                        color = if (c == selected) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(c) },
            )
        }
    }
}

private enum class Dialog { ICON_PACK, ICON_SHAPE, FONT, SWIPE_UP, SWIPE_DOWN, DOUBLE_TAP }

@Composable
private fun SettingsScreen(s: LauncherSettings, repo: AppRepository, updater: Updater) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<Dialog?>(null) }
    val a11yOn = LauncherAccessibilityService.connected.value

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

    fun open(action: String) {
        try {
            context.startActivity(Intent(action))
        } catch (e: Exception) {
            Actions.toast(context, "Can't open that settings page")
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item { Text("Pad Launcher", style = MaterialTheme.typography.headlineMedium) }

        item { Section("Setup") }
        item {
            ClickRow("Set as default home app", "Choose Pad Launcher as your Home app") {
                open(Settings.ACTION_HOME_SETTINGS)
            }
        }
        item {
            ClickRow(
                "Accessibility service: ${if (a11yOn) "ON" else "OFF"}",
                "Needed for lock-screen double tap, recents and split-screen gestures",
            ) { Actions.openAccessibilitySettings(context) }
        }

        item { Section("Updates") }
        item {
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
        item {
            val notes = updater.available?.notes.orEmpty()
            ClickRow("Release notes", notes.ifEmpty { "Open the GitHub releases page" }) { updater.openReleasePage() }
        }
        item { SwitchRow("Check for updates automatically", s.autoUpdateCheck) { s.autoUpdateCheck = it } }

        item { Section("Desktop") }
        item {
            val cols = s.desktopColumns
            Column(Modifier.padding(vertical = 4.dp)) {
                Text("Desktop grid columns: ${if (cols == 0) "Auto" else cols.toString()}")
                Slider(
                    value = cols.toFloat(),
                    onValueChange = { s.desktopColumns = it.roundToInt() },
                    valueRange = 0f..16f,
                    steps = 15,
                )
            }
        }
        item { SwitchRow("Show icon labels", s.showDesktopLabels) { s.showDesktopLabels = it } }
        item { ClickRow("Sort desktop icons by name", "Widgets stay in place") { DesktopGrid.sortApps(s, repo) } }

        item { Section("Taskbar") }
        item { SwitchRow("Center taskbar icons (off = left, like Windows 10)", s.taskbarCentered) { s.taskbarCentered = it } }
        item {
            SwitchRow("Show recently used apps", s.showRecentApps) {
                s.showRecentApps = it
                if (it && !RecentApps.hasPermission(context)) {
                    Actions.toast(context, "Allow \"Usage access\" for Pad Launcher")
                    RecentApps.openSettings(context)
                }
                repo.refreshRecents()
            }
        }
        item {
            ClickRow(
                "Usage access: ${if (RecentApps.hasPermission(context)) "allowed" else "not allowed"}",
                "Needed to show recent apps on the taskbar",
            ) { RecentApps.openSettings(context) }
        }
        item { SwitchRow("24-hour clock", s.clock24h) { s.clock24h = it } }

        item { Section("Start menu & panels") }
        item { IntSlider("Pinned apps per row", s.startColumns, 3..10) { s.startColumns = it } }
        item { IntSlider("Panel opacity %", s.panelOpacity, 30..100) { s.panelOpacity = it } }
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("Accent color")
                Spacer(Modifier.height(8.dp))
                ColorRow(ACCENT_COLORS, s.accentColor) { s.accentColor = it }
            }
        }

        item { Section("Icons") }
        item {
            val packs = remember { repo.iconPacks.installedPacks() }
            val current = packs.firstOrNull { it.packageName == s.iconPack }?.label ?: "None (system icons)"
            ClickRow("Icon pack", current) { dialog = Dialog.ICON_PACK }
        }
        item {
            ClickRow("Icon shape", ICON_SHAPES.firstOrNull { it.first == s.iconShape }?.second ?: "") {
                dialog = Dialog.ICON_SHAPE
            }
        }
        item { IntSlider("Icon size (dp)", s.iconSize, 32..96) { s.iconSize = it } }

        item { Section("Text") }
        item {
            ClickRow("Font", FONT_OPTIONS.firstOrNull { it.first == s.font }?.second ?: "") {
                dialog = Dialog.FONT
            }
        }
        item { IntSlider("Label size (sp)", s.labelSize, 8..20) { s.labelSize = it } }
        item { SwitchRow("Label shadow", s.labelShadow) { s.labelShadow = it } }
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("Desktop label color")
                Spacer(Modifier.height(8.dp))
                ColorRow(LABEL_COLORS, s.labelColor) { s.labelColor = it }
            }
        }

        item { Section("Gestures (empty desktop)") }
        item { ClickRow("Swipe up", GestureAction.from(s.gestureSwipeUp).label) { dialog = Dialog.SWIPE_UP } }
        item { ClickRow("Swipe down", GestureAction.from(s.gestureSwipeDown).label) { dialog = Dialog.SWIPE_DOWN } }
        item { ClickRow("Double tap", GestureAction.from(s.gestureDoubleTap).label) { dialog = Dialog.DOUBLE_TAP } }

        item { Section("Hidden apps") }
        val hiddenApps = repo.apps.filter { it.key in s.hidden }
        if (hiddenApps.isEmpty()) {
            item { Text("No hidden apps. In Start → All apps, long-press an app → Hide.", color = Color.Gray) }
        }
        items(hiddenApps, key = { it.key }) { app ->
            ClickRow(repo.label(app), "Tap to unhide") { s.toggleHidden(app.key) }
        }

        item { Section("About Xiaomi / HyperOS") }
        item {
            Text(
                "On Xiaomi devices, the recents screen, full-screen gestures and the split-screen button " +
                    "in recents belong to the system launcher (MIUI/HyperOS Home). No third-party launcher can " +
                    "change that. Tip: in Settings → Apps → Pad Launcher, turn on Autostart and set Battery " +
                    "saver to \"No restrictions\" so the accessibility service is not killed.",
                color = Color.Gray,
            )
        }
        item { Spacer(Modifier.height(48.dp)) }
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
            {
                if (it == "custom") {
                    fontPicker.launch(arrayOf("*/*"))
                } else {
                    s.font = it
                }
            },
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
private fun GestureDialog(title: String, current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) =
    ChoiceDialog(title, GestureAction.entries.map { it.name to it.label }, current, onSelect, onDismiss)

@Composable
private fun Section(title: String) {
    Text(
        title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun ClickRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(title)
        if (subtitle.isNotEmpty()) Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
        Text(title, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun IntSlider(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text("$title: $value")
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
