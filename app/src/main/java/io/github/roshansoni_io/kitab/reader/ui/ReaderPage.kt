package io.github.roshansoni_io.kitab.reader.ui

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.RectF
import android.view.ViewConfiguration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.reader.LinkTarget
import io.github.roshansoni_io.kitab.reader.PageText
import io.github.roshansoni_io.kitab.reader.ReaderBook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun ReaderPage(
    readerBook: ReaderBook,
    pageIndex: Int,
    viewportSize: () -> IntSize,
    backgroundColor: Color,
    onZoomChanged: (Boolean) -> Unit,
    onSelectingChanged: (Boolean) -> Unit,
    onLinkTapped: (LinkTarget) -> Unit,
) {
    val pageWidthPx = viewportSize().width
    var pageBitmap by remember(pageIndex, pageWidthPx) { mutableStateOf<Bitmap?>(null) }
    var renderFailed by remember(pageIndex, pageWidthPx) { mutableStateOf(false) }
    var zoomSettleTick by remember(pageIndex, pageWidthPx) { mutableIntStateOf(0) }
    var sharpRegion by remember(pageIndex, pageWidthPx) { mutableStateOf<SharpRegion?>(null) }
    var lastRegionSpec by remember(pageIndex, pageWidthPx) { mutableStateOf<RegionRenderSpec?>(null) }
    var pageSize by remember(pageIndex) { mutableStateOf<RectF?>(null) }
    var linkTargets by remember(pageIndex) { mutableStateOf<List<LinkTarget>>(emptyList()) }
    var pageText by remember(pageIndex) { mutableStateOf<PageText?>(null) }
    var anchorWord by remember(pageIndex) { mutableStateOf<IntRange?>(null) }
    var selection by remember(pageIndex) { mutableStateOf<TextSelection?>(null) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val touchSlopPx = remember(context) { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }

    LaunchedEffect(pageIndex) {
        try {
            pageSize = readerBook.pageSize(pageIndex)
            linkTargets = readerBook.linkTargets(pageIndex)
            pageText = readerBook.pageText(pageIndex)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (loadFailure: Exception) {
            Unit
        }
    }

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

    fun toPagePoint(layout: Offset): Offset? {
        val points = pageSize ?: return null
        val mapper = PageMapper(points, viewportSize())
        if (!mapper.isUsable) return null
        return mapper.toPagePoint(zoomState.fromScreen(layout))
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (renderFailure: Exception) {
                renderFailed = true
            }
        }
    }

    LaunchedEffect(zoomSettleTick) {
        if (zoomSettleTick == 0) return@LaunchedEffect
        val points = pageSize ?: return@LaunchedEffect
        val bitmap = pageBitmap ?: return@LaunchedEffect
        if (zoomState.scale <= ZoomState.SNAP_BACK_THRESHOLD) {
            sharpRegion = null
            lastRegionSpec = null
            return@LaunchedEffect
        }
        val viewport = viewportSize()
        val spec =
            PageGeometry.visiblePageRegion(
                pageSizePoints = points,
                viewport = viewport,
                zoomScale = zoomState.scale,
                panX = zoomState.offsetX,
                panY = zoomState.offsetY,
            ) ?: return@LaunchedEffect
        if (spec.matches(lastRegionSpec)) return@LaunchedEffect
        try {
            val regionBitmap =
                readerBook.renderRegion(
                    pageIndex = pageIndex,
                    pageRect = spec.pageRect,
                    zoom = spec.zoom,
                    backgroundColor = backgroundColor.toArgb(),
                )
            val fitted = PageGeometry.fittedSize(bitmap, viewport)
            val contentScale = fitted.width / points.width()
            val letterboxX = (viewport.width - fitted.width) / 2f
            val letterboxY = (viewport.height - fitted.height) / 2f
            val left = letterboxX + (spec.pageRect.left - points.left) * contentScale
            val top = letterboxY + (spec.pageRect.top - points.top) * contentScale
            sharpRegion =
                SharpRegion(
                    bitmap = regionBitmap,
                    left = left,
                    top = top,
                    width = spec.pageRect.width() * contentScale,
                    height = spec.pageRect.height() * contentScale,
                )
            lastRegionSpec = spec
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (renderFailure: Exception) {
            Unit
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
            Box(modifier = Modifier.fillMaxSize()) {
                ZoomedPageImage(
                    bitmap = bitmap,
                    zoomState = zoomState,
                    sharpRegion = sharpRegion,
                    selection = selection,
                    pageSize = pageSize,
                    onGestureEnd = { zoomSettleTick++ },
                    onTap = { tapPoint ->
                        if (selection != null) {
                            selection = null
                            anchorWord = null
                        } else {
                            val points = pageSize
                            val pagePoint = toPagePoint(tapPoint)
                            if (points != null && pagePoint != null) {
                                val mapper = PageMapper(points, viewportSize())
                                val hit = mapper.hitTest(linkTargets, pagePoint, touchSlopPx)
                                hit?.let(onLinkTapped)
                            }
                        }
                    },
                    onLongPress = { position ->
                        val text = pageText
                        val point = toPagePoint(position)
                        val index =
                            if (text != null && point != null) {
                                text.charNear(point, maxDistance = LONG_PRESS_SLOP_POINTS)
                            } else {
                                null
                            }
                        if (text != null && index != null) {
                            val word = text.wordRangeAt(index)
                            anchorWord = word
                            selection = text.selectionFor(word, index)
                            onSelectingChanged(true)
                            true
                        } else {
                            false
                        }
                    },
                    onSelectionDrag = { position ->
                        val text = pageText
                        val anchor = anchorWord
                        val point = toPagePoint(position)
                        if (text != null && anchor != null && point != null) {
                            val focus = text.charNear(point)
                            if (focus != null) {
                                selection = text.selectionFor(anchor, focus)
                            }
                        }
                    },
                    onSelectionEnd = { onSelectingChanged(false) },
                )
                selection?.let { current ->
                    SelectionToolbar(
                        onCopy = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("Kitab", current.text)),
                                )
                            }
                            selection = null
                            anchorWord = null
                        },
                        onClose = {
                            selection = null
                            anchorWord = null
                        },
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomedPageImage(
    bitmap: Bitmap,
    zoomState: ZoomState,
    sharpRegion: SharpRegion?,
    selection: TextSelection?,
    pageSize: RectF?,
    onGestureEnd: () -> Unit,
    onTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Boolean,
    onSelectionDrag: (Offset) -> Unit,
    onSelectionEnd: () -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .zoomable(
                        state = zoomState,
                        onTap = onTap,
                        onDoubleTap = { tapPoint ->
                            if (zoomState.isZoomed) {
                                zoomState.reset()
                            } else {
                                zoomState.zoomBy(DOUBLE_TAP_ZOOM_FACTOR, tapPoint)
                            }
                            onGestureEnd()
                        },
                        onGestureEnd = onGestureEnd,
                        onLongPress = onLongPress,
                        onSelectionDrag = onSelectionDrag,
                        onSelectionEnd = onSelectionEnd,
                    ).graphicsLayer {
                        scaleX = zoomState.scale
                        scaleY = zoomState.scale
                        translationX = zoomState.offsetX
                        translationY = zoomState.offsetY
                    },
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "PDF page",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (sharpRegion != null) {
                Image(
                    bitmap = sharpRegion.bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier =
                        Modifier
                            .offset {
                                with(density) {
                                    IntOffset(
                                        sharpRegion.left.roundToInt(),
                                        sharpRegion.top.roundToInt(),
                                    )
                                }
                            }.size(
                                width = with(density) { sharpRegion.width.toDp() },
                                height = with(density) { sharpRegion.height.toDp() },
                            ),
                )
            }
            val points = pageSize
            if (selection != null && points != null) {
                SelectionOverlay(
                    rects = selection.rects,
                    pageSize = points,
                )
            }
        }
    }
}

private data class SharpRegion(
    val bitmap: Bitmap,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

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

private const val DOUBLE_TAP_ZOOM_FACTOR = 2.5f
private const val LONG_PRESS_SLOP_POINTS = 30f
