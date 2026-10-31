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
                    SettingsScreen(app.settings, app.repo)
                }
            }
        }
    }
}

private val LABEL_COLORS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFBDBDBD, 0xFFFFC857, 0xFF7FD1FF, 0xFFFF8FAB, 0xFF9BE7A0, 0xFFC3A6FF,
).map { it.toInt() }

private enum class Dialog { ICON_PACK, ICON_SHAPE, FONT, SWIPE_UP, SWIPE_DOWN, DOUBLE_TAP }

@Composable
private fun SettingsScreen(s: LauncherSettings, repo: AppRepository) {
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

        item { Section("Home screen") }
        item { IntSlider("Home columns", s.homeColumns, 2..10) { s.homeColumns = it } }
        item { SwitchRow("Show labels on home", s.showHomeLabels) { s.showHomeLabels = it } }
        item { SwitchRow("Show clock", s.showClock) { s.showClock = it } }
        item { SwitchRow("24-hour clock", s.clock24h) { s.clock24h = it } }

        item { Section("App drawer") }
        item { IntSlider("Drawer columns", s.drawerColumns, 3..12) { s.drawerColumns = it } }
        item { SwitchRow("Show labels in drawer", s.showDrawerLabels) { s.showDrawerLabels = it } }
        item { IntSlider("Background opacity %", s.drawerOpacity, 0..100) { s.drawerOpacity = it } }

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
                Text("Label & clock color")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LABEL_COLORS.forEach { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(Color(c), CircleShape)
                                .border(
                                    width = if (c == s.labelColor) 3.dp else 1.dp,
                                    color = if (c == s.labelColor) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape,
                                )
                                .clickable { s.labelColor = c },
                        )
                    }
                }
            }
        }

        item { Section("Gestures (home screen)") }
        item { ClickRow("Swipe up", GestureAction.from(s.gestureSwipeUp).label) { dialog = Dialog.SWIPE_UP } }
        item { ClickRow("Swipe down", GestureAction.from(s.gestureSwipeDown).label) { dialog = Dialog.SWIPE_DOWN } }
        item { ClickRow("Double tap", GestureAction.from(s.gestureDoubleTap).label) { dialog = Dialog.DOUBLE_TAP } }

        item { Section("Hidden apps") }
        val hiddenApps = repo.apps.filter { it.key in s.hidden }
        if (hiddenApps.isEmpty()) {
            item { Text("No hidden apps. Long-press an app → Hide from drawer.", color = Color.Gray) }
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
