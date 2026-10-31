package com.ronanski.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    settings: LauncherSettings,
    repo: AppRepository,
    widgets: WidgetManager,
    startOpen: MutableState<Boolean>,
    searchFocus: MutableState<Boolean>,
    widgetPicker: MutableState<Boolean>,
    onAddWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    val font = rememberLauncherFont(settings)
    var desktopMenu by remember { mutableStateOf(false) }
    var menuAt by remember { mutableStateOf(Offset.Zero) }
    var renameTarget by remember { mutableStateOf<AppInfo?>(null) }

    fun openStart(search: Boolean) {
        searchFocus.value = search
        startOpen.value = true
    }

    fun run(actionName: String) = Actions.perform(context, GestureAction.from(actionName)) { openStart(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
                Desktop(
                    settings = settings,
                    repo = repo,
                    widgets = widgets,
                    font = font,
                    onRename = { renameTarget = it },
                    onBackgroundLongPress = {
                        menuAt = it
                        desktopMenu = true
                    },
                    onGesture = { run(it) },
                    modifier = Modifier.fillMaxSize(),
                )

                UpdateBanner(context.launcherApp.updater, font, Modifier.align(Alignment.TopEnd))

                Box(Modifier.offset { IntOffset(menuAt.x.roundToInt(), menuAt.y.roundToInt()) }) {
                    DropdownMenu(expanded = desktopMenu, onDismissRequest = { desktopMenu = false }) {
                        val item: @Composable (String, () -> Unit) -> Unit = { text, action ->
                            DropdownMenuItem(text = { Text(text) }, onClick = {
                                desktopMenu = false
                                action()
                            })
                        }
                        item("Add widget") { widgetPicker.value = true }
                        item("Sort icons by name") { DesktopGrid.sortApps(settings, repo) }
                        item("Change wallpaper") {
                            try {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"))
                            } catch (e: Exception) {
                            }
                        }
                        item("Personalize") { Actions.perform(context, GestureAction.LAUNCHER_SETTINGS) {} }
                    }
                }

                AnimatedVisibility(
                    visible = startOpen.value,
                    enter = slideInVertically { it / 6 } + fadeIn(),
                    exit = slideOutVertically { it / 6 } + fadeOut(),
                ) {
                    StartMenu(
                        settings = settings,
                        repo = repo,
                        font = font,
                        focusSearch = searchFocus.value,
                        onClose = { startOpen.value = false },
                        onRename = { renameTarget = it },
                    )
                }
            }
            Taskbar(
                settings = settings,
                repo = repo,
                font = font,
                startOpen = startOpen.value,
                onStartClick = {
                    if (startOpen.value) startOpen.value = false else openStart(false)
                },
                onSearchClick = { openStart(true) },
                onRename = { renameTarget = it },
                onAppLaunched = { startOpen.value = false },
            )
        }

        if (widgetPicker.value) {
            WidgetPicker(
                widgets = widgets,
                onPick = {
                    widgetPicker.value = false
                    onAddWidget(it)
                },
                onClose = { widgetPicker.value = false },
            )
        }
    }

    renameTarget?.let { target ->
        var text by remember(target.key) { mutableStateOf(repo.label(target)) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    settings.rename(target.key, text)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    settings.rename(target.key, null)
                    renameTarget = null
                }) { Text("Reset") }
            },
        )
    }
}

@Composable
private fun UpdateBanner(updater: Updater, font: FontFamily, modifier: Modifier = Modifier) {
    val info = updater.available ?: return
    val progress = updater.progress
    Text(
        text = if (progress != null) {
            "Downloading v${info.version}… ${(progress * 100).roundToInt()}%"
        } else {
            "Update available: v${info.version} — tap to install"
        },
        color = Color.White,
        fontFamily = font,
        modifier = modifier
            .padding(12.dp)
            .widthIn(max = 360.dp)
            .background(Color(0xE61F1F1F), RoundedCornerShape(8.dp))
            .clickable { updater.downloadAndInstall() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
