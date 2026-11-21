package io.github.roshansoni_io.kitab.feature.reader.selection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/** Which end of a text selection the user is dragging. */
internal enum class SelectionHandle { Start, End }

internal class TextSelectionState {
    var range by mutableStateOf<IntRange?>(null)
        private set

    var anchorWord by mutableStateOf<IntRange?>(null)
        private set

    var activeHandle by mutableStateOf<SelectionHandle?>(null)
        private set

    var activeTipPosition by mutableStateOf<Offset?>(null)
        private set

    private var handleTouchOffset = Offset.Zero

    fun selection(pageText: PageText?): TextSelection? {
        val currentRange = range ?: return null
        val text = pageText ?: return null
        if (text.isEmpty) return null
        return text.selectionBetween(currentRange.first, currentRange.last)
    }

    fun clear() {
        range = null
        anchorWord = null
        activeHandle = null
        activeTipPosition = null
    }

    fun selectWord(
        pageText: PageText,
        index: Int,
    ) {
        anchorWord = pageText.wordRangeAt(index)
        range = anchorWord
    }

    fun dragWordSelection(
        pageText: PageText,
        point: Offset,
    ) {
        val anchor = anchorWord ?: return
        val focus = pageText.charNear(point) ?: return
        range = pageText.wordSelectionRange(anchor, focus)
    }

    fun selectAll(pageText: PageText) {
        if (pageText.chars.isNotEmpty()) {
            range = 0..pageText.chars.lastIndex
            anchorWord = null
        }
    }

    fun handleAnchors(
        pageText: PageText?,
        toScreen: (Offset) -> Offset?,
    ): Pair<Offset, Offset>? {
        val text = pageText ?: return null
        val selectedRange = range ?: return null
        val startRect = text.chars.getOrNull(selectedRange.first)?.rect ?: return null
        val endRect = text.chars.getOrNull(selectedRange.last)?.rect ?: return null
        val start = toScreen(Offset(startRect.left, startRect.bottom)) ?: return null
        val end = toScreen(Offset(endRect.right, endRect.bottom)) ?: return null
        return start to end
    }

    fun handleAt(
        position: Offset,
        anchors: Pair<Offset, Offset>?,
        widthPx: Float,
        heightPx: Float,
        touchSlopPx: Float,
    ): SelectionHandle? {
        val (start, end) = anchors ?: return null

        fun contains(anchor: Offset): Boolean =
            abs(position.x - anchor.x) <= widthPx / 2f + touchSlopPx &&
                position.y >= anchor.y - touchSlopPx &&
                position.y <= anchor.y + heightPx + touchSlopPx

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

    fun beginHandleDrag(
        handle: SelectionHandle,
        position: Offset,
        anchors: Pair<Offset, Offset>,
    ) {
        val anchor = if (handle == SelectionHandle.Start) anchors.first else anchors.second
        activeHandle = handle
        activeTipPosition = anchor
        handleTouchOffset = position - anchor
        anchorWord = null
    }

    fun dragHandle(
        pageText: PageText?,
        position: Offset,
        toPagePoint: (Offset) -> Offset?,
    ) {
        val text = pageText ?: return
        val handle = activeHandle ?: return
        val tip = position - handleTouchOffset
        activeTipPosition = tip
        val point = toPagePoint(tip) ?: return
        val index = text.charNear(point)?.coerceIn(0, text.chars.lastIndex) ?: return
        when (handle) {
            SelectionHandle.Start -> {
                val current = range ?: return
                val end = current.last
                if (index >= end) {
                    range = end..index
                    activeHandle = SelectionHandle.End
                } else {
                    range = index..end
                }
            }

            SelectionHandle.End -> {
                val current = range ?: return
                val start = current.first
                if (index <= start) {
                    range = index..start
                    activeHandle = SelectionHandle.Start
                } else {
                    range = start..index
                }
            }
        }
    }

    fun endHandleDrag() {
        activeHandle = null
        activeTipPosition = null
    }
}
