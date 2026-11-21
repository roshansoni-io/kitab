package io.github.roshansoni_io.kitab.feature.recent

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.ui.components.AddBookFab
import io.github.roshansoni_io.kitab.core.ui.components.BookHeader
import io.github.roshansoni_io.kitab.core.ui.components.RemoveBookDialog
import io.github.roshansoni_io.kitab.feature.recent.components.RecentContent

@Composable
internal fun RecentScreen(
    uiState: RecentUiState,
    onFilterChanged: (BookFilter) -> Unit,
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
        RecentContent(
            uiState = uiState,
            contentPadding = contentPadding,
            onFilterSelected = onFilterChanged,
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
