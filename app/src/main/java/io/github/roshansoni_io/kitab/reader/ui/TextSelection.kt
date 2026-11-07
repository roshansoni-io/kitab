package io.github.roshansoni_io.kitab.reader.ui

import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import io.github.roshansoni_io.kitab.reader.PageText
import kotlin.math.hypot

/** A contiguous character selection on one page (page points). */
data class TextSelection(
    val range: IntRange,
    val rects: List<RectF>,
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
        if (char.rect.contains(point.x, point.y)) return index
    }
    var nearest = -1
    var nearestDistance = Float.MAX_VALUE
    chars.forEachIndexed { index, char ->
        val dx = char.rect.centerX() - point.x
        val dy = char.rect.centerY() - point.y
        val distance = hypot(dx, dy)
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
    if (chars[start].isWhitespace) {
        val nextWord = (start until chars.size).firstOrNull { !chars[it].isWhitespace }
        val previousWord = (start downTo 0).firstOrNull { !chars[it].isWhitespace }
        start = nextWord ?: previousWord ?: return start..start
    }
    var first = start
    while (first > 0 && !chars[first - 1].isWhitespace) first--
    var last = start
    while (last < chars.lastIndex && !chars[last + 1].isWhitespace) last++
    return first..last
}

/**
 * Builds the selection spanning from the anchor word (the long-pressed word)
 * to the word under the drag position.
 */
fun PageText.selectionFor(
    anchorWord: IntRange,
    focusIndex: Int,
): TextSelection {
    val focusWord = wordRangeAt(focusIndex)
    val range =
        if (focusWord.first < anchorWord.first) {
            focusWord.first..anchorWord.last
        } else {
            anchorWord.first..focusWord.last
        }
    return TextSelection(
        range = range,
        rects = range.map { chars[it].rect },
        text = textOf(range),
    )
}
