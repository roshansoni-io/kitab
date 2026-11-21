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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import androidx.compose.ui.platform.LocalView
import io.github.roshansoni_io.kitab.feature.reader.document.DocumentOpenException
import io.github.roshansoni_io.kitab.feature.reader.document.ReaderBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    resumePage: Int,
    settings: AppSettings,
    onProgress: (page: Int, pageCount: Int) -> Unit,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    val view = LocalView.current
    DisposableEffect(view, settings.keepScreenAwake) {
        view.keepScreenOn = settings.keepScreenAwake
        onDispose { view.keepScreenOn = false }
    }
    val openedBookState = remember(book.cachedPath) { mutableStateOf<Result<ReaderBook>?>(null) }

    LaunchedEffect(book.cachedPath) {
        openedBookState.value =
            runCatching {
                withContext(Dispatchers.IO) {
                    ReaderBook.open(File(book.cachedPath))
                }
            }
    }

    DisposableEffect(book.cachedPath) {
        onDispose {
            openedBookState.value?.getOrNull()?.close()
            openedBookState.value = null
        }
    }

    val openedBook = openedBookState.value
    when (openedBook) {
        null -> {
            ReaderLoadingScreen(book.title, onBack)
        }

        else -> {
            openedBook.fold(
                onSuccess = { readerBook ->
                    ReaderContent(
                        readerBook = readerBook,
                        bookTitle = book.title,
                        resumePage = resumePage,
                        settings = settings,
                        onProgress = onProgress,
                        onBack = onBack,
                        onOpenExternalLink = onOpenExternalLink,
                    )
                },
                onFailure = { failure ->
                    ReaderErrorScreen(
                        bookTitle = book.title,
                        failure = failure,
                        onBack = onBack,
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReaderLoadingScreen(
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
private fun ReaderErrorScreen(
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

private fun failureMessage(failure: Throwable): String =
    when (failure) {
        is DocumentOpenException.PasswordRequired -> "Password-protected books are not supported yet."
        else -> "This book could not be opened."
    }
