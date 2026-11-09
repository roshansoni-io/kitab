package io.github.roshansoni_io.kitab.selection

import android.graphics.RectF

/** One selectable character on a page; coordinates are PDF page points. */
data class PageChar(
    val text: String,
    val rect: RectF,
    val isWhitespace: Boolean,
    val lineIndex: Int,
)

/** Selectable text of one page in reading order. */
data class PageText(
    val chars: List<PageChar>,
) {
    val lineRanges: List<IntRange> =
        buildList {
            if (chars.isEmpty()) return@buildList
            var lineStart = 0
            for (index in 1 until chars.size) {
                if (chars[index].lineIndex != chars[index - 1].lineIndex) {
                    add(lineStart until index)
                    lineStart = index
                }
            }
            add(lineStart until chars.size)
        }

    val lineBounds: List<RectF> =
        lineRanges.map { range ->
            val first = chars[range.first].rect
            var left = first.left
            var top = first.top
            var right = first.right
            var bottom = first.bottom
            for (index in range.drop(1)) {
                val rect = chars[index].rect
                left = minOf(left, rect.left)
                top = minOf(top, rect.top)
                right = maxOf(right, rect.right)
                bottom = maxOf(bottom, rect.bottom)
            }
            RectF(left, top, right, bottom)
        }

    val isEmpty: Boolean get() = chars.isEmpty()

    /** Joins the characters in [range], inserting line breaks when the line changes. */
    fun textOf(range: IntRange): String {
        if (range.isEmpty()) return ""
        val builder = StringBuilder()
        var previousLine = -1
        for (index in range) {
            val char = chars[index]
            if (previousLine != -1 && char.lineIndex != previousLine) {
                builder.append('\n')
            }
            builder.append(char.text)
            previousLine = char.lineIndex
        }
        return builder.toString()
    }
}
