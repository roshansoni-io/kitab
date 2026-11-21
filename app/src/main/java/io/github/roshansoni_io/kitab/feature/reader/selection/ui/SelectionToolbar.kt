package io.github.roshansoni_io.kitab.feature.reader.selection.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.R

/** Floating action bar shown over an active text selection. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectionToolbar(
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = modifier,
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            TextButton(onClick = onCopy, contentPadding = contentPadding) {
                Text(text = stringResource(R.string.selection_action_copy))
            }
            TextButton(onClick = onSelectAll, contentPadding = contentPadding) {
                Text(text = stringResource(R.string.selection_action_select_all))
            }
            TextButton(onClick = onShare, contentPadding = contentPadding) {
                Text(text = stringResource(R.string.selection_action_share))
            }
            TextButton(onClick = onClose, contentPadding = contentPadding) {
                Text(text = stringResource(R.string.selection_action_close))
            }
        }
    }
}
