package io.github.roshansoni_io.kitab.feature.reader.document

import android.graphics.Bitmap
import android.graphics.Color
import io.github.roshansoni_io.kitab.core.data.repository.PdfCoverRenderer
import java.io.File

class MuPdfCoverRenderer : PdfCoverRenderer {
    override fun renderFirstPage(
        pdfFile: File,
        targetWidthPx: Int,
    ): Bitmap? {
        val document = MuPdfDocument.open(pdfFile)
        try {
            if (document.pageCount < 1) return null
            return document.renderPage(
                pageIndex = 0,
                targetWidthPx = targetWidthPx,
                backgroundColor = Color.WHITE,
            )
        } finally {
            document.close()
        }
    }
}
