package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.R
import kotlin.math.min

internal enum class SelectionHandle { Start, End }

@Composable
fun SelectionOverlay(
    rectsByLine: List<List<RectF>>,
    pageSize: RectF,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { invisibleToUser() },
    ) {
        val pageWidth = pageSize.width()
        val pageHeight = pageSize.height()
        if (pageWidth <= 0f || pageHeight <= 0f) return@Canvas

        // Aspect-fit the page into the canvas.

        val scale = min(size.width / pageWidth, size.height / pageHeight)
        val offsetX = (size.width - pageWidth * scale) / 2f
        val offsetY = (size.height - pageHeight * scale) / 2f

        val solid = Color(0xFF82C8FF)
        val merged = rectsByLine.flatMap(::mergeRects)

        drawIntoCanvas { canvas ->
            val layerPaint = Paint().apply { alpha = (0.4f * 255).toInt() }
            canvas.nativeCanvas.saveLayer(0f, 0f, size.width, size.height, layerPaint)
            merged.forEach { rect ->
                drawRect(
                    color = solid,
                    topLeft =
                        Offset(
                            offsetX + (rect.left - pageSize.left) * scale,
                            offsetY + (rect.top - pageSize.top) * scale,
                        ),
                    size = Size(rect.width() * scale, rect.height() * scale),
                )
            }
            canvas.nativeCanvas.restore()
        }
    }
}

/** Merges touching/overlapping rects to reduce seam artefacts. */

private fun mergeRects(rects: List<RectF>): List<RectF> {
    if (rects.size < 2) return rects
    val sorted = rects.sortedWith(compareBy({ it.top }, { it.left }))
    val result = mutableListOf<RectF>()
    var current = RectF(sorted[0])
    for (i in 1 until sorted.size) {
        val r = sorted[i]
        val touches =
            r.top <= current.bottom + 1f &&
                r.bottom >= current.top - 1f &&
                r.left <= current.right + 1f &&
                r.right >= current.left - 1f
        if (touches) {
            current.union(r)
        } else {
            result += RectF(current)
            current = RectF(r)
        }
    }
    result += current
    return result
}

@Composable
fun SelectionHandles(
    start: Offset,
    end: Offset,
    modifier: Modifier = Modifier,
) {
    val handlePainter = painterResource(R.drawable.selection_handle)

    Canvas(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { invisibleToUser() },
    ) {
        drawSelectionHandle(handlePainter, start)
        drawSelectionHandle(handlePainter, end)
    }
}

private fun DrawScope.drawSelectionHandle(
    painter: Painter,
    anchor: Offset,
) {
    val width = 40.dp.toPx()
    val height = 60.dp.toPx()
    translate(left = anchor.x - width / 2f, top = anchor.y) {
        with(painter) {
            draw(size = Size(width, height))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectionToolbar(
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            TextButton(onClick = onCopy, contentPadding = contentPadding) {
                Text(text = stringResource(R.string.selection_action_copy))
            }
            TextButton(onClick = onSelectAll, contentPadding = contentPadding) {
                Text(text = stringResource(R.string.selection_action_select_all))
            }
            TextButton(onClick = onShare, contentPadding = contentPadding) {
                Text(text = stringResource(R.string.selection_action_share))
            }
            TextButton(onClick = onClose, contentPadding = contentPadding) {
                Text(text = stringResource(R.string.selection_action_close))
            }
        }
    }
}
