package io.github.roshansoni_io.kitab.feature.reader.selection.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
import io.github.roshansoni_io.kitab.feature.reader.selection.PageText
import io.github.roshansoni_io.kitab.feature.reader.selection.SelectionHandle
import kotlin.math.min

internal val SELECTION_HANDLE_WIDTH = 40.dp
internal val SELECTION_HANDLE_HEIGHT = 56.dp

@Composable
fun SelectionOverlay(
    pageText: PageText,
    startIndex: Int,
    endIndex: Int,
    pageSize: RectF,
    selectionColor: SelectionColor,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { hideFromAccessibility() },
    ) {
        val pageWidth = pageSize.width()
        val pageHeight = pageSize.height()
        if (pageWidth <= 0f || pageHeight <= 0f) return@Canvas

        // Aspect-fit the page into the canvas.

        val scale = min(size.width / pageWidth, size.height / pageHeight)
        val offsetX = (size.width - pageWidth * scale) / 2f
        val offsetY = (size.height - pageHeight * scale) / 2f

        val highlight = selectionColor.composeColor.copy(alpha = 0.38f)
        val first = minOf(startIndex, endIndex)
        val last = maxOf(startIndex, endIndex)

        pageText.lineRanges.forEach { line ->
            val lineFirst = maxOf(first, line.first)
            val lineLast = minOf(last, line.last)
            if (lineFirst <= lineLast) {
                var current = RectF(pageText.chars[lineFirst].rect)
                for (index in lineFirst + 1..lineLast) {
                    val next = pageText.chars[index].rect
                    if (next.left <= current.right + 1f && next.right >= current.left - 1f) {
                        current.union(next)
                    } else {
                        drawSelectionRect(current, highlight, pageSize, scale, offsetX, offsetY)
                        current = RectF(next)
                    }
                }
                drawSelectionRect(current, highlight, pageSize, scale, offsetX, offsetY)
            }
        }
    }
}

private fun DrawScope.drawSelectionRect(
    rect: RectF,
    color: Color,
    pageSize: RectF,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
) {
    drawRect(
        color = color,
        topLeft = Offset(offsetX + (rect.left - pageSize.left) * scale, offsetY + (rect.top - pageSize.top) * scale),
        size = Size(rect.width() * scale, rect.height() * scale),
    )
}

@Composable
internal fun SelectionHandles(
    start: Offset,
    end: Offset,
    painter: Painter,
    tint: Color,
    modifier: Modifier = Modifier,
    activeHandle: SelectionHandle? = null,
    activeTipPosition: Offset? = null,
) {
    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { hideFromAccessibility() },
    ) {
        drawSelectionHandle(
            painter,
            if (activeHandle == SelectionHandle.Start) activeTipPosition ?: start else start,
            tint,
        )
        drawSelectionHandle(
            painter,
            if (activeHandle == SelectionHandle.End) activeTipPosition ?: end else end,
            tint,
        )
    }
}

private fun DrawScope.drawSelectionHandle(
    painter: Painter,
    anchor: Offset,
    tint: Color,
) {
    val width = SELECTION_HANDLE_WIDTH.toPx()
    val height = SELECTION_HANDLE_HEIGHT.toPx()
    translate(left = anchor.x - width / 2f, top = anchor.y) {
        with(painter) {
            draw(size = Size(width, height), colorFilter = ColorFilter.tint(tint))
        }
    }
}

internal val SelectionColor.composeColor: Color
    get() =
        when (this) {
            SelectionColor.Blue -> Color(0xFF2196F3)
            SelectionColor.Amber -> Color(0xFFFFB300)
            SelectionColor.Green -> Color(0xFF43A047)
            SelectionColor.Rose -> Color(0xFFE65175)
        }
