package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.roshansoni_io.kitab.core.data.model.Book

@Composable
internal fun ImportFailedDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "OK") }
        },
        title = { Text(text = "Something went wrong") },
        text = { Text(text = message) },
    )
}

@Composable
internal fun RemoveBookDialog(
    book: Book,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = "Remove") }
        },
        title = { Text(text = "Remove from library?") },
        text = {
            Text(
                text =
                    "\"${book.title}\" will be removed from your library " +
                        "and its stored PDF will be deleted.",
            )
        },
    )
}
