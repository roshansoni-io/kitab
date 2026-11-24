package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.platform.shareBookFile

@Composable
internal fun BookMenuButton(
    isExpanded: Boolean,
    book: Book,
    onExpandedChange: (Boolean) -> Unit,
    onToggleStar: (Book) -> Unit,
    onAddToLibrary: (Book) -> Unit,
    onRequestRemove: (Book) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onShowDetails: ((Book) -> Unit)? = null,
    onRequestRename: ((Book) -> Unit)? = null,
    onResetProgress: ((Book) -> Unit)? = null,
    onMarkCompleted: ((Book) -> Unit)? = null,
    onRequestDeleteFile: ((Book) -> Unit)? = null,
) {
    val context = LocalContext.current
    val isCompleted =
        (book.pageCount > 0 && book.lastPage >= book.pageCount - 1) ||
            (book.epubProgress != null && book.epubProgress >= 0.99)
    val hasProgress = book.lastPage > 0 || (book.epubProgress != null && book.epubProgress > 0.0)

    Box(modifier = modifier) {
        if (isCompact) {
            IconButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.size(32.dp),
                colors =
                    IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                    ),
            ) {
                Canvas(
                    modifier =
                        Modifier
                            .size(20.dp)
                            .semantics { contentDescription = "Book options" },
                ) {
                    val radius = 1.75.dp.toPx()
                    val spacing = 5.dp.toPx()
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val yPositions = floatArrayOf(centerY - spacing, centerY, centerY + spacing)

                    for (y in yPositions) {
                        // Drop shadow (offset downwards) for elevation depth against light backgrounds
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.5f),
                            radius = radius + 0.8.dp.toPx(),
                            center = Offset(centerX, y + 1.dp.toPx()),
                        )
                        // Concentric dark halo / outline for 360-degree contrast against pure white
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.4f),
                            radius = radius + 0.5.dp.toPx(),
                            center = Offset(centerX, y),
                        )
                        // Crisp white foreground dot
                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = Offset(centerX, y),
                        )
                    }
                }
            }
        } else {
            IconButton(
                onClick = { onExpandedChange(true) },
                colors =
                    IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Book options",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        DropdownMenu(expanded = isExpanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(
                text = { Text(text = if (book.starred) "Remove star" else "Star book") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_star),
                        contentDescription = null,
                        tint =
                            if (book.starred) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    onToggleStar(book)
                },
            )
            if (onMarkCompleted != null && !isCompleted) {
                DropdownMenuItem(
                    text = { Text(text = "Mark as read") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onExpandedChange(false)
                        onMarkCompleted(book)
                    },
                )
            }
            if (onResetProgress != null && hasProgress) {
                DropdownMenuItem(
                    text = { Text(text = "Reset reading progress") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_clock),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onExpandedChange(false)
                        onResetProgress(book)
                    },
                )
            }
            if (onRequestRename != null) {
                DropdownMenuItem(
                    text = { Text(text = "Rename book") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onExpandedChange(false)
                        onRequestRename(book)
                    },
                )
            }
            if (onShowDetails != null) {
                DropdownMenuItem(
                    text = { Text(text = "Book details") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onExpandedChange(false)
                        onShowDetails(book)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = if (book.inLibrary) "Remove from library" else "Add to library") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_library_books),
                        contentDescription = null,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    if (book.inLibrary) onRequestRemove(book) else onAddToLibrary(book)
                },
            )
            DropdownMenuItem(
                text = { Text("Send file") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onExpandedChange(false)
                    shareBookFile(context, book.cachedPath, book.title)
                },
            )
            if (onRequestDeleteFile != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(text = "Delete from device") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                        )
                    },
                    colors =
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        ),
                    onClick = {
                        onExpandedChange(false)
                        onRequestDeleteFile(book)
                    },
                )
            }
        }
    }
}
