package io.github.roshansoni_io.kitab.feature.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import io.github.roshansoni_io.kitab.R

@Composable
internal fun LibraryViewMenuItems(
    isListView: Boolean,
    onViewSelected: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
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
}
