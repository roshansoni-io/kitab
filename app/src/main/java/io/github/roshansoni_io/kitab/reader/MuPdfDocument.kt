package io.github.roshansoni_io.kitab.reader

import android.graphics.Bitmap
import android.graphics.RectF
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.DisplayList
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.RectI
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import java.io.File

sealed class DocumentOpenException(
    message: String,
) : Exception(message) {
    class PasswordRequired : DocumentOpenException("This document is password protected")

    class NotReadable(
        path: String,
    ) : DocumentOpenException("Cannot read document at $path")

    class Failed(
        document: File,
        cause: Throwable,
    ) : DocumentOpenException("Cannot open document ${document.name} (${cause.message})")
}

class MuPdfDocument internal constructor(
    private val document: Document,
) : AutoCloseable {
    private val lock = Any()
    private var closed = false

    val pageCount: Int
    val needsPassword: Boolean

    init {
        pageCount = document.countPages()
        needsPassword = document.needsPassword()
    }

    fun title(): String =
        when (val meta = document.getMetaData(Document.META_INFO_TITLE)) {
            null, "" -> ""
            else -> meta
        }

    fun authenticate(password: String): Boolean = synchronized(lock) { document.authenticatePassword(password) }

    fun pageSize(pageIndex: Int): RectF =
        synchronized(lock) {
            checkOpen()
            with(loadPage(pageIndex)) {
                try {
                    val bounds = getBounds()
                    RectF(bounds.x0, bounds.y0, bounds.x1, bounds.y1)
                } finally {
                    destroy()
                }
            }
        }

    fun renderPage(
        pageIndex: Int,
        targetWidthPx: Int,
        backgroundColor: Int,
    ): Bitmap =
        synchronized(lock) {
            checkOpen()
            require(targetWidthPx > 0) { "targetWidthPx must be positive but was $targetWidthPx" }
            val page = loadPage(pageIndex)
            try {
                buildPageBitmap(page, pageIndex, targetWidthPx, backgroundColor)
            } finally {
                page.destroy()
            }
        }

    private fun buildPageBitmap(
        page: Page,
        pageIndex: Int,
        targetWidthPx: Int,
        backgroundColor: Int,
    ): Bitmap {
        val pageBounds = page.getBounds()
        val pageWidth = pageBounds.x1 - pageBounds.x0
        val pageHeight = pageBounds.y1 - pageBounds.y0
        require(pageWidth > 0f && pageHeight > 0f) {
            "Invalid page bounds for index $pageIndex"
        }

        val zoom =
            minOf(
                targetWidthPx.toFloat() / pageWidth,
                MAX_RENDERED_HEIGHT_PX.toFloat() / pageHeight,
            )
        val ctm = Matrix(zoom, zoom)
        val deviceBounds = RectI(pageBounds.transform(ctm))
        val bitmapWidth = (deviceBounds.x1 - deviceBounds.x0).coerceAtLeast(1)
        val bitmapHeight = (deviceBounds.y1 - deviceBounds.y0).coerceAtLeast(1)

        val displayList: DisplayList = page.toDisplayList()
        try {
            val bitmap =
                Bitmap.createBitmap(
                    bitmapWidth,
                    bitmapHeight,
                    Bitmap.Config.ARGB_8888,
                )
            bitmap.eraseColor(backgroundColor)

            val device = AndroidDrawDevice(bitmap, 0, 0)
            val cookie = Cookie()
            try {
                displayList.run(device, ctm, cookie)
                device.close()
            } finally {
                cookie.destroy()
                device.destroy()
            }
            return bitmap
        } finally {
            displayList.destroy()
        }
    }

    private fun loadPage(pageIndex: Int): Page {
        val boundedIndex = pageIndex.coerceIn(0, pageCount - 1)
        return document.loadPage(boundedIndex)
    }

    private fun checkOpen() {
        check(!closed, ::closedException)
    }

    private fun closedException(): IllegalStateException = IllegalStateException("MuPdfDocument is already closed")

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            document.destroy()
        }
    }

    companion object {
        private const val MAX_RENDERED_HEIGHT_PX = 5600

        fun open(file: File): MuPdfDocument =
            try {
                Document.openDocument(file.path)?.let(::MuPdfDocument)
                    ?: throw DocumentOpenException.NotReadable(file.path)
            } catch (notFound: DocumentOpenException) {
                throw notFound
            } catch (cause: Exception) {
                throw DocumentOpenException.Failed(file, cause)
            }
    }
}
