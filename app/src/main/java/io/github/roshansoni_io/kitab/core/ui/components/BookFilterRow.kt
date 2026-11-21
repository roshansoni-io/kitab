package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.BookFilter

@Composable
internal fun BookFilterRow(
    selectedFilter: BookFilter,
    onSelectFilter: (BookFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp),
    ) {
        items(BookFilter.entries) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onSelectFilter(filter) },
                label = { Text(text = filter.label) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(filter.icon),
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }
    }
}

private val BookFilter.label: String
    get() =
        when (this) {
            BookFilter.All -> "All Books"
            BookFilter.Recent -> "Recent"
            BookFilter.Starred -> "Starred"
            BookFilter.Pdf -> "PDF"
        }

private val BookFilter.icon: Int
    get() =
        when (this) {
            BookFilter.All -> R.drawable.ic_kitab_logo
            BookFilter.Recent -> R.drawable.ic_clock
            BookFilter.Starred -> R.drawable.ic_star
            BookFilter.Pdf -> R.drawable.ic_pdf
        }
