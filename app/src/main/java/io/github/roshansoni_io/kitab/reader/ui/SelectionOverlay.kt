package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

/**
 * Draws selection highlights over the page image. Must live inside the same
 * zoom transform as the page so highlights scale and pan together with it.
 */
@Composable
fun SelectionOverlay(
    rects: List<RectF>,
    pageSize: RectF,
    modifier: Modifier = Modifier,
) {
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    Canvas(modifier = modifier.fillMaxSize()) {
        val pageWidth = pageSize.width()
        val pageHeight = pageSize.height()
        if (pageWidth <= 0f || pageHeight <= 0f) return@Canvas
        val fit = minOf(size.width / pageWidth, size.height / pageHeight)
        val offsetX = (size.width - pageWidth * fit) / 2f
        val offsetY = (size.height - pageHeight * fit) / 2f
        rects.forEach { rect ->
            drawRect(
                color = highlight,
                topLeft =
                    Offset(
                        offsetX + (rect.left - pageSize.left) * fit,
                        offsetY + (rect.top - pageSize.top) * fit,
                    ),
                size = Size(rect.width() * fit, rect.height() * fit),
            )
        }
    }
}

@Composable
fun SelectionToolbar(
    onCopy: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onCopy) {
                Text(text = "Copy")
            }
            TextButton(onClick = onClose) {
                Text(text = "Close")
            }
        }
    }
}
