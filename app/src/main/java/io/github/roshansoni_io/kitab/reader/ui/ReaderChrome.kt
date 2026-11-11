package io.github.roshansoni_io.kitab.reader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScaffold(
    bookTitle: String,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit,
    chromeVisible: Boolean,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopAppBar(
                title = { Text(text = bookTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            bottomBar()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderBottomBar(
    pagerState: PagerState,
    pageCount: Int,
) {
    if (pageCount < 2) return

    val scope = rememberCoroutineScope()

    // Using MaterialTheme surface color for the background
    // If you want it to match the peach image exactly, replace with Color(0xFFF8DEC5)
    val backgroundColor = MaterialTheme.colorScheme.surface

    // Material Theme Colors for the Slider
    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant

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
        // --- PREVIOUS PAGE BUTTON ---
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

        // --- CUSTOM SLIDER ---
        // While dragging, hold the thumb under the finger locally; committing to
        // pagerState.currentPage on every change would snap it between settled
        // pages and spawn a coroutine per drag tick.
        var isDraggingSlider by remember { mutableStateOf(false) }
        var sliderDragValue by remember { mutableFloatStateOf(0f) }
        Slider(
            value = if (isDraggingSlider) sliderDragValue else pagerState.currentPage.toFloat(),
            onValueChange = { value ->
                isDraggingSlider = true
                sliderDragValue = value
            },
            onValueChangeFinished = {
                isDraggingSlider = false
                scope.launch {
                    pagerState.scrollToPage(sliderDragValue.roundToInt().coerceIn(0, pageCount - 1))
                }
            },
            valueRange = 0f..(pageCount - 1).toFloat(),
            modifier = Modifier.weight(1f),
            // Custom Track
            track = { sliderState ->
                val fraction =
                    if (pageCount > 1) {
                        sliderState.value / (pageCount - 1).toFloat()
                    } else {
                        0f
                    }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp) // Thin track
                            .clip(RoundedCornerShape(2.dp))
                            .background(inactiveTrackColor),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(activeTrackColor),
                    )
                }
            },
            // Custom Thumb
            thumb = {
                Box(
                    modifier =
                        Modifier
                            .size(24.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape)
                            .background(activeTrackColor, CircleShape)
                            .border(width = 2.dp, color = MaterialTheme.colorScheme.surface, shape = CircleShape),
                )
            },
        )

        // --- PAGE COUNTER TEXT ---
        Text(
            text = String.format(Locale.US, "%d/%d", pagerState.currentPage + 1, pageCount),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        // --- NEXT PAGE BUTTON ---
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

internal object ScreenPadding {
    val medium = 24.dp
    val small = 8.dp
}
