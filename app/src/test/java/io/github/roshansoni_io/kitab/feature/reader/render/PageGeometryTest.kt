package io.github.roshansoni_io.kitab.feature.reader.render

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class PageGeometryTest {
    // Page 500x1000 points fitted exactly into a 1000x2000 viewport (fit = 2).
    private val page = RectF(0f, 0f, 500f, 1000f)
    private val viewport = IntSize(1000, 2000)

    @Test
    fun `visible region at 2x centered zoom covers half the page`() {
        val spec = PageGeometry.visiblePageRegion(page, viewport, zoomScale = 2f, panX = 0f, panY = 0f)
        assertNotNull(spec)
        val region = spec!!.pageRect
        assertEquals(125f, region.left, 0.5f)
        assertEquals(250f, region.top, 0.5f)
        assertEquals(375f, region.right, 0.5f)
        assertEquals(750f, region.bottom, 0.5f)
        // desiredZoom = fit * zoom * supersample = 2 * 2 * 2.
        assertEquals(8f, spec.zoom, 0.01f)
    }

    @Test
    fun `pan shifts the visible region within the page`() {
        // Panning right by half the viewport reveals the page area to the left.
        val spec =
            PageGeometry.visiblePageRegion(
                page,
                viewport,
                zoomScale = 2f,
                panX = 500f,
                panY = 0f,
            )
        assertNotNull(spec)
        val region = spec!!.pageRect
        assertEquals(0f, region.left, 0.5f)
        assertEquals(250f, region.top, 0.5f)
    }

    @Test
    fun `region is null when the viewport is fully off the page`() {
        // Panned far past the page edge nothing is visible, so no region is produced.
        assertNull(
            PageGeometry.visiblePageRegion(
                page,
                viewport,
                zoomScale = 5f,
                panX = 10_000f,
                panY = 10_000f,
            ),
        )
    }

    @Test
    fun `regionScreenRect inverts visiblePageRegion`() {
        val spec = PageGeometry.visiblePageRegion(page, viewport, zoomScale = 2f, panX = 0f, panY = 0f)!!
        val screen =
            PageGeometry
                .regionScreenRect(spec, page, viewport, zoomScale = 2f, panX = 0f, panY = 0f)!!
        assertEquals(0f, screen.left, 0.5f)
        assertEquals(0f, screen.top, 0.5f)
        assertEquals(1000f, screen.right, 0.5f)
        assertEquals(2000f, screen.bottom, 0.5f)
    }

    @Test
    fun `invalid inputs return null`() {
        assertNull(PageGeometry.visiblePageRegion(RectF(0f, 0f, 0f, 100f), viewport, 2f, 0f, 0f))
        assertNull(PageGeometry.visiblePageRegion(page, IntSize(0, 100), 2f, 0f, 0f))
        assertNull(PageGeometry.visiblePageRegion(page, viewport, 0f, 0f, 0f))
    }

    @Test
    fun `matches is tolerant only within bounds`() {
        val spec = RegionRenderSpec(RectF(0f, 0f, 100f, 100f), zoom = 2f)
        assertTrue(spec.matches(RegionRenderSpec(RectF(0.5f, 0f, 100f, 100f), zoom = 2.02f)))
        assertFalse(spec.matches(RegionRenderSpec(RectF(5f, 0f, 100f, 100f), zoom = 2f)))
        assertFalse(spec.matches(RegionRenderSpec(RectF(0f, 0f, 100f, 100f), zoom = 2.1f)))
        assertFalse(spec.matches(null))
    }

    @Test
    fun `fittedSize scales the bitmap to fit the viewport`() {
        val bitmap = Bitmap.createBitmap(1000, 500, Bitmap.Config.ARGB_8888)
        val fitted = PageGeometry.fittedSize(bitmap, IntSize(500, 250))
        assertEquals(500, fitted.width)
        assertEquals(250, fitted.height)
    }

    @Test
    fun `fittedSize keeps the taller axis at viewport width`() {
        val bitmap = Bitmap.createBitmap(1000, 2000, Bitmap.Config.ARGB_8888)
        val fitted = PageGeometry.fittedSize(bitmap, IntSize(1000, 500))
        assertEquals(250, fitted.width)
        assertEquals(500, fitted.height)
    }
}
