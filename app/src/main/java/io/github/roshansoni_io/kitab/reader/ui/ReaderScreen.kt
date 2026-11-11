package io.github.roshansoni_io.kitab.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.data.Book
import io.github.roshansoni_io.kitab.reader.DocumentOpenException
import io.github.roshansoni_io.kitab.reader.ReaderBook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
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

@OptIn(ExperimentalMaterial3Api::class)
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
            CircularProgressIndicator()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderContent(
    readerBook: ReaderBook,
    bookTitle: String,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { readerBook.pageCount })
    val zoomedPages = remember { mutableStateMapOf<Int, Boolean>() }
    val selectingPages = remember { mutableStateMapOf<Int, Boolean>() }
    val zoomStates = remember { mutableStateMapOf<Int, ZoomState>() }
    val viewportSize = remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    var pendingLinkUri by remember { mutableStateOf<String?>(null) }
    var chromeVisible by remember { mutableStateOf(true) }
    var zoomCarry by remember { mutableStateOf<ZoomCarry?>(null) }
    val backgroundColorArgb = MaterialTheme.colorScheme.background.toArgb()

    // Pre-renders the neighbouring pages at the same size the visible page uses so
    // a flip (especially a zoomed one) finds the next bitmap already in cache.
    suspend fun prefetchNeighbors(pageIndex: Int) {
        val neighborIndexes =
            listOf(pageIndex - 1, pageIndex + 1).filter { it in 0 until readerBook.pageCount }
        for (neighborIndex in neighborIndexes) {
            try {
                readerBook.renderPage(
                    pageIndex = neighborIndex,
                    targetWidthPx = viewportSize.value.width,
                    backgroundColor = backgroundColorArgb,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Unit
            }
        }
    }

    LaunchedEffect(readerBook, backgroundColorArgb) {
        snapshotFlow { pagerState.currentPage }.collectLatest { currentPage ->
            prefetchNeighbors(currentPage)
        }
    }
    ReaderScaffold(
        bookTitle = bookTitle,
        onBack = onBack,
        chromeVisible = chromeVisible,
        bottomBar = {
            ReaderBottomBar(pagerState = pagerState, pageCount = readerBook.pageCount)
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val density = LocalDensity.current
            LaunchedEffect(maxWidth, maxHeight) {
                viewportSize.value =
                    with(density) {
                        IntSize(
                            maxWidth.toPx().roundToInt(),
                            maxHeight.toPx().roundToInt(),
                        )
                    }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { pageIndex -> pageIndex },
                userScrollEnabled = zoomedPages.isEmpty() && selectingPages.isEmpty(),
                beyondViewportPageCount = 1,
            ) { pageIndex ->
                ReaderPage(
                    readerBook = readerBook,
                    pageIndex = pageIndex,
                    viewportSize = { viewportSize.value },
                    backgroundColor = MaterialTheme.colorScheme.background,
                    zoomCarry = zoomCarry,
                    onZoomCarryConsumed = { zoomCarry = null },
                    onZoomStateRegistered = { zoomState ->
                        if (zoomState == null) {
                            zoomStates.remove(pageIndex)
                        } else {
                            zoomStates[pageIndex] = zoomState
                        }
                    },
                    onPageEdgeSwipe = { direction ->
                        val zoomState = zoomStates[pageIndex]
                        val targetPage = pageIndex + direction
                        if (targetPage in 0 until readerBook.pageCount &&
                            zoomState != null &&
                            zoomState.isZoomed
                        ) {
                            zoomCarry =
                                ZoomCarry(
                                    targetPage = targetPage,
                                    scale = zoomState.scale,
                                    anchorStart = direction > 0,
                                )
                            scope.launch {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }
                    },
                    onZoomChanged = { isZoomed ->
                        if (isZoomed) zoomedPages[pageIndex] = true else zoomedPages.remove(pageIndex)
                    },
                    onSelectingChanged = { isSelecting ->
                        if (isSelecting) selectingPages[pageIndex] = true else selectingPages.remove(pageIndex)
                    },
                    onLinkTapped = { target ->
                        pendingLinkUri = null
                        val targetPage = target.targetPage
                        when {
                            targetPage != null -> {
                                scope.launch {
                                    pagerState.scrollToPage(
                                        targetPage.coerceIn(0, readerBook.pageCount - 1),
                                    )
                                }
                            }

                            target.uri != null -> {
                                pendingLinkUri = target.uri
                            }
                        }
                    },
                    onPlainTap = { chromeVisible = !chromeVisible },
                )
            }
            pendingLinkUri?.let { uri ->
                LinkConfirmChip(
                    uri = uri,
                    onOpen = { link ->
                        onOpenExternalLink(link)
                        pendingLinkUri = null
                    },
                    onClose = { pendingLinkUri = null },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp),
                )
            }
        }
    }
}
