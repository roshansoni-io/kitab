package io.github.roshansoni_io.kitab.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * The page-position slider: a thin custom track with a round thumb, one stop per
 * page. Presentational only — [value] and the change callbacks are owned by the
 * caller so the rest of the bar can show the dragged page live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderPageSlider(
    value: Float,
    pageCount: Int,
    onValueChanged: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
    Slider(
        value = value,
        onValueChange = onValueChanged,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = 0f..(pageCount - 1).toFloat(),
        modifier = modifier,
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
                        .height(4.dp)
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
}
