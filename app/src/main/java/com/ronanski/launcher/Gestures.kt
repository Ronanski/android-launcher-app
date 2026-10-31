package com.ronanski.launcher

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed

/**
 * Tap / long-press / long-press-then-drag, detected in the Initial pass so it also works
 * on top of widgets (Android views) without stealing their normal taps and scrolls.
 *
 * - quick tap -> [onTap] (if given)
 * - long press, released without moving -> [onRelease] (e.g. show menu)
 * - long press then move -> [onDragStart], [onDrag] (deltas), [onDragEnd]
 */
fun Modifier.longPressDrag(
    key: Any?,
    onTap: (() -> Unit)?,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        var tapped = false
        val endedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null) break
                if (!change.pressed) {
                    tapped = true
                    break
                }
                if ((change.position - down.position).getDistance() > slop) break
            }
            true
        }
        if (endedEarly != null) {
            if (tapped) onTap?.invoke()
            return@awaitEachGesture
        }

        onLongPress()
        var dragging = false
        var total = Offset.Zero
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val delta = change.positionChangeIgnoreConsumed()
            event.changes.forEach { it.consume() }
            if (!change.pressed) break
            total += delta
            if (!dragging && total.getDistance() > slop) {
                dragging = true
                onDragStart()
                onDrag(total)
            } else if (dragging) {
                onDrag(delta)
            }
        }
        if (dragging) onDragEnd() else onRelease()
    }
}
