package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.platform.shareBookFile

@Composable
internal fun BookMenuButton(
    isExpanded: Boolean,
    book: Book,
    onExpandedChange: (Boolean) -> Unit,
    onToggleStar: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        FilledTonalIconButton(
            onClick = { onExpandedChange(true) },
            modifier = if (isCompact) Modifier.size(32.dp) else Modifier,
            colors =
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black,
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Book options",
                modifier = Modifier.size(if (isCompact) 18.dp else 20.dp),
            )
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(
                text = { Text(text = if (book.starred) "Remove star" else "Star book") },
                onClick = {
                    onExpandedChange(false)
                    onToggleStar(book)
                },
            )
            DropdownMenuItem(
                text = { Text(text = if (book.inLibrary) "Remove from library" else "Add to library") },
                onClick = {
                    onExpandedChange(false)
                    if (book.inLibrary) onRequestRemove(book) else onAddToLibrary(book)
                },
            )
            DropdownMenuItem(
                text = { Text("Send file") },
                onClick = {
                    onExpandedChange(false)
                    shareBookFile(context, book.cachedPath, book.title)
                },
            )
        }
    }
}
