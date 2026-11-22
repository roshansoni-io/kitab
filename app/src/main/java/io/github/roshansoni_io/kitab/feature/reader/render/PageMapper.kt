package io.github.roshansoni_io.kitab.feature.reader.render

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.roshansoni_io.kitab.feature.reader.document.LinkTarget

/**
 * Maps coordinates between PDF page space (points) and the un-zoomed display
 * space of the rendered page image inside the viewport.
 *
 * The page image is letterboxed exactly like [androidx.compose.ui.layout.ContentScale.Fit]:
 * one uniform scale, centered in the viewport — so the mapping here stays valid
 * regardless of the bitmap's pixel resolution.
 */
class PageMapper(
    private val pageSizePoints: RectF,
    viewportSize: IntSize,
) {
    private val fit = PageGeometry.pageFit(pageSizePoints, viewportSize)

    val isUsable: Boolean get() = fit.scale > 0f

    /** Converts a point from un-zoomed display space to PDF page points. */
    fun toPagePoint(display: Offset): Offset = fit.toPagePoint(display, pageSizePoints)

    /** Converts a PDF page point to un-zoomed display space. */
    fun toDisplay(pagePoint: Offset): Offset = fit.toDisplay(pagePoint, pageSizePoints)

    /** Returns the link whose region contains [pagePoint] (slop-expanded), if any. */
    fun hitTest(
        targets: List<LinkTarget>,
        pagePoint: Offset,
        touchSlopPx: Float,
    ): LinkTarget? {
        val slop = touchSlopPx / fit.scale
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
