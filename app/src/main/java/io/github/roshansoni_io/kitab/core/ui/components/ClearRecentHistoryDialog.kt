package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun ClearRecentHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Clear Reading History?") },
        text = {
            Text(
                text = "Recently opened books will be cleared. Books saved in your library will remain intact.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}
