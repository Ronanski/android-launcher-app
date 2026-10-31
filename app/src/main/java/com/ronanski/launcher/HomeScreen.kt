package com.ronanski.launcher

import android.content.Intent
import android.provider.AlarmClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(settings: LauncherSettings, repo: AppRepository, drawerOpen: MutableState<Boolean>) {
    val context = LocalContext.current
    val font = rememberLauncherFont(settings)
    val apps = repo.apps
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val pinned = settings.pinned.mapNotNull { byKey[it] }
    var homeMenu by remember { mutableStateOf(false) }
    var menuAt by remember { mutableStateOf(Offset.Zero) }
    var renameTarget by remember { mutableStateOf<AppInfo?>(null) }
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }

    fun run(actionName: String) =
        Actions.perform(context, GestureAction.from(actionName)) { drawerOpen.value = true }

    Box(Modifier.fillMaxSize()) {
        // ---- Home layer (wallpaper shows through) ----
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { run(settings.gestureDoubleTap) },
                        onLongPress = {
                            menuAt = it
                            homeMenu = true
                        },
                    )
                }
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            when {
                                total < -swipeThreshold -> run(settings.gestureSwipeUp)
                                total > swipeThreshold -> run(settings.gestureSwipeDown)
                            }
                        },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            total += dy
                        },
                    )
                },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
            ) {
                if (settings.showClock) Clock(settings, font)
                UpdateBanner(context.launcherApp.updater, font)
                Spacer(Modifier.weight(1f))
                if (pinned.isEmpty()) {
                    Text(
                        "Swipe up to see all apps.\nLong-press an app → Add to home.\nLong-press empty space → Settings.",
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontFamily = font,
                        style = labelStyle(settings),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    val columns = settings.homeColumns.coerceAtLeast(1)
                    pinned.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { app ->
                                key(app.key) {
                                    AppIcon(
                                        app, repo, settings, settings.showHomeLabels, font,
                                        onRename = { renameTarget = it },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Box(Modifier.offset { IntOffset(menuAt.x.roundToInt(), menuAt.y.roundToInt()) }) {
                DropdownMenu(expanded = homeMenu, onDismissRequest = { homeMenu = false }) {
                    DropdownMenuItem(text = { Text("Launcher settings") }, onClick = {
                        homeMenu = false
                        Actions.perform(context, GestureAction.LAUNCHER_SETTINGS) {}
                    })
                    DropdownMenuItem(text = { Text("Change wallpaper") }, onClick = {
                        homeMenu = false
                        try {
                            context.startActivity(
                                Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper"),
                            )
                        } catch (e: Exception) {
                        }
                    })
                    DropdownMenuItem(text = { Text("All apps") }, onClick = {
                        homeMenu = false
                        drawerOpen.value = true
                    })
                }
            }
        }

        // ---- App drawer layer ----
        AnimatedVisibility(
            visible = drawerOpen.value,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) {
            AppDrawer(settings, repo, font, onClose = { drawerOpen.value = false }, onRename = { renameTarget = it })
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
private fun UpdateBanner(updater: Updater, font: FontFamily) {
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
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .clickable { updater.downloadAndInstall() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun Clock(settings: LauncherSettings, font: FontFamily) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000 + 50)
        }
    }
    val is24h = settings.clock24h
    val timeFormat = remember(is24h) { SimpleDateFormat(if (is24h) "HH:mm" else "h:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val color = Color(settings.labelColor)
    Column(
        Modifier
            .padding(top = 32.dp)
            .clickable {
                try {
                    context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
                } catch (e: Exception) {
                }
            },
    ) {
        Text(
            timeFormat.format(Date(now)),
            color = color, fontSize = 72.sp, fontFamily = font, fontWeight = FontWeight.Light,
            style = labelStyle(settings),
        )
        Text(
            dateFormat.format(Date(now)),
            color = color, fontSize = 18.sp, fontFamily = font, style = labelStyle(settings),
        )
    }
}
