package io.github.roshansoni_io.kitab.feature.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.repository.BookRepository
import io.github.roshansoni_io.kitab.core.data.repository.SettingsRepository
import io.github.roshansoni_io.kitab.core.ui.theme.KitabTheme
import io.github.roshansoni_io.kitab.core.ui.theme.isDarkTheme

@Composable
internal fun ReaderRoute(
    bookUri: String,
    bookRepository: BookRepository,
    settingsRepository: SettingsRepository,
    onProgressChanged: (String, Int, Int) -> Unit,
    onEpubProgressChanged: (String, String, Double) -> Unit,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    val books by bookRepository.books.collectAsStateWithLifecycle()
    val settings by settingsRepository.settings.collectAsStateWithLifecycle()
    val book = books.firstOrNull { it.uri == bookUri }

    if (book == null) {
        ReaderSystemBarsEffect(
            readerDarkTheme = isDarkTheme(settings.readerTheme),
            appDarkTheme = isDarkTheme(settings.themeMode),
        )
        KitabTheme(
            themeMode = settings.readerTheme,
            dynamicColor = settings.dynamicColor,
            customSeedColor = settings.customSeedColor,
        ) {
            ReaderUnavailableScreen(onBack = onBack)
        }
        return
    }

    val resume = settings.resumeBehavior == ResumeBehavior.Resume
    ReaderScreen(
        book = book,
        resumePage = if (resume) book.lastPage else 0,
        resumeEpubLocatorJson = if (resume) book.epubLocator else null,
        settings = settings,
        onProgress = { page, pageCount -> onProgressChanged(book.uri, page, pageCount) },
        onEpubProgress = { locatorJson, progression ->
            onEpubProgressChanged(book.uri, locatorJson, progression)
        },
        onBack = onBack,
        onOpenExternalLink = onOpenExternalLink,
    )
}
