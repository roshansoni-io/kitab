package io.github.roshansoni_io.kitab.feature.collection.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.ui.components.BookMenuButton
import io.github.roshansoni_io.kitab.core.ui.components.CoverImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookCoverCard(
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
