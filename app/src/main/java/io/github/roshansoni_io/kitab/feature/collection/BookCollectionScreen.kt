package io.github.roshansoni_io.kitab.feature.collection

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.ui.components.BookHeader
import io.github.roshansoni_io.kitab.core.ui.components.RemoveBookDialog
import io.github.roshansoni_io.kitab.feature.collection.components.BookCollectionContent
import io.github.roshansoni_io.kitab.feature.collection.components.CollectionViewMenuItems

@Composable
internal fun BookCollectionScreen(
    uiState: BookCollectionUiState,
    source: BookCollectionSource,
    onFilterChanged: (BookFilter) -> Unit,
    onListViewChanged: (Boolean) -> Unit,
    onSearchVisibilityChanged: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onAddBook: () -> Unit,
    onStarRequested: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRemoveRequested: (Book) -> Unit,
    onRemovalConfirmed: () -> Unit,
    onRemovalDismissed: () -> Unit,
) {
    val overflowMenuContent: (@Composable ColumnScope.(onDismissRequest: () -> Unit) -> Unit)? =
        if (source.supportsListView) {
            { onDismissRequest ->
                CollectionViewMenuItems(
                    isListView = uiState.isListView,
                    onViewSelected = onListViewChanged,
                    onDismissRequest = onDismissRequest,
                )
            }
        } else {
            null
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BookHeader(
                isSearchActive = uiState.isSearchActive,
                query = uiState.searchQuery,
                onSearchActiveChange = onSearchVisibilityChanged,
                onQueryChange = onSearchQueryChanged,
                overflowMenuContent = overflowMenuContent,
            )
        },
        floatingActionButton = { AddBookFab(onClick = onAddBook) },
    ) { contentPadding ->
        BookCollectionContent(
            uiState = uiState,
            source = source,
            contentPadding = contentPadding,
            onFilterSelected = onFilterChanged,
            onOpenBook = onOpenBook,
            onCoverRequested = onCoverRequested,
            onToggleStar = onStarRequested,
            onAddToLibrary = onAddToLibrary,
            onRequestRemove = onRemoveRequested,
        )
    }

    uiState.pendingRemoval?.let { book ->
        RemoveBookDialog(
            book = book,
            onConfirm = onRemovalConfirmed,
            onDismiss = onRemovalDismissed,
        )
    }
}

@Composable
private fun AddBookFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
        text = { Text(text = "Add Book") },
    )
}
