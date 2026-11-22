package io.github.roshansoni_io.kitab.feature.recent.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.ui.components.BookFilterRow
import io.github.roshansoni_io.kitab.core.ui.components.BookMenuButton
import io.github.roshansoni_io.kitab.core.ui.components.CoverImage
import io.github.roshansoni_io.kitab.core.ui.components.EmptyFilterState
import io.github.roshansoni_io.kitab.core.ui.components.LoadingState
import io.github.roshansoni_io.kitab.core.ui.components.SearchEmptyState
import io.github.roshansoni_io.kitab.feature.recent.RecentUiState

@Composable
internal fun RecentContent(
    uiState: RecentUiState,
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
            LoadingState("Loading recent books…", contentPadding)
        }

        uiState.books.isEmpty() && uiState.searchQuery.isNotBlank() -> {
            SearchEmptyState(uiState.searchQuery, contentPadding)
        }

        uiState.books.isEmpty() && uiState.totalBookCount == 0 -> {
            EmptyFilterState(BookFilter.Recent, contentPadding)
        }

        uiState.books.isEmpty() -> {
            EmptyFilterState(uiState.filter, contentPadding)
        }

        else -> {
            RecentCarousel(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentCarousel(
    uiState: RecentUiState,
    contentPadding: PaddingValues,
    onFilterSelected: (BookFilter) -> Unit,
    onOpenBook: (Book) -> Unit,
    onCoverRequested: (Book) -> Unit,
    onToggleStar: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
    ) {
        RecentHeader(uiState = uiState, onFilterSelected = onFilterSelected)
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
            RecentBookCard(
                book = book,
                cover = uiState.covers[book.uri],
                onCoverRequested = onCoverRequested,
                onOpenBook = onOpenBook,
                onToggleStar = onToggleStar,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun RecentHeader(
    uiState: RecentUiState,
    onFilterSelected: (BookFilter) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        BookFilterRow(
            selectedFilter = uiState.filter,
            onSelectFilter = onFilterSelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentBookCard(
    book: Book,
    cover: ImageBitmap?,
    onCoverRequested: (Book) -> Unit,
    onOpenBook: (Book) -> Unit,
    onToggleStar: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOverflowMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(book.uri, cover) {
        if (cover == null) onCoverRequested(book)
    }
    val shape = RectangleShape
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            modifier
                .clip(shape)
                .combinedClickable(
                    onClick = { onOpenBook(book) },
                    onLongClick = { onRequestRemove(book) },
                ),
    ) {
        Box(Modifier.fillMaxSize()) {
            CoverImage(
                cover = cover,
                modifier = Modifier.fillMaxSize(),
            )
            BookMenuButton(
                isExpanded = isOverflowMenuExpanded,
                book = book,
                onExpandedChange = { isOverflowMenuExpanded = it },
                onToggleStar = onToggleStar,
                onAddToLibrary = onAddToLibrary,
                onRequestRemove = onRequestRemove,
                isCompact = true,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}
