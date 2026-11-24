package io.github.roshansoni_io.kitab.feature.collection.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.BookSortOrder

@Composable
internal fun CollectionViewMenuItems(
    isListView: Boolean,
    onViewSelected: (Boolean) -> Unit,
    sortOrder: BookSortOrder,
    onSortSelected: (BookSortOrder) -> Unit,
    onDismissRequest: () -> Unit,
    onClearHistoryRequested: (() -> Unit)? = null,
) {
    DropdownMenuItem(
        text = { Text("Grid view") },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_grid), contentDescription = null)
        },
        trailingIcon = {
            if (!isListView) Icon(imageVector = Icons.Filled.Check, contentDescription = "Current view")
        },
        onClick = {
            onDismissRequest()
            onViewSelected(false)
        },
    )
    DropdownMenuItem(
        text = { Text("List view") },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_list), contentDescription = null)
        },
        trailingIcon = {
            if (isListView) Icon(imageVector = Icons.Filled.Check, contentDescription = "Current view")
        },
        onClick = {
            onDismissRequest()
            onViewSelected(true)
        },
    )
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text("Recently opened") },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_clock), contentDescription = null)
        },
        trailingIcon = {
            if (sortOrder == BookSortOrder.LastOpened) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Active sort")
            }
        },
        onClick = {
            onDismissRequest()
            onSortSelected(BookSortOrder.LastOpened)
        },
    )
    DropdownMenuItem(
        text = { Text("Title (A to Z)") },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_book), contentDescription = null)
        },
        trailingIcon = {
            if (sortOrder == BookSortOrder.TitleAsc) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Active sort")
            }
        },
        onClick = {
            onDismissRequest()
            onSortSelected(BookSortOrder.TitleAsc)
        },
    )
    DropdownMenuItem(
        text = { Text("Title (Z to A)") },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_book), contentDescription = null)
        },
        trailingIcon = {
            if (sortOrder == BookSortOrder.TitleDesc) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Active sort")
            }
        },
        onClick = {
            onDismissRequest()
            onSortSelected(BookSortOrder.TitleDesc)
        },
    )
    DropdownMenuItem(
        text = { Text("Reading progress") },
        leadingIcon = {
            Icon(painter = painterResource(R.drawable.ic_tune), contentDescription = null)
        },
        trailingIcon = {
            if (sortOrder == BookSortOrder.Progress) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Active sort")
            }
        },
        onClick = {
            onDismissRequest()
            onSortSelected(BookSortOrder.Progress)
        },
    )
    if (onClearHistoryRequested != null) {
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Clear reading history") },
            leadingIcon = {
                Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null)
            },
            colors =
                MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
            onClick = {
                onDismissRequest()
                onClearHistoryRequested()
            },
        )
    }
}
