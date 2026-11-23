package io.github.roshansoni_io.kitab.feature.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.feature.reader.document.DocumentOpenException
import io.github.roshansoni_io.kitab.feature.reader.epub.EpubReaderScreen

@Composable
fun ReaderScreen(
    book: Book,
    resumePage: Int,
    resumeEpubLocatorJson: String?,
    settings: AppSettings,
    onProgress: (page: Int, pageCount: Int) -> Unit,
    onEpubProgress: (locatorJson: String, progression: Double) -> Unit,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    ReaderKeepScreenOn(settings.keepScreenAwake)
    if (book.isEpub) {
        EpubReaderScreen(
            book = book,
            initialLocatorJson = resumeEpubLocatorJson,
            settings = settings,
            onProgress = onEpubProgress,
            onBack = onBack,
            onOpenExternalLink = onOpenExternalLink,
        )
    } else {
        PdfReaderScreen(
            book = book,
            resumePage = resumePage,
            settings = settings,
            onProgress = onProgress,
            onBack = onBack,
            onOpenExternalLink = onOpenExternalLink,
        )
    }
}

internal val Book.isEpub: Boolean
    get() = cachedPath.endsWith(".epub", ignoreCase = true)

@Composable
private fun ReaderKeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReaderLoadingScreen(
    bookTitle: String,
    onBack: () -> Unit,
) {
    ReaderScaffold(
        bookTitle = bookTitle,
        onBack = onBack,
        chromeVisible = true,
        bottomBar = {},
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LoadingIndicator()
                Text(
                    text = "Opening book\u2026",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderErrorScreen(
    bookTitle: String,
    failure: Throwable,
    onBack: () -> Unit,
) {
    ReaderScaffold(
        bookTitle = bookTitle,
        onBack = onBack,
        chromeVisible = true,
        bottomBar = {},
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = failureMessage(failure),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = ScreenPadding.medium),
            )
        }
    }
}

internal fun failureMessage(failure: Throwable): String =
    when (failure) {
        is DocumentOpenException.PasswordRequired -> "Password-protected books are not supported yet."
        else -> "This book could not be opened."
    }
