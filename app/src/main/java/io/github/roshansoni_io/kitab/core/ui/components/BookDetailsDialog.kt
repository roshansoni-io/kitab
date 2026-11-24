package io.github.roshansoni_io.kitab.core.ui.components

import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.Book
import java.io.File
import java.util.Date

@Composable
internal fun BookDetailsDialog(
    book: Book,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val fileSize =
        remember(book.cachedPath) {
            val file = File(book.cachedPath)
            if (file.exists()) Formatter.formatFileSize(context, file.length()) else "Unknown"
        }
    val format =
        remember(book.cachedPath) {
            if (book.cachedPath.endsWith(".epub", ignoreCase = true) || book.epubLocator != null) "EPUB" else "PDF"
        }
    val progress =
        remember(book.lastPage, book.pageCount, book.epubProgress) {
            if (format == "EPUB") {
                val pct = ((book.epubProgress ?: 0.0) * 100).toInt()
                "$pct% read"
            } else if (book.pageCount > 0) {
                val pct = ((book.lastPage + 1).toDouble() / book.pageCount * 100).toInt()
                "Page ${book.lastPage + 1} of ${book.pageCount} ($pct%)"
            } else {
                "Unread"
            }
        }
    val lastRead =
        remember(book.lastOpenedAt) {
            if (book.lastOpenedAt > 0) {
                val date = Date(book.lastOpenedAt)
                val timeFormat = DateFormat.getTimeFormat(context).format(date)
                val dateFormat = DateFormat.getMediumDateFormat(context).format(date)
                "$dateFormat, $timeFormat"
            } else {
                "Never opened"
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
        title = {
            Text(
                text = "Book Details",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailRow(label = "Title", value = book.title)
                DetailRow(label = "Format", value = format)
                DetailRow(label = "Progress", value = progress)
                DetailRow(label = "File size", value = fileSize)
                DetailRow(label = "Last read", value = lastRead)
                DetailRow(label = "Location", value = book.cachedPath)
            }
        },
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
