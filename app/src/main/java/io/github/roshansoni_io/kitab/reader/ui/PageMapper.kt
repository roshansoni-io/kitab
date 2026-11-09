package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.roshansoni_io.kitab.reader.LinkTarget

/**
 * Maps coordinates between PDF page space (points) and the un-zoomed display
 * space of the rendered page image inside the viewport.
 *
 * The page image is letterboxed exactly like [androidx.compose.ui.layout.ContentScale.Fit]:
 * one uniform scale, centered in the viewport — so the mapping here stays valid
 * regardless of the bitmap's pixel resolution.
 */
class PageMapper(
    pageSizePoints: RectF,
    viewportSize: IntSize,
) {
    private val scale: Float
    private val offsetX: Float
    private val offsetY: Float
    private val pageLeft: Float = pageSizePoints.left
    private val pageTop: Float = pageSizePoints.top

    init {
        val pageWidth = pageSizePoints.right - pageSizePoints.left
        val pageHeight = pageSizePoints.bottom - pageSizePoints.top
        val usable =
            pageWidth > 0f && pageHeight > 0f &&
                viewportSize.width > 0 && viewportSize.height > 0
        if (usable) {
            scale =
                minOf(
                    viewportSize.width / pageWidth,
                    viewportSize.height / pageHeight,
                )
            offsetX = (viewportSize.width - pageWidth * scale) / 2f
            offsetY = (viewportSize.height - pageHeight * scale) / 2f
        } else {
            scale = 0f
            offsetX = 0f
            offsetY = 0f
        }
    }

    val isUsable: Boolean get() = scale > 0f

    /** Converts a point from un-zoomed display space to PDF page points. */
    fun toPagePoint(display: Offset): Offset =
        Offset(
            pageLeft + (display.x - offsetX) / scale,
            pageTop + (display.y - offsetY) / scale,
        )

    /** Converts a PDF page point to un-zoomed display space. */
    fun toDisplay(pagePoint: Offset): Offset =
        Offset(
            offsetX + (pagePoint.x - pageLeft) * scale,
            offsetY + (pagePoint.y - pageTop) * scale,
        )

    /** Returns the link whose region contains [pagePoint] (slop-expanded), if any. */
    fun hitTest(
        targets: List<LinkTarget>,
        pagePoint: Offset,
        touchSlopPx: Float,
    ): LinkTarget? {
        val slop = touchSlopPx / scale
        for (target in targets) {
            val expanded =
                RectF(target.rect).apply {
                    inset(-slop, -slop)
                }
            if (expanded.contains(pagePoint.x, pagePoint.y)) return target
        }
        return null
    }
}
