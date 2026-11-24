package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.roshansoni_io.kitab.core.data.model.Book

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearWavyProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
}

internal fun readingProgress(book: Book): Float {
    val epubProgress = book.epubProgress
    if (epubProgress != null) {
        return epubProgress.toFloat().coerceIn(0f, 1f)
    }
    if (book.pageCount <= 0) {
        return 0f
    }
    return (book.lastPage.toFloat() / book.pageCount).coerceIn(0f, 1f)
}

internal fun progressLabel(book: Book): String {
    val epubProgress = book.epubProgress
    if (epubProgress != null) {
        return if (epubProgress > 0.0) {
            "${(epubProgress * 100).toInt()}% read"
        } else {
            "Ready to read"
        }
    }
    if (book.pageCount <= 0) {
        return "Ready to read"
    }
    return "Page ${book.lastPage + 1} / ${book.pageCount} · ${(readingProgress(book) * 100).toInt()}%"
}
