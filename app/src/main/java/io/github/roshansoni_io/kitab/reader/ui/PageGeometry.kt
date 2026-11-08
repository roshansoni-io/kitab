package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.sqrt

/** Edge tolerance in PDF points for [matches]. */
private const val RECT_TOLERANCE_POINTS = 1f

/** Relative zoom tolerance for [matches] (2%). */
private const val ZOOM_TOLERANCE_RATIO = 0.02f

/**
 * Pure coordinate math for fitting and sharpening the rendered page inside the viewport.
 */
object PageGeometry {
    /** Extra render density on top of screen pixels, applied only when zoomed in. */
    private const val SUPERSAMPLE = 2f

    /** Safety caps for the single sharp-region bitmap. */
    private const val MAX_REGION_PIXELS = 12_000_000f
    private const val MAX_REGION_DIMENSION = 4096f

    fun fittedSize(
        bitmap: Bitmap,
        viewport: IntSize,
    ): IntSize {
        if (bitmap.width <= 0 || bitmap.height <= 0 || viewport.width <= 0 || viewport.height <= 0) {
            return IntSize.Zero
        }
        val fitScale =
            minOf(
                viewport.width.toFloat() / bitmap.width,
                viewport.height.toFloat() / bitmap.height,
            )
        return IntSize(
            width = (bitmap.width * fitScale).toInt().coerceAtLeast(1),
            height = (bitmap.height * fitScale).toInt().coerceAtLeast(1),
        )
    }

    /**
     * Works out which part of the PDF page is currently visible and at what output
     * resolution it should be re-rendered so it is sharp on screen.
     */

    fun visiblePageRegion(
        pageSizePoints: RectF,
        viewport: IntSize,
        zoomScale: Float,
        panX: Float,
        panY: Float,
    ): RegionRenderSpec? {
        val pageLeft = pageSizePoints.left
        val pageTop = pageSizePoints.top
        val pageWidth = pageSizePoints.width()
        val pageHeight = pageSizePoints.height()
        if (pageWidth <= 0f || pageHeight <= 0f || zoomScale <= 0f) return null
        if (viewport.width <= 0 || viewport.height <= 0) return null

        val fit =
            minOf(
                viewport.width.toFloat() / pageWidth,
                viewport.height.toFloat() / pageHeight,
            )
        val offsetX = (viewport.width - pageWidth * fit) / 2f
        val offsetY = (viewport.height - pageHeight * fit) / 2f
        val centerX = viewport.width / 2f
        val centerY = viewport.height / 2f

        // Viewport corners → un-zoomed content space → PDF page points.
        fun pageX(screenX: Float): Float {
            val contentX = centerX + (screenX - centerX - panX) / zoomScale
            return ((contentX - offsetX) / fit + pageLeft)
                .coerceIn(pageLeft, pageLeft + pageWidth)
        }

        fun pageY(screenY: Float): Float {
            val contentY = centerY + (screenY - centerY - panY) / zoomScale
            return ((contentY - offsetY) / fit + pageTop)
                .coerceIn(pageTop, pageTop + pageHeight)
        }

        val region =
            RectF(
                pageX(0f),
                pageY(0f),
                pageX(viewport.width.toFloat()),
                pageY(viewport.height.toFloat()),
            )
        if (region.width() <= 0.5f || region.height() <= 0.5f) return null

        // Supersample only when actually zoomed past fit.
        val supersample = if (zoomScale > 1f) SUPERSAMPLE else 1f
        val desiredZoom = fit * zoomScale * supersample

        // Clamp density so the requested region fits in memory and texture limits.
        val pixelArea = region.width() * region.height()
        val maxZoomByPixels = sqrt(MAX_REGION_PIXELS / pixelArea)
        val maxZoomByDimension =
            minOf(
                MAX_REGION_DIMENSION / region.width(),
                MAX_REGION_DIMENSION / region.height(),
            )
        val outputZoom = minOf(desiredZoom, maxZoomByPixels, maxZoomByDimension)
        return RegionRenderSpec(pageRect = region, zoom = outputZoom)
    }

    /**
     * Inverse of [visiblePageRegion]'s transform: where [region]'s rectangle lands on
     * screen, in **screen pixels**, under the given zoom/pan.

     */
    fun regionScreenRect(
        region: RegionRenderSpec,
        pageSizePoints: RectF,
        viewport: IntSize,
        zoomScale: Float,
        panX: Float,
        panY: Float,
    ): RectF? {
        val pageLeft = pageSizePoints.left
        val pageTop = pageSizePoints.top
        val pageWidth = pageSizePoints.width()
        val pageHeight = pageSizePoints.height()
        if (pageWidth <= 0f || pageHeight <= 0f || zoomScale <= 0f) return null
        if (viewport.width <= 0 || viewport.height <= 0) return null

        val fit =
            minOf(
                viewport.width.toFloat() / pageWidth,
                viewport.height.toFloat() / pageHeight,
            )
        val offsetX = (viewport.width - pageWidth * fit) / 2f
        val offsetY = (viewport.height - pageHeight * fit) / 2f
        val centerX = viewport.width / 2f
        val centerY = viewport.height / 2f

        // PDF page points → letterboxed content space → zoomed + panned screen space.
        fun screenX(pageX: Float): Float {
            val contentX = offsetX + (pageX - pageLeft) * fit
            return centerX + (contentX - centerX) * zoomScale + panX
        }

        fun screenY(pageY: Float): Float {
            val contentY = offsetY + (pageY - pageTop) * fit
            return centerY + (contentY - centerY) * zoomScale + panY
        }

        return RectF(
            screenX(region.pageRect.left),
            screenY(region.pageRect.top),
            screenX(region.pageRect.right),
            screenY(region.pageRect.bottom),
        )
    }
}

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
