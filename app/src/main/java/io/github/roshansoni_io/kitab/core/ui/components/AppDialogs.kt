package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
internal fun StorageAccessDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Not now") }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = "Allow access") }
        },
        title = { Text(text = "Storage access needed") },
        text = { Text(text = message) },
    )
}
