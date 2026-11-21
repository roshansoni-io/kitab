package io.github.roshansoni_io.kitab.feature.reader.render

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import io.github.roshansoni_io.kitab.feature.reader.document.LinkTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PageMapperTest {
    // Page 100x50 points letterboxed into a 200x100 viewport: scale = 2, no padding.
    @Test
    fun `page center maps to viewport center`() {
        val mapper = PageMapper(RectF(0f, 0f, 100f, 50f), IntSize(200, 100))
        val display = mapper.toDisplay(Offset(50f, 25f))
        assertEquals(100f, display.x, 0.01f)
        assertEquals(50f, display.y, 0.01f)
    }

    // Viewport 200x100 with page 100x100 points: scale = min(2, 1) = 1, vertical
    // fit exact, horizontal letterboxed by 50px on each side.
    @Test
    fun `letterboxes along the shorter axis`() {
        val mapper = PageMapper(RectF(0f, 0f, 100f, 100f), IntSize(200, 100))
        assertEquals(50f, mapper.toDisplay(Offset(0f, 0f)).x, 0.01f)
        assertEquals(0f, mapper.toDisplay(Offset(0f, 0f)).y, 0.01f)
    }

    @Test
    fun `page rect origin is respected`() {
        val mapper = PageMapper(RectF(10f, 20f, 110f, 70f), IntSize(200, 100))
        val display = mapper.toDisplay(Offset(10f, 20f))
        assertEquals(0f, display.x, 0.01f)
        assertEquals(0f, display.y, 0.01f)
        val pagePoint = mapper.toPagePoint(display)
        assertEquals(10f, pagePoint.x, 0.01f)
        assertEquals(20f, pagePoint.y, 0.01f)
    }

    @Test
    fun `display to page point round trip preserves input`() {
        val mapper = PageMapper(RectF(0f, 0f, 100f, 50f), IntSize(200, 100))
        val point = Offset(37f, 21f)
        val pagePoint = mapper.toPagePoint(point)
        val roundTrip = mapper.toDisplay(pagePoint)
        assertEquals(point.x, roundTrip.x, 0.01f)
        assertEquals(point.y, roundTrip.y, 0.01f)
    }

    @Test
    fun `isUsable is false for empty inputs`() {
        assertFalse(PageMapper(RectF(0f, 0f, 0f, 50f), IntSize(200, 100)).isUsable)
        assertFalse(PageMapper(RectF(0f, 0f, 100f, 50f), IntSize(0, 100)).isUsable)
    }

    @Test
    fun `hitTest matches the link containing the point`() {
        val mapper = PageMapper(RectF(0f, 0f, 100f, 50f), IntSize(200, 100))
        val link =
            LinkTarget(
                rect = RectF(40f, 10f, 60f, 20f),
                targetPage = 2,
                uri = null,
            )
        val hit = mapper.hitTest(listOf(link), Offset(50f, 15f), touchSlopPx = 0f)
        assertEquals(link, hit)
        assertNull(mapper.hitTest(listOf(link), Offset(20f, 15f), touchSlopPx = 0f))
        // Touch slop is converted into page points by the page scale before matching.
        val withinSlop = mapper.hitTest(listOf(link), Offset(20f, 15f), touchSlopPx = 40f)
        assertEquals(link, withinSlop)
    }
}
