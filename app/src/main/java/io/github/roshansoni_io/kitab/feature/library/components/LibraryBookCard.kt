package io.github.roshansoni_io.kitab.feature.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.ui.components.BookMenuButton
import io.github.roshansoni_io.kitab.core.ui.components.CompactProgressBar
import io.github.roshansoni_io.kitab.core.ui.components.CoverImage
import io.github.roshansoni_io.kitab.core.ui.components.ProgressBar
import io.github.roshansoni_io.kitab.core.ui.components.progressLabel
import io.github.roshansoni_io.kitab.core.ui.components.readingProgress

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryBookCard(
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
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(.72f),
        ) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryBookRow(
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
    val shape = MaterialTheme.shapes.extraLarge
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                    onClick = { onOpenBook(book) },
                    onLongClick = { onRequestRemove(book) },
                ),
    ) {
        ListItem(
            supportingContent = {
                Column {
                    Text(
                        text = progressLabel(book),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ProgressBar(
                        progress = readingProgress(book),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                    )
                }
            },
            leadingContent = {
                CoverImage(
                    cover = cover,
                    modifier =
                        Modifier
                            .width(58.dp)
                            .aspectRatio(.72f),
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BookMenuButton(
                        isExpanded = isOverflowMenuExpanded,
                        book = book,
                        onExpandedChange = { isOverflowMenuExpanded = it },
                        onToggleStar = onToggleStar,
                        onAddToLibrary = onAddToLibrary,
                        onRequestRemove = onRequestRemove,
                    )
                    StarButton(book = book, onToggleStar = onToggleStar)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StarButton(
    book: Book,
    onToggleStar: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    IconToggleButton(
        checked = book.starred,
        onCheckedChange = { onToggleStar(book) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = if (book.starred) "Remove star" else "Star book",
            tint =
                if (book.starred) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.size(22.dp),
        )
    }
}
