package io.github.roshansoni_io.kitab.reader.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.hypot

class ZoomState(
    private val viewportSize: () -> IntSize,
    private val contentSize: () -> IntSize,
) {
    var scale by mutableStateOf(1f)
        private set

    var offsetX by mutableFloatStateOf(0f)
        private set

    var offsetY by mutableFloatStateOf(0f)
        private set

    val isZoomed: Boolean get() = scale > 1f + MAX_SCALE_ERROR

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun zoomBy(
        zoomFactor: Float,
        centroid: Offset,
    ) {
        val newScale = (scale * zoomFactor).coerceIn(MIN_SCALE, MAX_SCALE)
        val retained = newScale / scale
        val pivot = centroid - viewportCenter()
        scale = newScale
        offsetX = pivot.x * (1f - retained) + retained * offsetX
        offsetY = pivot.y * (1f - retained) + retained * offsetY
        clampOffsets()
    }

    fun panBy(pan: Offset): Offset {
        val previousX = offsetX
        val previousY = offsetY
        offsetX += pan.x
        offsetY += pan.y
        clampOffsets()
        return Offset(pan.x - (offsetX - previousX), pan.y - (offsetY - previousY))
    }

    fun settle() {
        if (scale < SNAP_BACK_THRESHOLD) {
            reset()
        } else {
            clampOffsets()
        }
    }

    /**
     * Jumps to [newScale] anchored at a page boundary so a flipped-in page keeps the
     * reading position: [anchorStart] pins the top-left corner of the content on
     * screen, otherwise the bottom-left corner is pinned. Offsets fall back to zero
     * while the content size is not yet known from a rendered bitmap.
     */
    fun anchorTo(
        newScale: Float,
        anchorStart: Boolean,
    ) {
        scale = newScale.coerceIn(MIN_SCALE, MAX_SCALE)
        val content = contentSize()
        val viewport = viewportSize()
        if (content == IntSize.Zero || viewport == IntSize.Zero) {
            offsetX = 0f
            offsetY = 0f
            return
        }
        val maxOffsetX = maxOf(0f, content.width * scale / 2f - viewport.width / 2f)
        val maxOffsetY = maxOf(0f, content.height * scale / 2f - viewport.height / 2f)
        // A positive translation moves the content right/down, which reveals its
        // left/top portion; the clamped maximum is exactly the edge-anchored offset.
        offsetX = maxOffsetX
        offsetY = if (anchorStart) maxOffsetY else -maxOffsetY
    }

    private fun viewportCenter(): Offset = viewportSize().let { Offset(it.width / 2f, it.height / 2f) }

    /**
     * Maps a point in layout (screen) coordinates to un-zoomed content coordinates,
     * inverting the scale/pan transform of the current zoom state. Used for
     * hit-testing content (links, selection) while the page is zoomed.
     */
    fun fromScreen(point: Offset): Offset {
        val center = viewportCenter()
        if (viewportSize() == IntSize.Zero) return point
        val panned = point - center - Offset(offsetX, offsetY)
        return center + Offset(panned.x / scale, panned.y / scale)
    }

    /** Inverse of [fromScreen]: maps un-zoomed content coordinates to layout coordinates. */
    fun toScreen(point: Offset): Offset {
        val center = viewportCenter()
        if (viewportSize() == IntSize.Zero) return point
        return center + Offset((point.x - center.x) * scale + offsetX, (point.y - center.y) * scale + offsetY)
    }

    private fun clampOffsets() {
        val viewport = viewportSize()
        val content = contentSize()
        if (content == IntSize.Zero || viewport == IntSize.Zero) return
        val maxOffsetX = maxOf(0f, content.width * scale / 2f - viewport.width / 2f)
        val maxOffsetY = maxOf(0f, content.height * scale / 2f - viewport.height / 2f)
        offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
        offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f
        const val MAX_SCALE_ERROR = 0.01f
        const val SNAP_BACK_THRESHOLD = 1.05f
    }
}

@Composable
fun Modifier.zoomable(
    state: ZoomState,
    onTap: (Offset) -> Unit = {},
    onGestureEnd: () -> Unit = {},
    onPageEdgeSwipe: (Int) -> Unit = {},
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
    val currentOnPageEdgeSwipe by rememberUpdatedState(onPageEdgeSwipe)
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
            var previousCentroid: Offset? = null
            var previousSpan = 0f
            var pointerCount = 1
            var releasedPosition: Offset? = null
            var selecting = false
            var longPressFired = false
            var edgeSwipeDistance = 0f
            var pageSwipeSent = false
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
                    previousCentroid = null
                    previousSpan = 0f
                    if (pressed.size >= 2) multipleFingers = true
                    previousCentroid = pressedCentroid(pressed)
                    previousSpan = pressedSpan(pressed)
                    continue
                }

                if (consuming) {
                    event.changes.forEach { change -> change.consume() }
                }

                val centroid = pressedCentroid(pressed)
                val span = pressedSpan(pressed)
                val previous = previousCentroid
                if (previous != null) {
                    val pan = centroid - previous
                    movement += pan.getDistance()
                    when {
                        pressed.size >= 2 && previousSpan > 0f && span > 0f -> {
                            state.zoomBy(span / previousSpan, centroid)
                            consuming = true
                        }

                        state.isZoomed && pressed.size == 1 -> {
                            val unconsumed = state.panBy(pan)
                            consuming = true
                            // Pan is clamped at the content edge; whatever the
                            // clamp refuses keeps accumulating until it releases
                            // the pager to flip in the neighbouring page.
                            edgeSwipeDistance += unconsumed.x
                            if (!pageSwipeSent &&
                                abs(edgeSwipeDistance) >= viewConfiguration.touchSlop * 2f
                            ) {
                                pageSwipeSent = true
                                currentOnPageEdgeSwipe(if (edgeSwipeDistance < 0f) 1 else -1)
                            }
                        }
                    }
                }
                previousCentroid = centroid
                previousSpan = span
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

private fun pressedCentroid(pressed: List<PointerInputChange>): Offset {
    val first = pressed.first().position
    val last = pressed.last().position
    return Offset((first.x + last.x) / 2f, (first.y + last.y) / 2f)
}

private fun pressedSpan(pressed: List<PointerInputChange>): Float {
    if (pressed.size < 2) return 0f
    val first = pressed.first().position
    val last = pressed.last().position
    return hypot(first.x - last.x, first.y - last.y)
}

/** Zoom scale transferred from the page the user left when a zoomed edge-swipe flips. */
data class ZoomCarry(
    val targetPage: Int,
    val scale: Float,
    val anchorStart: Boolean,
)
