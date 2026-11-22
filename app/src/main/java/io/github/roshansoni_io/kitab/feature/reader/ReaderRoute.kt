package io.github.roshansoni_io.kitab.feature.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.roshansoni_io.kitab.core.data.repository.BookRepository
import io.github.roshansoni_io.kitab.core.data.repository.SettingsRepository

@Composable
internal fun ReaderRoute(
    bookUri: String,
    bookRepository: BookRepository,
    settingsRepository: SettingsRepository,
    onProgressChanged: (String, Int, Int) -> Unit,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    val books by bookRepository.books.collectAsStateWithLifecycle()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val book = books.firstOrNull { it.uri == bookUri }

    if (book == null) {
        ReaderScaffold(
            bookTitle = "Book unavailable",
            onBack = onBack,
            chromeVisible = true,
            bottomBar = {},
        ) {
            Box(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "This book is no longer available.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
        return
    }

    ReaderScreen(
        book = book,
        resumePage =
            if (settings.resumeBehavior ==
                io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior.Resume
            ) {
                book.lastPage
            } else {
                0
            },
        settings = settings,
        onProgress = { page, pageCount -> onProgressChanged(book.uri, page, pageCount) },
        onBack = onBack,
        onOpenExternalLink = onOpenExternalLink,
    )
}
