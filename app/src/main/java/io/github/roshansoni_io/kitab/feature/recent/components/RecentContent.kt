package io.github.roshansoni_io.kitab.feature.recent.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.ui.components.BookFilterRow
import io.github.roshansoni_io.kitab.core.ui.components.BookMenuButton
import io.github.roshansoni_io.kitab.core.ui.components.CoverImage
import io.github.roshansoni_io.kitab.core.ui.components.EmptyFilterState
import io.github.roshansoni_io.kitab.core.ui.components.LoadingState
import io.github.roshansoni_io.kitab.core.ui.components.ProgressBar
import io.github.roshansoni_io.kitab.core.ui.components.SearchEmptyState
import io.github.roshansoni_io.kitab.core.ui.components.readingProgress
import io.github.roshansoni_io.kitab.feature.recent.RecentUiState

@Composable
internal fun RecentContent(
    uiState: RecentUiState,
    contentPadding: PaddingValues,
    onFilterSelected: (BookFilter) -> Unit,
    onOpenBook: (Book) -> Unit,
    onToggleStar: (Book) -> Unit,
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
                onToggleStar = onToggleStar,
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
    onToggleStar: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                ),
    ) {
        RecentHeader(uiState = uiState, onFilterSelected = onFilterSelected)
        HorizontalUncontainedCarousel(
            state = rememberCarouselState { uiState.books.size },
            itemWidth = 196.dp,
            itemSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 20.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(352.dp)
                    .padding(top = 18.dp),
        ) { index ->
            val book = uiState.books[index]
            RecentBookCard(
                book = book,
                cover = uiState.covers[book.uri],
                onOpenBook = onOpenBook,
                onToggleStar = onToggleStar,
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
        Text(
            text = "Recently Opened",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${uiState.totalBookCount} books opened",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        BookFilterRow(
            selectedFilter = uiState.filter,
            onSelectFilter = onFilterSelected,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentBookCard(
    book: Book,
    cover: ImageBitmap?,
    onOpenBook: (Book) -> Unit,
    onToggleStar: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isOverflowMenuExpanded by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.extraLarge
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
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                CoverImage(
                    title = book.title,
                    cover = cover,
                    modifier = Modifier.fillMaxSize(),
                )
                BookMenuButton(
                    isExpanded = isOverflowMenuExpanded,
                    book = book,
                    onExpandedChange = { isOverflowMenuExpanded = it },
                    onToggleStar = onToggleStar,
                    onRequestRemove = onRequestRemove,
                    isCompact = true,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProgressBar(
                        progress = readingProgress(book),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(readingProgress(book) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
