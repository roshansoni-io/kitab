package io.github.roshansoni_io.kitab.feature.reader

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * The page-position slider. Presentational only — [value] and the change
 * callbacks are owned by the caller so the rest of the bar can show the dragged
 * position live without fighting ongoing user input.
 */
@Composable
internal fun ReaderPageSlider(
    value: Float,
    onValueChanged: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    isInteracting: Boolean = false,
) {
    val clamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val sliderState =
        remember(valueRange) {
            SliderState(
                value = clamped,
                trackRange = valueRange,
            )
        }
    LaunchedEffect(clamped, isInteracting) {
        if (!isInteracting) {
            sliderState.value = clamped
        }
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
