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
import io.github.roshansoni_io.kitab.core.data.model.BookSortOrder
import io.github.roshansoni_io.kitab.core.ui.components.BookDetailsDialog
import io.github.roshansoni_io.kitab.core.ui.components.BookHeader
import io.github.roshansoni_io.kitab.core.ui.components.ClearRecentHistoryDialog
import io.github.roshansoni_io.kitab.core.ui.components.DeleteBookFileDialog
import io.github.roshansoni_io.kitab.core.ui.components.RemoveBookDialog
import io.github.roshansoni_io.kitab.core.ui.components.RenameBookDialog
import io.github.roshansoni_io.kitab.feature.collection.components.BookCollectionContent
import io.github.roshansoni_io.kitab.feature.collection.components.CollectionViewMenuItems

@Composable
internal fun BookCollectionScreen(
    uiState: BookCollectionUiState,
    source: BookCollectionSource,
    onListViewChanged: (Boolean) -> Unit,
    onSortOrderChanged: (BookSortOrder) -> Unit,
    onSearchVisibilityChanged: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onAddBook: () -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRemoveRequested: (Book) -> Unit,
    onRemovalConfirmed: () -> Unit,
    onRemovalDismissed: () -> Unit,
    onShowDetails: (Book) -> Unit,
    onDetailsDismissed: () -> Unit,
    onRequestRename: (Book) -> Unit,
    onRenameConfirmed: (String) -> Unit,
    onRenameDismissed: () -> Unit,
    onResetProgress: (Book) -> Unit,
    onMarkCompleted: (Book) -> Unit,
    onClearHistoryRequested: () -> Unit,
    onClearHistoryConfirmed: () -> Unit,
    onClearHistoryDismissed: () -> Unit,
    onRequestDeleteFile: (Book) -> Unit,
    onDeleteFileConfirmed: () -> Unit,
    onDeleteFileDismissed: () -> Unit,
) {
    val overflowMenuContent: (@Composable ColumnScope.(onDismissRequest: () -> Unit) -> Unit)? =
        if (source.supportsListView) {
            { onDismissRequest ->
                CollectionViewMenuItems(
                    isListView = uiState.isListView,
                    onViewSelected = onListViewChanged,
                    sortOrder = uiState.sortOrder,
                    onSortSelected = onSortOrderChanged,
                    onDismissRequest = onDismissRequest,
                    onClearHistoryRequested = if (source == BookCollectionSource.Recent) onClearHistoryRequested else null,
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
            onOpenBook = onOpenBook,
            onCoverRequested = onCoverRequested,
            onAddToLibrary = onAddToLibrary,
            onRequestRemove = onRemoveRequested,
            onShowDetails = onShowDetails,
            onRequestRename = onRequestRename,
            onResetProgress = onResetProgress,
            onMarkCompleted = onMarkCompleted,
            onRequestDeleteFile = onRequestDeleteFile,
        )
    }

    uiState.pendingRemoval?.let { book ->
        RemoveBookDialog(
            book = book,
            onConfirm = onRemovalConfirmed,
            onDismiss = onRemovalDismissed,
        )
    }

    uiState.pendingRename?.let { book ->
        RenameBookDialog(
            book = book,
            onConfirm = onRenameConfirmed,
            onDismiss = onRenameDismissed,
        )
    }

    uiState.pendingDetails?.let { book ->
        BookDetailsDialog(
            book = book,
            onDismiss = onDetailsDismissed,
        )
    }

    if (uiState.pendingClearHistory) {
        ClearRecentHistoryDialog(
            onConfirm = onClearHistoryConfirmed,
            onDismiss = onClearHistoryDismissed,
        )
    }

    uiState.pendingFileDeletion?.let { book ->
        DeleteBookFileDialog(
            book = book,
            onConfirm = onDeleteFileConfirmed,
            onDismiss = onDeleteFileDismissed,
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
