package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.book.Book
import io.github.roshansoni_io.kitab.reader.DocumentOpenException
import io.github.roshansoni_io.kitab.reader.ReaderBook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    onBack: () -> Unit,
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
        bottomBar = {},
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
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
        bottomBar = {},
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
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
) {
    val pagerState = rememberPagerState(pageCount = { readerBook.pageCount })
    val zoomedPages = remember { mutableStateMapOf<Int, Boolean>() }
    val viewportSize = remember { mutableStateOf(IntSize.Zero) }

    ReaderScaffold(
        bookTitle = bookTitle,
        onBack = onBack,
        bottomBar = {
            ReaderBottomBar(pagerState = pagerState, pageCount = readerBook.pageCount)
        },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
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
                userScrollEnabled = zoomedPages.isEmpty(),
                beyondViewportPageCount = 1,
            ) { pageIndex ->
                ReaderPage(
                    readerBook = readerBook,
                    pageIndex = pageIndex,
                    viewportSize = { viewportSize.value },
                    backgroundColor = MaterialTheme.colorScheme.background,
                    onZoomChanged = { isZoomed ->
                        if (isZoomed) zoomedPages[pageIndex] = true else zoomedPages.remove(pageIndex)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReaderPage(
    readerBook: ReaderBook,
    pageIndex: Int,
    viewportSize: () -> IntSize,
    backgroundColor: Color,
    onZoomChanged: (Boolean) -> Unit,
) {
    val pageWidthPx = viewportSize().width
    var pageBitmap by remember(pageIndex, pageWidthPx) { mutableStateOf<Bitmap?>(null) }
    var renderFailed by remember(pageIndex, pageWidthPx) { mutableStateOf(false) }
    var renderedWidthPixels by remember(pageIndex, pageWidthPx) { mutableIntStateOf(0) }
    var zoomSettleTick by remember(pageIndex, pageWidthPx) { mutableIntStateOf(0) }

    val zoomState =
        remember(pageIndex) {
            ZoomState(
                viewportSize = viewportSize,
                contentSize = {
                    val bitmap = pageBitmap
                    if (bitmap == null || pageWidthPx <= 0) {
                        IntSize.Zero
                    } else {
                        PageGeometry.fittedSize(
                            bitmap = bitmap,
                            viewport = viewportSize(),
                        )
                    }
                },
            )
        }

    LaunchedEffect(pageIndex, pageWidthPx) {
        if (pageWidthPx > 0 && pageBitmap == null && !renderFailed) {
            try {
                pageBitmap =
                    readerBook.renderPage(
                        pageIndex = pageIndex,
                        targetWidthPx = pageWidthPx,
                        backgroundColor = backgroundColor.toArgb(),
                    )
                renderedWidthPixels = pageWidthPx
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (renderFailure: Exception) {
                renderFailed = true
            }
        }
    }

    LaunchedEffect(zoomSettleTick) {
        val bitmap = pageBitmap
        if (zoomSettleTick > 0 && bitmap != null && zoomState.scale > ZOOM_SHARPEN_THRESHOLD) {
            val targetWidth =
                (pageWidthPx * zoomState.scale)
                    .roundToInt()
                    .coerceAtMost(ZOOM_RENDER_MAX_WIDTH_PX)
            if (targetWidth > renderedWidthPixels) {
                try {
                    pageBitmap =
                        readerBook.renderPage(
                            pageIndex = pageIndex,
                            targetWidthPx = targetWidth,
                            backgroundColor = backgroundColor.toArgb(),
                        )
                    renderedWidthPixels = targetWidth
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (renderFailure: Exception) {
                    Unit
                }
            }
        }
    }

    LaunchedEffect(zoomState) {
        snapshotFlow { zoomState.isZoomed }.collect { isZoomed -> onZoomChanged(isZoomed) }
    }

    DisposableEffect(pageIndex) {
        onDispose { onZoomChanged(false) }
    }

    val bitmap = pageBitmap
    when {
        bitmap == null && renderFailed -> {
            ReaderPageError()
        }

        bitmap == null -> {
            ReaderPageLoading()
        }

        else -> {
            ZoomedPageImage(
                bitmap = bitmap,
                zoomState = zoomState,
                onGestureEnd = { zoomSettleTick++ },
            )
        }
    }
}

@Composable
private fun ZoomedPageImage(
    bitmap: Bitmap,
    zoomState: ZoomState,
    onGestureEnd: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "PDF page",
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxSize()
                    .zoomable(
                        state = zoomState,
                        onDoubleTap = { tapPoint ->
                            if (zoomState.isZoomed) {
                                zoomState.reset()
                            } else {
                                zoomState.zoomBy(DOUBLE_TAP_ZOOM_FACTOR, tapPoint)
                            }
                        },
                        onGestureEnd = onGestureEnd,
                    ).graphicsLayer {
                        scaleX = zoomState.scale
                        scaleY = zoomState.scale
                        translationX = zoomState.offsetX
                        translationY = zoomState.offsetY
                    },
        )
    }
}

@Composable
private fun ReaderPageLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReaderPageError() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "This page could not be rendered.", style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScaffold(
    bookTitle: String,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = bookTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun ReaderBottomBar(
    pagerState: PagerState,
    pageCount: Int,
) {
    if (pageCount < 2) return
    var draggingValue by remember { mutableStateOf<Float?>(null) }
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val displayValue = draggingValue ?: currentPage.toFloat()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = ScreenPadding.medium, vertical = ScreenPadding.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ScreenPadding.medium),
    ) {
        Text(
            text = String.format(Locale.US, "%d / %d", currentPage + 1, pageCount),
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = displayValue,
            onValueChange = { value -> draggingValue = value },
            onValueChangeFinished = {
                val target = (displayValue).roundToInt().coerceIn(0, pageCount - 1)
                draggingValue = null
                scope.launch { pagerState.scrollToPage(target) }
            },
            valueRange = 0f..(pageCount - 1).toFloat(),
        )
    }
}

private object ScreenPadding {
    val medium = 24.dp
    val small = 8.dp
}

private const val DOUBLE_TAP_ZOOM_FACTOR = 2.5f
private const val ZOOM_SHARPEN_THRESHOLD = 1.15f
private const val ZOOM_RENDER_MAX_WIDTH_PX = 3000
