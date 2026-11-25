package io.github.roshansoni_io.kitab.feature.collection.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.ui.components.EmptyLibraryState
import io.github.roshansoni_io.kitab.core.ui.components.EmptyRecentState
import io.github.roshansoni_io.kitab.core.ui.components.LoadingState
import io.github.roshansoni_io.kitab.core.ui.components.SearchEmptyState
import io.github.roshansoni_io.kitab.feature.collection.BookCollectionSource
import io.github.roshansoni_io.kitab.feature.collection.BookCollectionUiState

@Composable
internal fun BookCollectionContent(
    uiState: BookCollectionUiState,
    source: BookCollectionSource,
    contentPadding: PaddingValues,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    onShowDetails: ((Book) -> Unit)? = null,
    onRequestRename: ((Book) -> Unit)? = null,
    onResetProgress: ((Book) -> Unit)? = null,
    onMarkCompleted: ((Book) -> Unit)? = null,
    onRequestDeleteFile: ((Book) -> Unit)? = null,
) {
    when {
        uiState.isLoading -> {
            LoadingState(loadingMessage(source), contentPadding)
        }

        uiState.books.isEmpty() && uiState.searchQuery.isNotBlank() -> {
            SearchEmptyState(uiState.searchQuery, contentPadding)
        }

        uiState.books.isEmpty() -> {
            when (source) {
                BookCollectionSource.Recent -> EmptyRecentState(contentPadding)
                BookCollectionSource.Library -> EmptyLibraryState(contentPadding)
            }
        }

        source == BookCollectionSource.Recent && !uiState.isListView -> {
            BookCarousel(
                uiState = uiState,
                contentPadding = contentPadding,
                onOpenBook = onOpenBook,
                onCoverRequested = onCoverRequested,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
                onShowDetails = onShowDetails,
                onRequestRename = onRequestRename,
                onResetProgress = onResetProgress,
                onMarkCompleted = onMarkCompleted,
                onRequestDeleteFile = onRequestDeleteFile,
            )
        }

        uiState.isListView -> {
            BookList(
                uiState = uiState,
                contentPadding = contentPadding,
                onOpenBook = onOpenBook,
                onCoverRequested = onCoverRequested,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
                onShowDetails = onShowDetails,
                onRequestRename = onRequestRename,
                onResetProgress = onResetProgress,
                onMarkCompleted = onMarkCompleted,
                onRequestDeleteFile = onRequestDeleteFile,
            )
        }

        else -> {
            BookGrid(
                uiState = uiState,
                contentPadding = contentPadding,
                onOpenBook = onOpenBook,
                onCoverRequested = onCoverRequested,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
                onShowDetails = onShowDetails,
                onRequestRename = onRequestRename,
                onResetProgress = onResetProgress,
                onMarkCompleted = onMarkCompleted,
                onRequestDeleteFile = onRequestDeleteFile,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookCarousel(
    uiState: BookCollectionUiState,
    contentPadding: PaddingValues,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    onShowDetails: ((Book) -> Unit)? = null,
    onRequestRename: ((Book) -> Unit)? = null,
    onResetProgress: ((Book) -> Unit)? = null,
    onMarkCompleted: ((Book) -> Unit)? = null,
    onRequestDeleteFile: ((Book) -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding() + 20.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
    ) {
        HorizontalUncontainedCarousel(
            state = rememberCarouselState { uiState.books.size },
            itemWidth = 160.dp,
            itemSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(230.dp),
        ) { index ->
            val book = uiState.books[index]
            BookCoverCard(
                book = book,
                cover = uiState.covers[book.uri],
                onCoverRequested = onCoverRequested,
                onOpenBook = onOpenBook,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
                onShowDetails = onShowDetails,
                onRequestRename = onRequestRename,
                onResetProgress = onResetProgress,
                onMarkCompleted = onMarkCompleted,
                onRequestDeleteFile = onRequestDeleteFile,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun BookGrid(
    uiState: BookCollectionUiState,
    contentPadding: PaddingValues,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    onShowDetails: ((Book) -> Unit)? = null,
    onRequestRename: ((Book) -> Unit)? = null,
    onResetProgress: ((Book) -> Unit)? = null,
    onMarkCompleted: ((Book) -> Unit)? = null,
    onRequestDeleteFile: ((Book) -> Unit)? = null,
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 120.dp,
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(uiState.books, key = Book::uri) { book ->
            BookCoverCard(
                book = book,
                cover = uiState.covers[book.uri],
                onCoverRequested = onCoverRequested,
                onOpenBook = onOpenBook,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
                onShowDetails = onShowDetails,
                onRequestRename = onRequestRename,
                onResetProgress = onResetProgress,
                onMarkCompleted = onMarkCompleted,
                onRequestDeleteFile = onRequestDeleteFile,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(.72f),
            )
        }
    }
}

@Composable
private fun BookList(
    uiState: BookCollectionUiState,
    contentPadding: PaddingValues,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    onShowDetails: ((Book) -> Unit)? = null,
    onRequestRename: ((Book) -> Unit)? = null,
    onResetProgress: ((Book) -> Unit)? = null,
    onMarkCompleted: ((Book) -> Unit)? = null,
    onRequestDeleteFile: ((Book) -> Unit)? = null,
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 120.dp,
                start = contentPadding.calculateStartPadding(layoutDirection) + 20.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 20.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(uiState.books, key = Book::uri) { book ->
            BookListRow(
                book = book,
                cover = uiState.covers[book.uri],
                onCoverRequested = onCoverRequested,
                onOpenBook = onOpenBook,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
                onShowDetails = onShowDetails,
                onRequestRename = onRequestRename,
                onResetProgress = onResetProgress,
                onMarkCompleted = onMarkCompleted,
                onRequestDeleteFile = onRequestDeleteFile,
            )
        }
    }
}

private fun loadingMessage(source: BookCollectionSource): String =
    when (source) {
        BookCollectionSource.Recent -> "Loading recent books…"
        BookCollectionSource.Library -> "Loading your library…"
    }
