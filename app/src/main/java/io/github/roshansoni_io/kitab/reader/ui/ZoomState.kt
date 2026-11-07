package io.github.roshansoni_io.kitab.reader.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
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

    fun panBy(pan: Offset) {
        offsetX += pan.x
        offsetY += pan.y
        clampOffsets()
    }

    fun settle() {
        if (scale < SNAP_BACK_THRESHOLD) {
            reset()
        } else {
            clampOffsets()
        }
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

fun Modifier.zoomable(
    state: ZoomState,
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onGestureEnd: () -> Unit = {},
): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            var movement = 0f
            var multipleFingers = false
            var consuming = state.isZoomed
            var previousCentroid: Offset? = null
            var previousSpan = 0f
            var releasedPosition: Offset? = null

            awaitFirstDown(requireUnconsumed = false)
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) {
                    releasedPosition = event.changes.lastOrNull()?.position
                    break
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

                        pressed.size >= 2 -> {
                            consuming = true
                        }

                        state.isZoomed -> {
                            state.panBy(pan)
                            consuming = true
                        }
                    }
                }
                if (pressed.size >= 2) multipleFingers = true
                previousCentroid = centroid
                previousSpan = span
            }

            state.settle()
            onGestureEnd()

            val isTap =
                !multipleFingers &&
                    movement <= viewConfiguration.touchSlop &&
                    releasedPosition != null

            if (isTap) {
                maybeDetectDoubleTap(releasedPosition ?: Offset.Zero, onTap, onDoubleTap)
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

private suspend fun AwaitPointerEventScope.maybeDetectDoubleTap(
    firstTapPoint: Offset,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
) {
    val secondDown =
        withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
            awaitFirstDown(requireUnconsumed = false)
        } ?: run {
            onTap(firstTapPoint)
            return
        }

    var secondMovement = 0f
    var secondTapPoint: Offset? = secondDown.position
    while (true) {
        val event = awaitPointerEvent()
        val pressed = event.changes.filter { it.pressed }
        if (pressed.isEmpty()) {
            secondTapPoint = event.changes.lastOrNull()?.position
            break
        }
        val centroid = pressedCentroid(pressed)
        secondTapPoint?.let { previous -> secondMovement += (centroid - previous).getDistance() }
        secondTapPoint = centroid
    }

    val secondTap = secondTapPoint
    val isDoubleTap =
        secondTap != null &&
            secondMovement <= viewConfiguration.touchSlop &&
            hypot(secondTap.x - firstTapPoint.x, secondTap.y - firstTapPoint.y) <= DOUBLE_TAP_SLACK_PX

    if (isDoubleTap) {
        onDoubleTap(secondTap)
    } else {
        onTap(firstTapPoint)
    }
}

private const val DOUBLE_TAP_SLACK_PX = 80f
