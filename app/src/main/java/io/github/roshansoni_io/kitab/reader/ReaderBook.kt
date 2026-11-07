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
        val cacheKey = PageCacheKey(pageIndex, targetWidthPx)
        pageCache.get(cacheKey)?.let { return it }
        return withContext(renderDispatcher) {
            pageCache.get(cacheKey)?.let { return@withContext it }
            trace("Kitab.pageRender") {
                document
                    .renderPage(pageIndex, targetWidthPx, backgroundColor)
                    .also { pageCache.put(cacheKey, it) }
            }
        }
    }

    override fun close() {
        document.close()
        pageCache.clear()
    }

    companion object {
        private const val DEFAULT_CACHE_BYTES = 96L * 1024 * 1024

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

private data class PageCacheKey(
    val pageIndex: Int,
    val targetWidthPx: Int,
)

private class PageBitmapCache(
    maxBytes: Long,
) {
    private val delegate =
        object : LruCache<PageCacheKey, Bitmap>(cacheCapacity(maxBytes)) {
            override fun sizeOf(
                key: PageCacheKey,
                value: Bitmap,
            ): Int = value.byteCount.coerceAtLeast(1)
        }

    fun get(key: PageCacheKey): Bitmap? = delegate.get(key)

    fun put(
        key: PageCacheKey,
        bitmap: Bitmap,
    ) {
        delegate.put(key, bitmap)
    }

    fun clear() {
        delegate.evictAll()
    }

    private companion object {
        fun cacheCapacity(maxBytes: Long): Int = maxBytes.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
