package io.github.roshansoni_io.kitab.feature.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.ui.components.AddBookFab
import io.github.roshansoni_io.kitab.core.ui.components.BookHeader
import io.github.roshansoni_io.kitab.core.ui.components.RemoveBookDialog
import io.github.roshansoni_io.kitab.feature.library.components.LibraryContent

@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    onFilterChanged: (BookFilter) -> Unit,
    onListViewChanged: (Boolean) -> Unit,
    onSearchVisibilityChanged: (Boolean) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenBook: (Book) -> Unit,
    onAddBook: () -> Unit,
    onStarRequested: (Book) -> Unit,
    onRemoveRequested: (Book) -> Unit,
    onRemovalConfirmed: () -> Unit,
    onRemovalDismissed: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BookHeader(
                isSearchActive = uiState.isSearchActive,
                query = uiState.searchQuery,
                onSearchActiveChange = onSearchVisibilityChanged,
                onQueryChange = onSearchQueryChanged,
            )
        },
        floatingActionButton = { AddBookFab(onClick = onAddBook) },
    ) { contentPadding ->
        LibraryContent(
            uiState = uiState,
            contentPadding = contentPadding,
            onFilterSelected = onFilterChanged,
            onListViewChanged = onListViewChanged,
            onOpenBook = onOpenBook,
            onToggleStar = onStarRequested,
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
