package io.github.roshansoni_io.kitab.selection

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.hypot

/** A contiguous character selection on one page. */
data class TextSelection(
    val pageText: PageText,
    val startIndex: Int,
    val endIndex: Int,
) {
    val first: Int = minOf(startIndex, endIndex)
    val last: Int = maxOf(startIndex, endIndex)

    fun text(): String = pageText.textOf(first..last)

    fun bounds(): RectF {
        val firstRect = pageText.chars[first].rect
        var left = firstRect.left
        var top = firstRect.top
        var right = firstRect.right
        var bottom = firstRect.bottom
        for (index in first + 1..last) {
            val rect = pageText.chars[index].rect
            left = minOf(left, rect.left)
            top = minOf(top, rect.top)
            right = maxOf(right, rect.right)
            bottom = maxOf(bottom, rect.bottom)
        }
        return RectF(left, top, right, bottom)
    }
}

/**
 * Finds the character at [point] (page points). When the point is outside all
 * glyph rectangles, the nearest character within [maxDistance] points is used,
 * so dragging past the text edge still extends the selection sensibly.
 */
fun PageText.charNear(
    point: Offset,
    maxDistance: Float = Float.MAX_VALUE,
): Int? {
    if (isEmpty) return null
    var lineIndex = -1
    var nearestVerticalDistance = Float.MAX_VALUE
    var nearestCenterDistance = Float.MAX_VALUE
    var nearestBoundsDistance = Float.MAX_VALUE
    lineBounds.forEachIndexed { index, bounds ->
        val verticalDistance =
            when {
                point.y < bounds.top -> bounds.top - point.y
                point.y > bounds.bottom -> point.y - bounds.bottom
                else -> 0f
            }
        val centerDistance = abs(point.y - bounds.centerY())
        val boundsDistance = bounds.distanceTo(point)
        if (
            verticalDistance < nearestVerticalDistance ||
            (verticalDistance == nearestVerticalDistance && centerDistance < nearestCenterDistance) ||
            (
                verticalDistance == nearestVerticalDistance &&
                    centerDistance == nearestCenterDistance &&
                    boundsDistance < nearestBoundsDistance
            )
        ) {
            lineIndex = index
            nearestVerticalDistance = verticalDistance
            nearestCenterDistance = centerDistance
            nearestBoundsDistance = boundsDistance
        }
    }
    if (lineIndex < 0) return null
    val line = lineRanges[lineIndex]
    var containingWhitespace: Int? = null
    var nearestIndex = -1
    var nearestDistance = Float.MAX_VALUE
    for (index in line) {
        val char = chars[index]
        val distance = char.rect.distanceTo(point)
        if (distance == 0f) {
            if (!char.isWordSeparator()) return index
            containingWhitespace = index
        } else if (!char.isWordSeparator() && distance < nearestDistance) {
            nearestIndex = index
            nearestDistance = distance
        }
    }
    if (containingWhitespace != null) {
        val index = containingWhitespace
        return listOf(index - 1, index + 1)
            .filter { it in line }
            .filterNot { chars[it].isWordSeparator() }
            .minByOrNull { chars[it].rect.distanceTo(point) }
            ?: index
    }
    return nearestIndex.takeIf { nearestDistance <= maxDistance }
}

/** Expands [index] to the whole word (non-whitespace run) containing it. */
fun PageText.wordRangeAt(index: Int): IntRange {
    if (isEmpty) return IntRange.EMPTY
    var start = index.coerceIn(0, chars.lastIndex)
    if (chars[start].isWordSeparator()) {
        start =
            chars.indices
                .filterNot { chars[it].isWordSeparator() }
                .minByOrNull { chars[it].rect.distanceTo(chars[start].rect.centerX(), chars[start].rect.centerY()) }
                ?: return start..start
    }
    var first = start
    while (first > 0 && !hasWordBoundary(first - 1, first)) first--
    var last = start
    while (last < chars.lastIndex && !hasWordBoundary(last, last + 1)) last++
    return first..last
}

private fun PageText.hasWordBoundary(
    leftIndex: Int,
    rightIndex: Int,
): Boolean {
    val left = chars[leftIndex]
    val right = chars[rightIndex]
    if (left.lineIndex != right.lineIndex || left.isWordSeparator() || right.isWordSeparator()) return true
    val gap =
        if (left.rect.centerX() <= right.rect.centerX()) {
            right.rect.left - left.rect.right
        } else {
            left.rect.left - right.rect.right
        }
    val typicalHeight = maxOf(left.rect.height(), right.rect.height())
    return gap > maxOf(MIN_WORD_GAP_POINTS, typicalHeight * WORD_GAP_HEIGHT_RATIO)
}

private fun PageChar.isWordSeparator(): Boolean = isWhitespace || text.isBlank()

private fun RectF.distanceTo(point: Offset): Float = distanceTo(point.x, point.y)

private fun RectF.distanceTo(
    x: Float,
    y: Float,
): Float {
    val dx =
        when {
            x < left -> left - x
            x > right -> x - right
            else -> 0f
        }
    val dy =
        when {
            y < top -> top - y
            y > bottom -> y - bottom
            else -> 0f
        }
    return hypot(dx, dy)
}

private const val MIN_WORD_GAP_POINTS = 1.5f
private const val WORD_GAP_HEIGHT_RATIO = 0.28f

/** Builds the character selection spanning [startIndex] through [endIndex] in either order. */
fun PageText.selectionBetween(
    startIndex: Int,
    endIndex: Int,
): TextSelection {
    require(!isEmpty) { "Cannot select characters from an empty page" }
    val first = startIndex.coerceIn(0, chars.lastIndex)
    val last = endIndex.coerceIn(0, chars.lastIndex)
    return TextSelection(this, first, last)
}

/**
 * The word range spanning from the anchor word to the word under the drag position.
 */
fun PageText.wordSelectionRange(
    anchorWord: IntRange,
    focusIndex: Int,
): IntRange {
    val focusWord = wordRangeAt(focusIndex)
    return if (focusWord.first < anchorWord.first) {
        focusWord.first..anchorWord.last
    } else {
        anchorWord.first..focusWord.last
    }
}
