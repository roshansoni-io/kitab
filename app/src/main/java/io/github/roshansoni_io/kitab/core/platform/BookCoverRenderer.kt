package io.github.roshansoni_io.kitab.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

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
        fun android(): BookCoverRenderer = BookCoverRenderer(::renderAndroidPdfPage)
    }
}

private fun renderAndroidPdfPage(
    pdfFile: File,
    targetWidthPx: Int,
): Bitmap? =
    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use rendererUse@{ renderer ->
            if (renderer.pageCount == 0) return@rendererUse null
            val page = renderer.openPage(0)
            try {
                if (page.width <= 0 || page.height <= 0) return@rendererUse null
                val scale =
                    minOf(
                        targetWidthPx.coerceAtLeast(1).toFloat() / page.width,
                        MAX_HEIGHT_PX.toFloat() / page.height,
                    )
                val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    } catch (failure: Exception) {
                        bitmap.recycle()
                        throw failure
                    }
                }
            } finally {
                page.close()
            }
        }
    }

private const val MAX_HEIGHT_PX = 600
