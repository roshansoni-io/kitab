package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import io.github.roshansoni_io.kitab.selection.TextSelection
import io.github.roshansoni_io.kitab.selection.ui.SelectionOverlay
import kotlin.math.roundToInt

/**
 * The fully rendered page as Compose sees it: the base bitmap plus, when zoomed,
 * a [SharpRegion] overlay re-rendered at display density so the zoomed pixels
 * stay sharp.
 */
internal data class SharpRegion(
    val bitmap: Bitmap,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

/**
 * Draws one page bitmap inside the zoomable transform, layered with the sharp
 * region overlay and the text selection overlay.
 */
@Composable
internal fun ZoomedPageImage(
    bitmap: Bitmap,
    zoomState: ZoomState,
    sharpRegion: SharpRegion?,
    selection: TextSelection?,
    pageSize: RectF?,
    onGestureEnd: () -> Unit,
    onTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Boolean,
    onSelectionDrag: (Offset) -> Unit,
    onSelectionEnd: () -> Unit,
    onHandleHit: (Offset) -> Boolean,
    onHandleDrag: (Offset) -> Unit,
    onHandleEnd: () -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .zoomable(
                        state = zoomState,
                        onTap = onTap,
                        onGestureEnd = onGestureEnd,
                        onLongPress = onLongPress,
                        onSelectionDrag = onSelectionDrag,
                        onSelectionEnd = onSelectionEnd,
                        onHandleHit = onHandleHit,
                        onHandleDrag = onHandleDrag,
                        onHandleEnd = onHandleEnd,
                    ).graphicsLayer {
                        scaleX = zoomState.scale
                        scaleY = zoomState.scale
                        translationX = zoomState.offsetX
                        translationY = zoomState.offsetY
                    },
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF page",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (sharpRegion != null) {
                Image(
                    bitmap = sharpRegion.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier =
                        Modifier
                            .offset {
                                with(density) {
                                    IntOffset(
                                        sharpRegion.left.roundToInt(),
                                        sharpRegion.top.roundToInt(),
                                    )
                                }
                            }.size(
                                width = with(density) { sharpRegion.width.toDp() },
                                height = with(density) { sharpRegion.height.toDp() },
                            ),
                )
            }
            val points = pageSize
            if (selection != null && points != null) {
                SelectionOverlay(
                    pageText = selection.pageText,
                    startIndex = selection.first,
                    endIndex = selection.last,
                    pageSize = points,
                )
            }
        }
    }
}
