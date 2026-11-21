package io.github.roshansoni_io.kitab.core.data.repository

import android.graphics.Bitmap
import java.io.File

fun interface PdfCoverRenderer {
    fun renderFirstPage(
        pdfFile: File,
        targetWidthPx: Int,
    ): Bitmap?
}
