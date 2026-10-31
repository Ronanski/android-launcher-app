package com.ronanski.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

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
        TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.8f), Offset(0f, 2f), 6f))
    } else {
        TextStyle.Default
    }

/** The app's icon (icon pack + shape applied), loaded in the background. */
@Composable
fun AppIconImage(
    app: AppInfo,
    repo: AppRepository,
    settings: LauncherSettings,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val shapeKey = settings.iconShape
    val pack = settings.iconPack
    val icon by produceState<ImageBitmap?>(null, app.key, pack, shapeKey, sizePx) {
        value = repo.icon(app, sizePx, pack, fullBleed = shapeKey != "original")
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size).clip(iconShape(shapeKey)))
    } else {
        Spacer(modifier.size(size))
    }
}

class MenuEntry(val label: String, val action: () -> Unit)

/** Standard Pin to Start / Pin to taskbar / desktop / rename entries. */
fun pinEntries(
    context: android.content.Context,
    app: AppInfo,
    settings: LauncherSettings,
    onRename: (AppInfo) -> Unit,
    includeDesktop: Boolean = true,
): List<MenuEntry> = buildList {
    add(MenuEntry(if (app.key in settings.startPinned) "Unpin from Start" else "Pin to Start") { settings.toggleStart(app.key) })
    add(MenuEntry(if (app.key in settings.taskbarPinned) "Unpin from taskbar" else "Pin to taskbar") { settings.toggleTaskbar(app.key) })
    if (includeDesktop && !settings.isOnDesktop(app.key)) {
        add(MenuEntry("Add to desktop") { DesktopGrid.addApp(context, settings, app.key) })
    }
    add(MenuEntry("Rename") { onRename(app) })
}

/** Right-click style menu: app shortcuts, caller's entries, then split screen / info / uninstall. */
@Composable
fun AppContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    app: AppInfo,
    entries: List<MenuEntry>,
    onLaunched: () -> Unit = {},
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
        entries.forEach { entry -> item(entry.label, entry.action) }
        if (entries.isNotEmpty()) HorizontalDivider()
        item("Open in split screen") {
            AppLaunch.openSplit(context, app)
            onLaunched()
        }
        item("App info") { AppLaunch.appInfo(context, app) }
        item("Uninstall") { AppLaunch.uninstall(context, app) }
    }
}
