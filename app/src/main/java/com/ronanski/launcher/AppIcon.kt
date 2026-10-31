package com.ronanski.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ICON_SHAPES = listOf(
    "original" to "Original (system)",
    "circle" to "Circle",
    "squircle" to "Squircle",
    "rounded" to "Rounded square",
    "square" to "Square",
    "teardrop" to "Teardrop",
)

fun iconShape(key: String): Shape = when (key) {
    "circle" -> CircleShape
    "squircle" -> RoundedCornerShape(32)
    "rounded" -> RoundedCornerShape(20)
    "teardrop" -> RoundedCornerShape(50, 50, 10, 50)
    else -> RectangleShape
}

fun labelStyle(settings: LauncherSettings): TextStyle =
    if (settings.labelShadow) {
        TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 2f), 6f))
    } else {
        TextStyle.Default
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    repo: AppRepository,
    settings: LauncherSettings,
    showLabel: Boolean,
    font: FontFamily,
    onRename: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    onLaunched: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val sizeDp = settings.iconSize
    val sizePx = with(LocalDensity.current) { sizeDp.dp.roundToPx() }
    val shapeKey = settings.iconShape
    val pack = settings.iconPack
    val icon by produceState<ImageBitmap?>(null, app.key, pack, shapeKey, sizePx) {
        value = repo.icon(app, sizePx, pack, fullBleed = shapeKey != "original")
    }
    var menuOpen by remember { mutableStateOf(false) }
    val label = repo.label(app)

    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = {
                        AppLaunch.open(context, app)
                        onLaunched()
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                )
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val bitmap = icon
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = label,
                    modifier = Modifier.size(sizeDp.dp).clip(iconShape(shapeKey)),
                )
            } else {
                Spacer(Modifier.size(sizeDp.dp))
            }
            if (showLabel) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    color = Color(settings.labelColor),
                    fontSize = settings.labelSize.sp,
                    fontFamily = font,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = labelStyle(settings),
                )
            }
        }
        AppMenu(menuOpen, { menuOpen = false }, app, settings, onRename, onLaunched)
    }
}

@Composable
private fun AppMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    app: AppInfo,
    settings: LauncherSettings,
    onRename: (AppInfo) -> Unit,
    onLaunched: () -> Unit,
) {
    val context = LocalContext.current
    val shortcuts = remember(expanded, app.key) {
        if (expanded) AppLaunch.shortcuts(context, app).take(5) else emptyList()
    }
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val item: @Composable (String, () -> Unit) -> Unit = { text, action ->
            DropdownMenuItem(
                text = { Text(text) },
                onClick = {
                    onDismiss()
                    action()
                },
            )
        }

        shortcuts.forEach { shortcut ->
            item(shortcut.shortLabel?.toString() ?: shortcut.id) {
                AppLaunch.startShortcut(context, shortcut)
                onLaunched()
            }
        }
        if (shortcuts.isNotEmpty()) HorizontalDivider()

        val pinned = app.key in settings.pinned
        item(if (pinned) "Remove from home" else "Add to home") { settings.togglePin(app.key) }
        if (pinned) {
            item("Move left") { settings.movePinned(app.key, -1) }
            item("Move right") { settings.movePinned(app.key, 1) }
        }
        item("Open in split screen") {
            AppLaunch.openSplit(context, app)
            onLaunched()
        }
        item("Rename") { onRename(app) }
        item(if (app.key in settings.hidden) "Unhide" else "Hide from drawer") {
            settings.toggleHidden(app.key)
        }
        item("App info") { AppLaunch.appInfo(context, app) }
        item("Uninstall") { AppLaunch.uninstall(context, app) }
    }
}
