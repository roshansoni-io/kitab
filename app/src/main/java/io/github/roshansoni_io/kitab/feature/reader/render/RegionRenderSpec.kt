package io.github.roshansoni_io.kitab.feature.reader.render

import android.graphics.RectF
import kotlin.math.abs

/** Edge tolerance in PDF points for [matches]. */
private const val RECT_TOLERANCE_POINTS = 1f

/** Relative zoom tolerance for [matches] (2%). */
private const val ZOOM_TOLERANCE_RATIO = 0.02f

/**
 * A sub-rectangle of a page (PDF points) and the density to render it at.
 *
 */
class RegionRenderSpec(
    val pageRect: RectF,
    val zoom: Float,
) {
    init {
        require(pageRect.width() > 0f && pageRect.height() > 0f) {
            "pageRect must have positive width and height, got $pageRect"
        }
        require(zoom > 0f) { "zoom must be > 0, got $zoom" }
    }

    override fun toString(): String =
        "RegionRenderSpec(left=${pageRect.left}, top=${pageRect.top}, " +
            "right=${pageRect.right}, bottom=${pageRect.bottom}, zoom=$zoom)"
}

/** True when re-rendering [other] would produce effectively the same sharp region. */
fun RegionRenderSpec.matches(other: RegionRenderSpec?): Boolean {
    if (other == null) return false
    // Defensive: callers can mutate the exposed RectF after construction.
    if (pageRect.width() <= 0f || pageRect.height() <= 0f) return false
    if (other.pageRect.width() <= 0f || other.pageRect.height() <= 0f) return false
    val sameRect =
        abs(pageRect.left - other.pageRect.left) < RECT_TOLERANCE_POINTS &&
            abs(pageRect.top - other.pageRect.top) < RECT_TOLERANCE_POINTS &&
            abs(pageRect.right - other.pageRect.right) < RECT_TOLERANCE_POINTS &&
            abs(pageRect.bottom - other.pageRect.bottom) < RECT_TOLERANCE_POINTS
    val sameZoom = abs(zoom - other.zoom) <= other.zoom * ZOOM_TOLERANCE_RATIO
    return sameRect && sameZoom
}
