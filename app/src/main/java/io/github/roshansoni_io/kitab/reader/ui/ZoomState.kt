package io.github.roshansoni_io.kitab.reader.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

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
