package io.github.roshansoni_io.kitab.feature.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.ui.components.BookFilterRow
import io.github.roshansoni_io.kitab.core.ui.components.EmptyFilterState
import io.github.roshansoni_io.kitab.core.ui.components.EmptyLibraryState
import io.github.roshansoni_io.kitab.core.ui.components.LoadingState
import io.github.roshansoni_io.kitab.core.ui.components.SearchEmptyState
import io.github.roshansoni_io.kitab.feature.library.LibraryUiState

@Composable
internal fun LibraryContent(
    uiState: LibraryUiState,
    contentPadding: PaddingValues,
    onFilterSelected: (BookFilter) -> Unit,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onToggleStar: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
) {
    when {
        uiState.isLoading -> {
            LoadingState("Loading your library…", contentPadding)
        }

        uiState.books.isEmpty() && uiState.searchQuery.isNotBlank() -> {
            SearchEmptyState(uiState.searchQuery, contentPadding)
        }

        uiState.books.isEmpty() && uiState.totalBookCount == 0 -> {
            EmptyLibraryState(contentPadding)
        }

        uiState.books.isEmpty() -> {
            EmptyFilterState(uiState.filter, contentPadding)
        }

        uiState.isListView -> {
            LibraryBookList(
                uiState = uiState,
                contentPadding = contentPadding,
                onFilterSelected = onFilterSelected,
                onOpenBook = onOpenBook,
                onCoverRequested = onCoverRequested,
                onToggleStar = onToggleStar,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
            )
        }

        else -> {
            LibraryGrid(
                uiState = uiState,
                contentPadding = contentPadding,
                onFilterSelected = onFilterSelected,
                onOpenBook = onOpenBook,
                onCoverRequested = onCoverRequested,
                onToggleStar = onToggleStar,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
            )
        }
    }
}

@Composable
private fun LibraryGrid(
    uiState: LibraryUiState,
    contentPadding: PaddingValues,
    onFilterSelected: (BookFilter) -> Unit,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onToggleStar: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 120.dp,
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibraryHeaderSection(
                uiState = uiState,
                onFilterSelected = onFilterSelected,
            )
        }
        items(uiState.books, key = Book::uri) { book ->
            LibraryBookCard(
                book = book,
                cover = uiState.covers[book.uri],
                onCoverRequested = onCoverRequested,
                onOpenBook = onOpenBook,
                onToggleStar = onToggleStar,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
            )
        }
    }
}

@Composable
private fun LibraryBookList(
    uiState: LibraryUiState,
    contentPadding: PaddingValues,
    onFilterSelected: (BookFilter) -> Unit,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onToggleStar: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 120.dp,
                start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LibraryHeaderSection(
                uiState = uiState,
                onFilterSelected = onFilterSelected,
            )
        }
        items(uiState.books, key = Book::uri) { book ->
            LibraryBookRow(
                book = book,
                cover = uiState.covers[book.uri],
                onCoverRequested = onCoverRequested,
                onOpenBook = onOpenBook,
                onToggleStar = onToggleStar,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
            )
        }
    }
}

@Composable
private fun LibraryHeaderSection(
    uiState: LibraryUiState,
    onFilterSelected: (BookFilter) -> Unit,
) {
    Column {
        BookFilterRow(
            selectedFilter = uiState.filter,
            onSelectFilter = onFilterSelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
