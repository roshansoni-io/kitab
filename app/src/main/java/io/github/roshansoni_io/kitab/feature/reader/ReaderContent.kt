package io.github.roshansoni_io.kitab.feature.reader

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.feature.reader.document.ReaderBook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The reading surface: a horizontal pager of [ReaderPage]s plus the chrome that
 * fades over it and neighbor prefetching so page flips find their bitmaps cached.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderContent(
    readerBook: ReaderBook,
    bookTitle: String,
    resumePage: Int,
    settings: AppSettings,
    onProgress: (page: Int, pageCount: Int) -> Unit,
    onBack: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    val pagerState =
        rememberPagerState(
            initialPage = resumePage.coerceIn(0, readerBook.pageCount - 1),
            pageCount = { readerBook.pageCount },
        )
    val zoomedPages = remember { mutableStateMapOf<Int, Boolean>() }
    val selectingPages = remember { mutableStateMapOf<Int, Boolean>() }
    val viewportSize = remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    var pendingLinkUri by remember { mutableStateOf<String?>(null) }
    var chromeVisible by remember { mutableStateOf(settings.readerControlsInitiallyVisible) }
    val pageBackground =
        when (settings.readerPageTone) {
            ReaderPageTone.Theme -> MaterialTheme.colorScheme.background
            ReaderPageTone.Paper -> Color(0xFFFFFFFF)
            ReaderPageTone.Warm -> Color(0xFFFFF1D6)
            ReaderPageTone.Dark -> Color(0xFF171717)
        }
    val backgroundColorArgb = pageBackground.toArgb()
    val latestOnProgress by rememberUpdatedState(onProgress)

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
            latestOnProgress(currentPage, readerBook.pageCount)
            prefetchNeighbors(currentPage)
        }
    }

    // Renders the page under the slider finger while it is being dragged,
    // debounced so a fast drag across many pages does not flood the render queue.
    var dragPrefetchJob by remember { mutableStateOf<Job?>(null) }

    fun prefetchDragPage(pageIndex: Int) {
        if (pageIndex !in 0 until readerBook.pageCount) return
        dragPrefetchJob?.cancel()
        dragPrefetchJob =
            scope.launch {
                delay(DRAG_PREFETCH_DELAY_MS)
                try {
                    readerBook.renderPage(pageIndex, viewportSize.value.width, backgroundColorArgb)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Unit
                }
            }
    }

    // Slider release: start rendering the chosen page (an already-rendered page
    // is free) and jump to it immediately — the page composes without requesting
    // a duplicate render.
    fun openReleasedPage(pageIndex: Int) {
        dragPrefetchJob?.cancel()
        val targetPage = pageIndex.coerceIn(0, readerBook.pageCount - 1)
        scope.launch { pagerState.scrollToPage(targetPage) }
        scope.launch {
            try {
                readerBook.renderPage(targetPage, viewportSize.value.width, backgroundColorArgb)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Unit
            }
        }
    }
    ReaderScaffold(
        bookTitle = bookTitle,
        onBack = onBack,
        chromeVisible = chromeVisible,
        bottomBar = {
            ReaderBottomBar(
                pagerState = pagerState,
                pageCount = readerBook.pageCount,
                onDragPage = ::prefetchDragPage,
                onReleasePage = ::openReleasedPage,
            )
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
            val pageContent: @Composable (Int) -> Unit = { pageIndex ->
                ReaderPage(
                    readerBook = readerBook,
                    pageIndex = pageIndex,
                    viewportSize = { viewportSize.value },
                    backgroundColor = pageBackground,
                    selectionColor = settings.selectionColor,
                    maximumZoom = ZoomState.MAX_SCALE,
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
            if (settings.readerPaging == ReaderPaging.Vertical) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                    userScrollEnabled = zoomedPages.isEmpty() && selectingPages.isEmpty(),
                    beyondViewportPageCount = 1,
                    pageContent = { page -> pageContent(page) },
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                    userScrollEnabled = zoomedPages.isEmpty() && selectingPages.isEmpty(),
                    beyondViewportPageCount = 1,
                    pageContent = { page -> pageContent(page) },
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

/** How long the slider can keep moving before the hovered page starts rendering. */
private const val DRAG_PREFETCH_DELAY_MS = 120L
