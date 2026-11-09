package io.github.roshansoni_io.kitab.reader.ui

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.reader.LinkTarget
import io.github.roshansoni_io.kitab.reader.PageText
import io.github.roshansoni_io.kitab.reader.ReaderBook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    onPageEdgeSwipe: (Int) -> Unit,
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
    var pageTextLoadFailed by remember(pageIndex) { mutableStateOf(false) }
    var selectionRange by remember(pageIndex) { mutableStateOf<IntRange?>(null) }
    var anchorWord by remember(pageIndex) { mutableStateOf<IntRange?>(null) }
    var activeHandle by remember(pageIndex) { mutableStateOf<SelectionHandle?>(null) }
    var activeHandleTip by remember(pageIndex) { mutableStateOf<Offset?>(null) }
    var handleTouchOffset by remember(pageIndex) { mutableStateOf(Offset.Zero) }
    val selection =
        remember(selectionRange, pageText) {
            val range = selectionRange
            val text = pageText
            if (text != null && range != null && !text.isEmpty) {
                text.selectionBetween(range.first, range.last)
            } else {
                null
            }
        }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val touchSlopPx = remember(context) { ViewConfiguration.get(context).scaledTouchSlop.toFloat() }
    val handlePainter = painterResource(R.drawable.selection_handle)
    val handleWidthPx = with(density) { SELECTION_HANDLE_WIDTH.toPx() }
    val handleHeightPx = with(density) { SELECTION_HANDLE_HEIGHT.toPx() }
    val handleTouchSlopPx = with(density) { 16.dp.toPx() }

    LaunchedEffect(pageIndex) {
        try {
            pageSize = readerBook.pageSize(pageIndex)
            linkTargets = readerBook.linkTargets(pageIndex)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.e("ReaderPage", "Could not load page geometry or links for page $pageIndex", failure)
        }
        try {
            pageText = readerBook.pageText(pageIndex)
            Log.d("ReaderPage", "Loaded ${pageText?.chars?.size ?: 0} text characters on page $pageIndex")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            pageTextLoadFailed = true
            Log.e("ReaderPage", "Could not extract text on page $pageIndex", failure)
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

    fun toScreen(pagePoint: Offset): Offset? {
        val points = pageSize ?: return null
        val mapper = PageMapper(points, viewportSize())
        if (!mapper.isUsable) return null
        return zoomState.toScreen(mapper.toDisplay(pagePoint))
    }

    fun clearSelection() {
        selectionRange = null
        anchorWord = null
        activeHandle = null
        activeHandleTip = null
    }

    fun handleAnchors(): Pair<Offset, Offset>? {
        val text = pageText ?: return null
        val range = selectionRange ?: return null
        val start = range.first
        val end = range.last
        val startRect = text.chars.getOrNull(start)?.rect ?: return null
        val endRect = text.chars.getOrNull(end.coerceAtLeast(start))?.rect ?: return null

        val startAnchor = toScreen(Offset(startRect.left, startRect.bottom)) ?: return null
        val endAnchor = toScreen(Offset(endRect.right, endRect.bottom)) ?: return null
        return startAnchor to endAnchor
    }

    fun handleAt(position: Offset): SelectionHandle? {
        val (start, end) = handleAnchors() ?: return null

        fun contains(anchor: Offset): Boolean =
            abs(position.x - anchor.x) <= handleWidthPx / 2f + handleTouchSlopPx &&
                position.y >= anchor.y - handleTouchSlopPx &&
                position.y <= anchor.y + handleHeightPx + handleTouchSlopPx

        val hitsStart = contains(start)
        val hitsEnd = contains(end)
        return when {
            hitsStart && hitsEnd -> {
                if ((position - start).getDistance() <= (position - end).getDistance()) {
                    SelectionHandle.Start
                } else {
                    SelectionHandle.End
                }
            }

            hitsStart -> {
                SelectionHandle.Start
            }

            hitsEnd -> {
                SelectionHandle.End
            }

            else -> {
                null
            }
        }
    }

    fun dragSelectionHandle(position: Offset) {
        val text = pageText ?: return
        val handle = activeHandle ?: return
        val point = toPagePoint(position) ?: return
        val index =
            text.charNear(point)?.coerceIn(0, text.chars.lastIndex)
                ?: return
        when (handle) {
            SelectionHandle.Start -> {
                val range = selectionRange ?: return
                val end = range.last
                if (index >= end) {
                    selectionRange = end..index
                    activeHandle = SelectionHandle.End
                } else {
                    selectionRange = index..end
                }
            }

            SelectionHandle.End -> {
                val range = selectionRange ?: return
                val start = range.first
                if (index <= start) {
                    selectionRange = index..start
                    activeHandle = SelectionHandle.Start
                } else {
                    selectionRange = start..index
                }
            }
        }
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
            val selectionPageBounds = remember(selection) { selection?.bounds() }
            val selectionScreenBounds =
                selectionPageBounds?.let { bounds ->
                    val topLeft = toScreen(Offset(bounds.left, bounds.top))
                    val bottomRight = toScreen(Offset(bounds.right, bounds.bottom))
                    if (topLeft != null && bottomRight != null) {
                        RectF(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
                    } else {
                        null
                    }
                }
            var toolbarSize by remember { mutableStateOf(IntSize.Zero) }
            val gap = with(density) { 8.dp.toPx() }
            val margin = with(density) { 8.dp.toPx() }
            val toolbarOffset =
                if (selectionScreenBounds != null && toolbarSize != IntSize.Zero) {
                    val viewport = viewportSize()
                    val x =
                        (selectionScreenBounds.centerX() - toolbarSize.width / 2f)
                            .coerceIn(
                                margin,
                                (viewport.width - toolbarSize.width - margin).coerceAtLeast(margin),
                            )
                    val above = selectionScreenBounds.top - toolbarSize.height - gap
                    val y =
                        if (above >= margin) {
                            above
                        } else {
                            (selectionScreenBounds.bottom + gap)
                                .coerceAtMost(
                                    (viewport.height - toolbarSize.height - margin)
                                        .coerceAtLeast(margin),
                                )
                        }
                    IntOffset(x.roundToInt(), y.roundToInt())
                } else {
                    IntOffset.Zero
                }

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
                            clearSelection()
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
                        if (text != null && text.isEmpty) {
                            val message =
                                if (pageTextLoadFailed) {
                                    "Could not read text on this page"
                                } else {
                                    "This page has no selectable text; it may be scanned"
                                }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                        val index =
                            if (text != null && point != null) {
                                text.charNear(point, maxDistance = LONG_PRESS_SLOP_POINTS)
                            } else {
                                null
                            }
                        if (text != null && index != null) {
                            val word = text.wordRangeAt(index)
                            anchorWord = word
                            selectionRange = word
                            onSelectingChanged(true)
                            true
                        } else {
                            if (text != null) {
                                Log.d("ReaderPage", "No text character near long press on page $pageIndex at $point")
                                Toast.makeText(context, "Couldn't detect text at this position", Toast.LENGTH_SHORT).show()
                            }
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
                                val range = text.wordSelectionRange(anchor, focus)
                                selectionRange = range
                            }
                        }
                    },
                    onSelectionEnd = { onSelectingChanged(false) },
                    onHandleHit = { position ->
                        val handle = handleAt(position)
                        val anchors = handleAnchors()
                        if (handle != null && anchors != null) {
                            val anchor = if (handle == SelectionHandle.Start) anchors.first else anchors.second
                            activeHandle = handle
                            activeHandleTip = anchor
                            handleTouchOffset = position - anchor
                            anchorWord = null
                            onSelectingChanged(true)
                            true
                        } else {
                            false
                        }
                    },
                    onHandleDrag = { position ->
                        val tip = position - handleTouchOffset
                        activeHandleTip = tip
                        dragSelectionHandle(tip)
                    },
                    onHandleEnd = {
                        activeHandle = null
                        activeHandleTip = null
                        onSelectingChanged(false)
                    },
                    onPageEdgeSwipe = onPageEdgeSwipe,
                )
                val anchors = handleAnchors()
                if (selection != null && anchors != null) {
                    SelectionHandles(
                        start = anchors.first,
                        end = anchors.second,
                        painter = handlePainter,
                        activeHandle = activeHandle,
                        activeTipPosition = activeHandleTip,
                    )
                }
                if (selection != null && selectionScreenBounds != null) {
                    SelectionToolbar(
                        onCopy = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(ClipData.newPlainText("Kitab", selection.text())),
                                )
                            }
                        },
                        onSelectAll = {
                            val text = pageText
                            if (text != null && text.chars.isNotEmpty()) {
                                selectionRange = 0..text.chars.lastIndex
                                anchorWord = null
                            }
                        },
                        onShare = {
                            val share =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, selection.text())
                                }
                            context.startActivity(
                                Intent
                                    .createChooser(share, "Share text")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                        onClose = { clearSelection() },
                        modifier =
                            Modifier
                                .offset { toolbarOffset }
                                .onSizeChanged { toolbarSize = it }
                                .zIndex(1f),
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
    onHandleHit: (Offset) -> Boolean,
    onHandleDrag: (Offset) -> Unit,
    onHandleEnd: () -> Unit,
    onPageEdgeSwipe: (Int) -> Unit,
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
                        onHandleHit = onHandleHit,
                        onHandleDrag = onHandleDrag,
                        onHandleEnd = onHandleEnd,
                        onPageEdgeSwipe = onPageEdgeSwipe,
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
                    pageText = selection.pageText,
                    startIndex = selection.first,
                    endIndex = selection.last,
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
