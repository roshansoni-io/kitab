package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import io.github.roshansoni_io.kitab.reader.PageText
import kotlin.math.hypot

/** A contiguous character selection on one page (page points). */
data class TextSelection(
    val rects: List<RectF>,
    val rectsByLine: List<List<RectF>>,
    val text: String,
)

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
    chars.forEachIndexed { index, char ->
        if (char.rect.contains(point.x, point.y) && !char.isWordSeparator()) return index
    }
    // PDF text extraction can give a space a wide hit box that overlaps the
    // neighboring glyph. Prefer actual letters over whitespace in that case.
    chars.forEachIndexed { index, char ->
        if (char.rect.contains(point.x, point.y)) {
            val neighbors = listOf(index - 1, index + 1).filter { it in chars.indices }
            return neighbors
                .filterNot { chars[it].isWordSeparator() }
                .minByOrNull { chars[it].rect.distanceTo(point) }
                ?: index
        }
    }
    var nearest = -1
    var nearestDistance = Float.MAX_VALUE
    chars.forEachIndexed { index, char ->
        if (char.isWordSeparator()) return@forEachIndexed
        val distance = char.rect.distanceTo(point)
        if (distance < nearestDistance) {
            nearestDistance = distance
            nearest = index
        }
    }
    return nearest.takeIf { nearestDistance <= maxDistance }
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

private fun io.github.roshansoni_io.kitab.reader.PageChar.isWordSeparator(): Boolean = isWhitespace || text.isBlank()

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
    if (isEmpty) return TextSelection(rects = emptyList(), rectsByLine = emptyList(), text = "")
    val first = minOf(startIndex, endIndex).coerceIn(0, chars.lastIndex)
    val last = maxOf(startIndex, endIndex).coerceIn(0, chars.lastIndex)
    val range = first..last
    return TextSelection(
        rects = range.map { chars[it].rect },
        rectsByLine = range.groupBy { chars[it].lineIndex }.values.map { line -> line.map { chars[it].rect } },
        text = textOf(range),
    )
}

/**
 * The word range spanning from the anchor word (the long-pressed word) to the
 * word under the drag position.
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
