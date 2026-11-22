package io.github.roshansoni_io.kitab.feature.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Tracks pinching, panning, taps, long-press text selection, and
 * selection-handle dragging for a [ZoomState]-driven page.
 */
@Composable
fun Modifier.zoomable(
    state: ZoomState,
    onTap: (Offset) -> Unit = {},
    onGestureEnd: () -> Unit = {},
    onLongPress: (Offset) -> Boolean = { false },
    onSelectionDrag: (Offset) -> Unit = {},
    onSelectionEnd: () -> Unit = {},
    onHandleHit: (Offset) -> Boolean = { false },
    onHandleDrag: (Offset) -> Unit = {},
    onHandleEnd: () -> Unit = {},
): Modifier {
    // A pointerInput coroutine can outlive several recompositions. Keep the
    // callbacks current so a gesture started before page text loads does not
    // keep using the initial (empty) pageText captured by its closure.
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnGestureEnd by rememberUpdatedState(onGestureEnd)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnSelectionDrag by rememberUpdatedState(onSelectionDrag)
    val currentOnSelectionEnd by rememberUpdatedState(onSelectionEnd)
    val currentOnHandleHit by rememberUpdatedState(onHandleHit)
    val currentOnHandleDrag by rememberUpdatedState(onHandleDrag)
    val currentOnHandleEnd by rememberUpdatedState(onHandleEnd)

    return pointerInput(state) {
        awaitEachGesture {
            var movement = 0f
            var multipleFingers = false
            var consuming = state.isZoomed
            var pointerCount = 1
            var releasedPosition: Offset? = null
            var selecting = false
            var longPressFired = false
            var lastPosition: Offset

            val down = awaitFirstDown(requireUnconsumed = false)
            if (currentOnHandleHit(down.position)) {
                down.consume()
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        event.changes.forEach { it.consume() }
                        currentOnHandleDrag(pressed.first().position)
                    }
                } finally {
                    currentOnHandleEnd()
                }
                return@awaitEachGesture
            }
            lastPosition = down.position
            val downTime = System.currentTimeMillis()

            while (true) {
                val longPressRemaining =
                    if (!longPressFired && !multipleFingers && movement <= viewConfiguration.touchSlop) {
                        viewConfiguration.longPressTimeoutMillis - (System.currentTimeMillis() - downTime)
                    } else {
                        -1L
                    }

                if (longPressRemaining <= 0 && !longPressFired &&
                    !multipleFingers && movement <= viewConfiguration.touchSlop
                ) {
                    longPressFired = true
                    if (currentOnLongPress(lastPosition)) selecting = true
                    continue
                }

                val event =
                    if (longPressRemaining > 0) {
                        withTimeoutOrNull(longPressRemaining) { awaitPointerEvent() }
                    } else {
                        awaitPointerEvent()
                    }
                if (event == null) {
                    longPressFired = true
                    if (currentOnLongPress(lastPosition)) selecting = true
                    continue
                }

                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) {
                    releasedPosition = event.changes.lastOrNull()?.position
                    break
                }
                lastPosition = pressed.first().position

                if (selecting) {
                    event.changes.forEach { change -> change.consume() }
                    currentOnSelectionDrag(lastPosition)
                    continue
                }

                // A finger added or lifted changes the centroid discontinuously;
                // skip this frame's delta so the content does not jump.
                if (pressed.size != pointerCount) {
                    pointerCount = pressed.size
                    if (pressed.size >= 2) multipleFingers = true
                    continue
                }

                if (consuming) {
                    event.changes.forEach { change -> change.consume() }
                }

                val centroid = event.calculateCentroid(useCurrent = true)
                val pan = event.calculatePan()
                movement += pan.getDistance()
                when {
                    pressed.size >= 2 -> {
                        state.zoomBy(event.calculateZoom(), centroid)
                        consuming = true
                    }

                    state.isZoomed -> {
                        state.panBy(pan)
                        consuming = true
                    }
                }
            }

            if (selecting) {
                currentOnSelectionEnd()
                return@awaitEachGesture
            }

            state.settle()
            currentOnGestureEnd()

            val isTap =
                !multipleFingers &&
                    !longPressFired &&
                    movement <= viewConfiguration.touchSlop &&
                    releasedPosition != null

            if (isTap) {
                currentOnTap(releasedPosition ?: Offset.Zero)
            }
        }
    }
}
