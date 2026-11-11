package io.github.roshansoni_io.kitab.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The reader bottom bar: page navigation buttons, a drag-tracking page slider,
 * and the page counter.
 */
@Composable
internal fun ReaderBottomBar(
    pagerState: PagerState,
    pageCount: Int,
    onDragPage: (Int) -> Unit = {},
    onReleasePage: (Int) -> Unit = {},
) {
    if (pageCount < 2) return

    val scope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.surface

    // Text and Icon colors
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                }
            },
            enabled = pagerState.currentPage > 0,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Page",
                tint = if (pagerState.currentPage > 0) contentColor else contentColor.copy(alpha = 0.3f),
            )
        }

        // While dragging, hold the thumb under the finger locally; writing the
        // value to pagerState.currentPage on every change would snap it between
        // settled pages and spawn a coroutine per drag tick.
        var isDraggingSlider by remember { mutableStateOf(false) }
        var sliderDragValue by remember { mutableFloatStateOf(0f) }
        ReaderPageSlider(
            value = if (isDraggingSlider) sliderDragValue else pagerState.currentPage.toFloat(),
            pageCount = pageCount,
            onValueChanged = { value ->
                isDraggingSlider = true
                sliderDragValue = value
                onDragPage(value.roundToInt().coerceIn(0, pageCount - 1))
            },
            onValueChangeFinished = {
                isDraggingSlider = false
                onReleasePage(sliderDragValue.roundToInt().coerceIn(0, pageCount - 1))
            },
            modifier = Modifier.weight(1f),
        )

        // Live while dragging the slider (the pager itself only settles on release)
        // and during interactive swipes (fraction crosses ±0.5 halfway through).
        val displayedPage =
            if (isDraggingSlider) {
                sliderDragValue.roundToInt()
            } else {
                pagerState.currentPage + pagerState.currentPageOffsetFraction.roundToInt()
            }
        Text(
            text = String.format(Locale.US, "%d/%d", displayedPage.coerceIn(0, pageCount - 1) + 1, pageCount),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        IconButton(
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(pageCount - 1))
                }
            },
            enabled = pagerState.currentPage < pageCount - 1,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Page",
                tint = if (pagerState.currentPage < pageCount - 1) contentColor else contentColor.copy(alpha = 0.3f),
            )
        }
    }
}
