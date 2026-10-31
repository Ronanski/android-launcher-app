package com.ronanski.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Windows-style desktop: a grid where app icons and widgets can be placed freely.
 * Long-press + drag moves things; long-press + release opens the item's menu.
 */
@Composable
fun Desktop(
    settings: LauncherSettings,
    repo: AppRepository,
    widgets: WidgetManager,
    font: FontFamily,
    onRename: (AppInfo) -> Unit,
    onBackgroundLongPress: (Offset) -> Unit,
    onGesture: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps = repo.apps
    val byKey = remember(apps) { apps.associateBy { it.key } }
    var resizingId by remember { mutableStateOf<String?>(null) }
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val cols = if (settings.desktopColumns > 0) {
            settings.desktopColumns
        } else {
            (maxWidth / 104.dp).toInt().coerceIn(4, 16)
        }
        val cellDp = maxWidth / cols
        val rows = (maxHeight / cellDp).toInt().coerceAtLeast(1)
        val cellPx = with(density) { cellDp.toPx() }
        SideEffect {
            DesktopGrid.columns = cols
            DesktopGrid.rows = rows
            DesktopGrid.cellPx = cellPx
        }
        DesktopGrid.columns = cols
        DesktopGrid.rows = rows
        DesktopGrid.cellPx = cellPx

        val items by rememberUpdatedState(settings.desktop)
        fun itemAt(p: Offset): DesktopItem? = items.firstOrNull { item ->
            val x = DesktopGrid.clampX(item) * cellPx
            val y = DesktopGrid.clampY(item) * cellPx
            p.x >= x && p.x < x + item.w * cellPx && p.y >= y && p.y < y + item.h * cellPx
        }

        // Empty-desktop gestures (behind the icons/widgets).
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(cellPx) {
                    detectTapGestures(
                        onTap = { resizingId = null },
                        onDoubleTap = { if (itemAt(it) == null) onGesture(settings.gestureDoubleTap) },
                        onLongPress = { if (itemAt(it) == null) onBackgroundLongPress(it) },
                    )
                }
                .pointerInput(cellPx) {
                    var total = 0f
                    var blocked = false
                    detectVerticalDragGestures(
                        onDragStart = {
                            total = 0f
                            blocked = itemAt(it)?.isWidget == true
                        },
                        onDragEnd = {
                            if (!blocked) {
                                when {
                                    total < -swipeThreshold -> onGesture(settings.gestureSwipeUp)
                                    total > swipeThreshold -> onGesture(settings.gestureSwipeDown)
                                }
                            }
                        },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            total += dy
                        },
                    )
                },
        )

        settings.desktop.forEach { item ->
            val app = if (item.isWidget) null else byKey[item.key]
            if (item.isWidget || app != null) {
                key(item.id) {
                    DesktopItemView(
                        item = item,
                        app = app,
                        settings = settings,
                        repo = repo,
                        widgets = widgets,
                        font = font,
                        cellPx = cellPx,
                        cellDp = cellDp,
                        resizing = resizingId == item.id,
                        onResize = { resizingId = if (resizingId == item.id) null else item.id },
                        onRename = onRename,
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopItemView(
    item: DesktopItem,
    app: AppInfo?,
    settings: LauncherSettings,
    repo: AppRepository,
    widgets: WidgetManager,
    font: FontFamily,
    cellPx: Float,
    cellDp: Dp,
    resizing: Boolean,
    onResize: () -> Unit,
    onRename: (AppInfo) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(item)
    val currentApp by rememberUpdatedState(app)
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var resizeBy by remember { mutableStateOf(Offset.Zero) }

    val x = DesktopGrid.clampX(item)
    val y = DesktopGrid.clampY(item)
    val widthPx = (item.w * cellPx + resizeBy.x).coerceAtLeast(cellPx)
    val heightPx = (item.h * cellPx + resizeBy.y).coerceAtLeast(cellPx)
    val width = with(density) { widthPx.toDp() }
    val height = with(density) { heightPx.toDp() }
    val accent = Color(settings.accentColor)

    Box(
        Modifier
            .offset { IntOffset((x * cellPx + drag.x).roundToInt(), (y * cellPx + drag.y).roundToInt()) }
            .size(width, height)
            .zIndex(if (dragging || resizing) 1f else 0f)
            .graphicsLayer {
                val s = if (dragging) 1.06f else 1f
                scaleX = s
                scaleY = s
                alpha = if (dragging) 0.85f else 1f
            }
            .longPressDrag(
                key = item.id to cellPx,
                onTap = if (item.isWidget) {
                    null
                } else {
                    {
                        currentApp?.let { AppLaunch.open(context, it) }
                        Unit
                    }
                },
                onLongPress = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu = true
                },
                onRelease = { },
                onDragStart = {
                    dragging = true
                    menu = false
                },
                onDrag = { drag += it },
                onDragEnd = {
                    val it0 = current
                    val nx = ((DesktopGrid.clampX(it0) * cellPx + drag.x) / cellPx).roundToInt()
                    val ny = ((DesktopGrid.clampY(it0) * cellPx + drag.y) / cellPx).roundToInt()
                    if (DesktopGrid.fits(settings.desktop, nx, ny, it0.w, it0.h, it0.id)) {
                        settings.updateDesktop(it0.id) { d -> d.copy(x = nx, y = ny) }
                    }
                    drag = Offset.Zero
                    dragging = false
                },
            ),
    ) {
        if (item.isWidget) {
            val widgetId = item.key.toIntOrNull() ?: -1
            WidgetView(widgetId, widgets, width, height, Modifier.fillMaxSize().padding(6.dp))
            if (resizing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .border(2.dp, accent, RoundedCornerShape(12.dp)),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .pointerInput(item.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    val it0 = current
                                    val nw = ((it0.w * cellPx + resizeBy.x) / cellPx).roundToInt().coerceAtLeast(1)
                                    val nh = ((it0.h * cellPx + resizeBy.y) / cellPx).roundToInt().coerceAtLeast(1)
                                    val cx = DesktopGrid.clampX(it0)
                                    val cy = DesktopGrid.clampY(it0)
                                    if (DesktopGrid.fits(settings.desktop, cx, cy, nw, nh, it0.id)) {
                                        settings.updateDesktop(it0.id) { d -> d.copy(x = cx, y = cy, w = nw, h = nh) }
                                    }
                                    resizeBy = Offset.Zero
                                },
                                onDragCancel = { resizeBy = Offset.Zero },
                            ) { change, amount ->
                                change.consume()
                                resizeBy += amount
                            }
                        },
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(if (resizing) "Done resizing" else "Resize") }, onClick = {
                    menu = false
                    onResize()
                })
                DropdownMenuItem(text = { Text("Remove widget") }, onClick = {
                    menu = false
                    widgets.delete(widgetId)
                    settings.removeFromDesktop(item.id)
                })
            }
        } else if (app != null) {
            DesktopAppIcon(app, repo, settings, font, cellDp)
            AppContextMenu(
                expanded = menu,
                onDismiss = { menu = false },
                app = app,
                entries = windowEntries(context, app, settings) +
                    pinEntries(context, app, settings, onRename, includeDesktop = false) +
                    MenuEntry("Remove from desktop") { settings.removeFromDesktop(item.id) },
            )
        }
    }
}

@Composable
private fun DesktopAppIcon(app: AppInfo, repo: AppRepository, settings: LauncherSettings, font: FontFamily, cell: Dp) {
    val iconSize = min(settings.iconSize.dp, cell * 0.62f)
    Column(
        Modifier.fillMaxSize().padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIconImage(app, repo, settings, iconSize)
        if (settings.showDesktopLabels) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = repo.label(app),
                color = Color(settings.labelColor),
                fontSize = settings.labelSize.sp,
                lineHeight = (settings.labelSize + 2).sp,
                fontFamily = font,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = labelStyle(settings),
            )
        }
    }
}
