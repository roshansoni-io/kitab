package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.Book

@Composable
internal fun BookMenuButton(
    isExpanded: Boolean,
    book: Book,
    onExpandedChange: (Boolean) -> Unit,
    onToggleStar: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    Box(modifier = modifier) {
        FilledTonalIconButton(
            onClick = { onExpandedChange(true) },
            modifier = if (isCompact) Modifier.size(32.dp) else Modifier,
            colors =
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
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
                leadingIcon = {
                    Icon(
                        painter = painterResource(if (book.starred) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                        contentDescription = null,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onToggleStar(book)
                },
            )
            DropdownMenuItem(
                text = { Text(text = "Remove from library") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onRequestRemove(book)
                },
            )
        }
    }
}
