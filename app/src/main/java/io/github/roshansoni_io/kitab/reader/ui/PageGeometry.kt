package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.Bitmap
import androidx.compose.ui.unit.IntSize

/**
 * Pure coordinate math for fitting a rendered page bitmap inside the reader viewport.
 *
 * Page bitmaps can be much larger than the on-screen size (the browser renders pages
 * wider than the viewport so zoomed content stays sharp), so zoom scale must always
 * be measured against the *displayed* letterboxed size — never the bitmap pixel size.
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
}
