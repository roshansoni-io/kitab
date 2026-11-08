package io.github.roshansoni_io.kitab.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScaffold(
    bookTitle: String,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = bookTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { innerPadding ->
        content(innerPadding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderBottomBar(
    pagerState: PagerState,
    pageCount: Int,
) {
    if (pageCount < 2) return
    var draggingValue by remember { mutableStateOf<Float?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    val currentPage = pagerState.currentPage
    val displayValue = draggingValue ?: currentPage.toFloat()

    // Colors matching your design
    val activeTrackColor = Color(0xFFFFCC80) // Light orange
    val inactiveTrackColor = Color(0xFF4E342E) // Dark brown
    val thumbLineColor = Color(0xFFFFB74D) // Orange line
    val haloColor = Color.Black.copy(alpha = 0.5f) // Dark semi-transparent halo

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = ScreenPadding.medium, vertical = ScreenPadding.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScreenPadding.medium),
    ) {
        val displayedPage = displayValue.roundToInt().coerceIn(0, pageCount - 1)
        Text(
            text = String.format(Locale.US, "%d / %d", displayedPage + 1, pageCount),
            style = MaterialTheme.typography.labelLarge,
        )

        Slider(
            value = displayValue,
            onValueChange = { value ->
                isDragging = true
                draggingValue = value
            },
            onValueChangeFinished = { isDragging = false },
            valueRange = 0f..(pageCount - 1).toFloat(),
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
                            .height(24.dp) // Now correctly resolved
                            .clip(RoundedCornerShape(50)),
                ) {
                    // Background (unfilled) track
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(inactiveTrackColor),
                    )

                    // Active (filled) track
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(activeTrackColor),
                    )

                    // Tiny white dot on the right
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp)
                                .size(4.dp)
                                .background(Color.White, CircleShape),
                    )
                }
            },
            thumb = {
                Box(
                    modifier = Modifier.wrapContentSize(unbounded = true),
                    contentAlignment = Alignment.Center,
                ) {
                    // Dark background halo
                    Box(
                        modifier =
                            Modifier
                                .size(48.dp)
                                .background(haloColor, CircleShape),
                    )

                    // Vertical line handle
                    Box(
                        modifier =
                            Modifier
                                .width(4.dp) // Now correctly resolved
                                .height(36.dp) // Now correctly resolved
                                .background(thumbLineColor, RoundedCornerShape(2.dp)),
                    )

                    // Top Number Bubble
                    Box(
                        modifier =
                            Modifier
                                .offset(y = (-38).dp)
                                .size(32.dp)
                                .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (displayValue.roundToInt() + 1).toString(),
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
        )
    }

    LaunchedEffect(draggingValue, isDragging) {
        val value = draggingValue
        if (value != null) {
            val targetPage = value.roundToInt().coerceIn(0, pageCount - 1)
            if (targetPage != pagerState.currentPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
        if (!isDragging) draggingValue = null
    }
}

internal object ScreenPadding {
    val medium = 24.dp
    val small = 8.dp
}
