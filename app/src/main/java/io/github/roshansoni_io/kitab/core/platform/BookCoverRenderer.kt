package io.github.roshansoni_io.kitab.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.artifex.mupdf.fitz.Cookie
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.RectI
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun interface BookCoverRenderer {
    fun renderFirstPage(
        pdfFile: File,
        targetWidthPx: Int,
    ): Bitmap?

    fun loadOrCreateThumbnail(
        pdfFile: File,
        thumbnailFile: File,
        targetWidthPx: Int,
    ): Bitmap? {
        BitmapFactory.decodeFile(thumbnailFile.path)?.let { return it }
        thumbnailFile.delete()
        if (!saveThumbnail(pdfFile, thumbnailFile, targetWidthPx)) return null
        val thumbnail = BitmapFactory.decodeFile(thumbnailFile.path)
        if (thumbnail == null) thumbnailFile.delete()
        return thumbnail
    }

    fun saveThumbnail(
        pdfFile: File,
        thumbnailFile: File,
        targetWidthPx: Int,
    ): Boolean {
        if (thumbnailFile.isFile) return true
        val parent = thumbnailFile.parentFile ?: return false
        if (!parent.exists() && !parent.mkdirs()) return false
        val temporary = File(parent, "${thumbnailFile.name}.${UUID.randomUUID()}.tmp")
        return try {
            val bitmap = renderFirstPage(pdfFile, targetWidthPx) ?: return false
            try {
                FileOutputStream(temporary).use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                    output.fd.sync()
                }
            } finally {
                bitmap.recycle()
            }
            check(temporary.renameTo(thumbnailFile))
            true
        } catch (_: Exception) {
            temporary.delete()
            false
        }
    }

    companion object {
        fun android(): BookCoverRenderer = BookCoverRenderer(::renderDocumentPage)
    }
}

private fun renderDocumentPage(
    documentFile: File,
    targetWidthPx: Int,
): Bitmap? {
    val document = Document.openDocument(documentFile.path) ?: return null
    try {
        if (document.isReflowable()) document.layout(COVER_PAGE_WIDTH, COVER_PAGE_HEIGHT, COVER_FONT_SIZE)
        if (document.countPages() == 0) return null
        val page = document.loadPage(0)
        try {
            val bounds = page.getBounds()
            val pageWidth = bounds.x1 - bounds.x0
            val pageHeight = bounds.y1 - bounds.y0
            if (pageWidth <= 0f || pageHeight <= 0f) return null
            val scale =
                minOf(
                    targetWidthPx.coerceAtLeast(1) / pageWidth,
                    MAX_HEIGHT_PX / pageHeight,
                )
            val matrix = Matrix(scale, scale).translate(-bounds.x0, -bounds.y0)
            val deviceBounds = RectI(bounds.transform(matrix))
            val width = (deviceBounds.x1 - deviceBounds.x0).coerceAtLeast(1)
            val height = (deviceBounds.y1 - deviceBounds.y0).coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            val displayList = page.toDisplayList()
            try {
                val device = AndroidDrawDevice(bitmap, 0, 0)
                val cookie = Cookie()
                try {
                    displayList.run(device, matrix, cookie)
                    device.close()
                } catch (failure: Exception) {
                    bitmap.recycle()
                    throw failure
                } finally {
                    cookie.destroy()
                    device.destroy()
                }
            } finally {
                displayList.destroy()
            }
            return bitmap
        } finally {
            page.destroy()
        }
    } finally {
        document.destroy()
    }
}

private const val MAX_HEIGHT_PX = 600
private const val COVER_PAGE_WIDTH = 420f
private const val COVER_PAGE_HEIGHT = 680f
private const val COVER_FONT_SIZE = 18f
