package io.github.roshansoni_io.kitab.feature.reader

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
import androidx.compose.material3.Surface
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
    // While dragging, hold the thumb under the finger locally; writing the
    // value to pagerState.currentPage on every change would snap it between
    // settled pages and spawn a coroutine per drag tick.
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragValue by remember { mutableFloatStateOf(0f) }

    val displayedPage =
        if (isDraggingSlider) {
            sliderDragValue.roundToInt()
        } else {
            pagerState.currentPage + pagerState.currentPageOffsetFraction.roundToInt()
        }
    val label = String.format(Locale.US, "%d/%d", displayedPage.coerceIn(0, pageCount - 1) + 1, pageCount)

    ReaderBottomBar(
        progress = if (isDraggingSlider) sliderDragValue else pagerState.currentPage.toFloat(),
        label = label,
        onSeek = { value ->
            isDraggingSlider = true
            sliderDragValue = value
            onDragPage(value.roundToInt().coerceIn(0, pageCount - 1))
        },
        onSeekFinished = {
            isDraggingSlider = false
            onReleasePage(sliderDragValue.roundToInt().coerceIn(0, pageCount - 1))
        },
        onPrevious = {
            scope.launch {
                pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
            }
        },
        onNext = {
            scope.launch {
                pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(pageCount - 1))
            }
        },
        valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
        canGoPrevious = pagerState.currentPage > 0,
        canGoNext = pagerState.currentPage < pageCount - 1,
        isInteracting = isDraggingSlider,
    )
}

@Composable
internal fun ReaderBottomBar(
    progress: Float,
    label: String,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    canGoPrevious: Boolean = true,
    canGoNext: Boolean = true,
    isInteracting: Boolean = false,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onPrevious,
                enabled = canGoPrevious,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Page",
                )
            }

            ReaderPageSlider(
                value = progress,
                onValueChanged = onSeek,
                onValueChangeFinished = onSeekFinished,
                valueRange = valueRange,
                isInteracting = isInteracting,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            IconButton(
                onClick = onNext,
                enabled = canGoNext,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Page",
                )
            }
        }
    }
}
