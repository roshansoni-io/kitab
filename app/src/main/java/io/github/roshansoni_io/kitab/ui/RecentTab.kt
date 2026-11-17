package io.github.roshansoni_io.kitab.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.data.Book

@Composable
internal fun LibraryContent(
    isLoading: Boolean,
    books: List<Book>,
    covers: Map<String, ImageBitmap>,
    query: String,
    contentPadding: PaddingValues,
    onOpenBook: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
) {
    when {
        isLoading -> {
            LoadingState(
                message = "Loading your library\u2026",
                contentPadding = contentPadding,
            )
        }

        books.isEmpty() && query.isNotBlank() -> {
            SearchEmptyState(
                query = query,
                contentPadding = contentPadding,
            )
        }

        books.isEmpty() -> {
            EmptyLibraryState(contentPadding = contentPadding)
        }

        else -> {
            LibraryGrid(
                books = books,
                covers = covers,
                contentPadding = contentPadding,
                onOpenBook = onOpenBook,
                onRequestRemove = onRequestRemove,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryTopBar(
    title: String,
    isSearchActive: Boolean,
    query: String,
    onSearchActiveChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                SearchField(
                    query = query,
                    onQueryChange = onQueryChange,
                )
            } else {
                Text(text = title)
            }
        },
        navigationIcon = {
            if (isSearchActive) {
                IconButton(
                    onClick = {
                        onSearchActiveChange(false)
                        onQueryChange("")
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close search",
                    )
                }
            }
        },
        actions = {
            if (isSearchActive) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                }
            } else {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                }
            }
        },
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = "Search books") },
        singleLine = true,
        keyboardOptions =
            androidx.compose.foundation.text
                .KeyboardOptions(imeAction = ImeAction.Search),
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
    )
}

@Composable
private fun LibraryGrid(
    books: List<Book>,
    covers: Map<String, ImageBitmap>,
    contentPadding: PaddingValues,
    onOpenBook: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
                start = contentPadding.calculateStartPadding(layoutDirection) + 8.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = books, key = { it.uri }) { book ->
            BookCoverTile(
                book = book,
                cover = covers[book.uri],
                onOpenBook = onOpenBook,
                onRequestRemove = onRequestRemove,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SearchEmptyState(
    query: String,
    contentPadding: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "No results", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "No books match \"${query.trim()}\"",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
