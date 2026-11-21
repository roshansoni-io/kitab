package io.github.roshansoni_io.kitab.feature.library.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.R

@Composable
internal fun LibraryViewToggle(
    isListView: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        ViewToggleButton(
            icon = R.drawable.ic_grid,
            contentDescription = "Grid view",
            selected = !isListView,
            index = 0,
            onClick = { onToggle(false) },
        )
        ViewToggleButton(
            icon = R.drawable.ic_list,
            contentDescription = "List view",
            selected = isListView,
            index = 1,
            onClick = { onToggle(true) },
        )
    }
}

@Composable
private fun SingleChoiceSegmentedButtonRowScope.ViewToggleButton(
    icon: Int,
    contentDescription: String,
    selected: Boolean,
    index: Int,
    onClick: () -> Unit,
) {
    SegmentedButton(
        selected = selected,
        onClick = onClick,
        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
        icon = {},
        label = {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
            )
        },
        colors =
            SegmentedButtonDefaults.colors(
                activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                activeContentColor = MaterialTheme.colorScheme.onSurface,
                inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}
