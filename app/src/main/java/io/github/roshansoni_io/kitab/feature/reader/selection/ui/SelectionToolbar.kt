package io.github.roshansoni_io.kitab.feature.reader.selection.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.R

/** Floating action bar shown over an active text selection. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectionToolbar(
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        content = {
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
        },
    )
}
