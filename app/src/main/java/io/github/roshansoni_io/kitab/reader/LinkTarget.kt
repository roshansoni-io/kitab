package io.github.roshansoni_io.kitab.reader

import android.graphics.RectF

/**
 * A tappable link region on one PDF page.
 *
 * Rect coordinates are in PDF page points (the coordinate space MuPDF uses for
 * page geometry), not in screen pixels — the UI maps them onto the rendered
 * page image before hit-testing.
 */
data class LinkTarget(
    val rect: RectF,
    /** 0-based page index the link jumps to internally, or null for external URIs. */
    val targetPage: Int?,
    /** External web URI (http/https/mailto/...), or null for internal links. */
    val uri: String?,
)
