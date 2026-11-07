package io.github.roshansoni_io.kitab.reader

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
