package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.unit.IntSize

/**
 * Pure coordinate math for fitting and sharpening the rendered page inside the viewport.
 *
 * Page bitmaps can be larger or smaller than the on-screen size (base renders fit the
 * viewport width, sharp region re-renders match screen pixel density), so zoom scale must
 * always be measured against the *displayed letterboxed* size — never the bitmap pixel size.
 */
object PageGeometry {
    /**
     * Size the bitmap is actually displayed at: scaled to fit entirely inside
     * [viewport] (touching the smaller dimension, centered otherwise), preserving
     * the bitmap's aspect ratio.
     */
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
     *
     * The result covers exactly the portion of the page under the viewport after the
     * current zoom/pan transform, so rendering it yields roughly one viewport worth of
     * pixels — sharp text at any zoom depth without large allocations.
     *
     * @param zoomScale current pinch zoom (1 = fit)
     * @param panX horizontal zoom pan offset in pixels
     * @param panY vertical zoom pan offset in pixels
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

        // Screen-pixel density: region points × fit × zoom equals the on-screen size.
        val outputZoom = fit * zoomScale
        return RegionRenderSpec(pageRect = region, zoom = outputZoom)
    }
}

/** A sub-rectangle of a page (PDF points) and the density to render it at. */
data class RegionRenderSpec(
    val pageRect: RectF,
    val zoom: Float,
)

/** True when re-rendering [other] would produce effectively the same sharp region. */
fun RegionRenderSpec.matches(other: RegionRenderSpec?): Boolean {
    if (other == null) return false
    if (pageRect.width() <= 0f || pageRect.height() <= 0f) return false
    val sameRect =
        kotlin.math.abs(pageRect.left - other.pageRect.left) < 1f &&
            kotlin.math.abs(pageRect.top - other.pageRect.top) < 1f &&
            kotlin.math.abs(pageRect.right - other.pageRect.right) < 1f &&
            kotlin.math.abs(pageRect.bottom - other.pageRect.bottom) < 1f
    val sameZoom = kotlin.math.abs(zoom - other.zoom) <= other.zoom * 0.02f
    return sameRect && sameZoom
}
