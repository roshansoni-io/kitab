package io.github.roshansoni_io.kitab.feature.reader.epub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.ui.theme.isDarkTheme
import io.github.roshansoni_io.kitab.feature.reader.ReaderErrorScreen
import io.github.roshansoni_io.kitab.feature.reader.ReaderLoadingScreen
import kotlinx.coroutines.CancellationException

@Composable
internal fun EpubReaderScreen(
    book: Book,
    initialLocatorJson: String?,
    settings: AppSettings,
    onProgress: (locatorJson: String, progression: Double) -> Unit,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = isDarkTheme(settings.readerTheme)
    val preferences = remember(settings, darkTheme) { settings.toEpubPreferences(darkTheme) }
    val path = book.cachedPath
    // Reuses the session surviving a configuration change; a fresh process opens it below.
    var sessionState by remember(path) {
        mutableStateOf(EpubSessionRegistry.current(path)?.let { Result.success(it) })
    }

    LaunchedEffect(path) {
        if (sessionState == null) {
            sessionState =
                try {
                    Result.success(
                        openEpubSession(
                            context = context,
                            path = path,
                            preferences = preferences,
                            initialLocator = parseLocator(initialLocatorJson),
                        ).also { EpubSessionRegistry.install(it) },
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    Result.failure(error)
                }
        }
    }

    val openedSession = sessionState
    when {
        openedSession == null -> {
            ReaderLoadingScreen(book.title, onBack)
        }

        openedSession.isSuccess -> {
            EpubReaderContent(
                session = openedSession.getOrThrow(),
                bookTitle = book.title,
                preferences = preferences,
                settings = settings,
                onProgress = onProgress,
                onBack = onBack,
                onOpenExternalLink = onOpenExternalLink,
            )
        }

        else -> {
            ReaderErrorScreen(
                bookTitle = book.title,
                failure = openedSession.exceptionOrNull() ?: EpubOpenException(path, "Unknown error"),
                onBack = onBack,
            )
        }
    }
}
