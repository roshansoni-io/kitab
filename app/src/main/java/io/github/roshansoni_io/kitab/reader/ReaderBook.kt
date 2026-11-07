package io.github.roshansoni_io.kitab.reader

import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.os.trace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ReaderBook private constructor(
    val document: MuPdfDocument,
) : AutoCloseable {

    val pageCount: Int get() = document.pageCount

    private val pageCache = PageBitmapCache(maxBytes = DEFAULT_CACHE_BYTES)
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    suspend fun renderPage(
        pageIndex: Int,
        targetWidthPx: Int,
        backgroundColor: Int,
    ): Bitmap {
        require(pageIndex in 0 until pageCount) { "Invalid page index $pageIndex" }
        pageCache.get(pageIndex)?.let { return it }
        return withContext(renderDispatcher) {
            pageCache.get(pageIndex)?.let { return@withContext it }
            trace("Kitab.pageRender") {
                document.renderPage(pageIndex, targetWidthPx, backgroundColor)
                    .also { pageCache.put(pageIndex, it) }
            }
        }
    }

    override fun close() {
        document.close()
        pageCache.clear()
    }

    companion object {
        private const val DEFAULT_CACHE_BYTES = 64L * 1024 * 1024

        fun open(file: File): ReaderBook {
            val document = MuPdfDocument.open(file)
            try {
                if (document.needsPassword) {
                    throw DocumentOpenException.PasswordRequired()
                }
                if (document.pageCount < 1) {
                    throw DocumentOpenException.NotReadable(file.path)
                }
                return ReaderBook(document)
            } catch (failure: Exception) {
                document.close()
                throw failure
            }
        }
    }
}

private class PageBitmapCache(maxBytes: Long) {

    private val delegate = object : LruCache<Int, Bitmap>(cacheCapacity(maxBytes)) {
        override fun sizeOf(key: Int, value: Bitmap): Int = value.byteCount.coerceAtLeast(1)
    }

    fun get(pageIndex: Int): Bitmap? = delegate.get(pageIndex)

    fun put(pageIndex: Int, bitmap: Bitmap) {
        delegate.put(pageIndex, bitmap)
    }

    fun clear() {
        delegate.evictAll()
    }

    private companion object {
        fun cacheCapacity(maxBytes: Long): Int = maxBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
