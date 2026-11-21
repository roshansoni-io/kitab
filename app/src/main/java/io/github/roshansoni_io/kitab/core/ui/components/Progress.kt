package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.roshansoni_io.kitab.core.data.model.Book

@Composable
internal fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}

@Composable
internal fun CompactProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier,
    ) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            trackColor = Color.Transparent,
        )
    }
}

internal fun readingProgress(book: Book): Float =
    if (book.pageCount <= 0) {
        0f
    } else {
        (book.lastPage.toFloat() / book.pageCount).coerceIn(0f, 1f)
    }

internal fun progressLabel(book: Book): String =
    if (book.pageCount <= 0) {
        "Ready to read"
    } else {
        "Page ${book.lastPage + 1} / ${book.pageCount} · ${(readingProgress(book) * 100).toInt()}%"
    }
