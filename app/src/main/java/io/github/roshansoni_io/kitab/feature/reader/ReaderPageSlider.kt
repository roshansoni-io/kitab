package io.github.roshansoni_io.kitab.feature.reader

import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

/**
 * The page-position slider. Presentational only — [value] and the change
 * callbacks are owned by the caller so the rest of the bar can show the dragged
 * page live.
 */
@Composable
internal fun ReaderPageSlider(
    value: Float,
    pageCount: Int,
    onValueChanged: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderState =
        rememberSliderState(
            value = value,
            0,
            0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
        )
    LaunchedEffect(value) {
        sliderState.value = value
    }
    Slider(
        state = sliderState,
        onValueChange = { newValue ->
            sliderState.value = newValue
            onValueChanged(newValue)
        },
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
    )
}
