package io.github.roshansoni_io.kitab.reader

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.LruCache
import androidx.core.os.trace
import io.github.roshansoni_io.kitab.selection.PageText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class ReaderBook private constructor(
    private val document: MuPdfDocument,
) : Closeable {
    val pageCount: Int get() = document.pageCount

    private val closed = AtomicBoolean(false)

    private val pageCache = PageBitmapCache(maxBytes = DEFAULT_CACHE_BYTES)
    private val pageTextCache =
        object : LinkedHashMap<Int, PageText>(8, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, PageText>): Boolean = size > MAX_CACHED_TEXT_PAGES
        }
    private val linkTargetCache =
        object : LinkedHashMap<Int, List<LinkTarget>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<LinkTarget>>): Boolean = size > MAX_CACHED_LINK_PAGES
        }
    private val pageSizeCache = mutableMapOf<Int, RectF>()

    // All native MuPDF access is serialized onto this dispatcher.
    private val renderDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    // Runs native teardown after any in-flight render without blocking the caller.
    private val closeScope = CoroutineScope(SupervisorJob() + renderDispatcher)

    /**
     * Renders [pageIndex] at [targetWidthPx].
     *
     * The returned [Bitmap] is owned by the internal cache — callers must not
     * mutate pixels or call [Bitmap.recycle] on it.
     */
    suspend fun renderPage(
        pageIndex: Int,
        targetWidthPx: Int,
        backgroundColor: Int,
    ): Bitmap {
        ensureOpen()
        requireValidPage(pageIndex)
        val cacheKey = PageCacheKey(pageIndex, targetWidthPx, backgroundColor)
        pageCache.get(cacheKey)?.let { return it }
        return withContext(renderDispatcher) {
            ensureOpen()
            pageCache.get(cacheKey)?.let { return@withContext it }
            trace("Kitab.pageRender p$pageIndex w$targetWidthPx") {
                document
                    .renderPage(pageIndex, targetWidthPx, backgroundColor)
                    .also { pageCache.put(cacheKey, it) }
            }
        }
    }

    suspend fun pageSize(pageIndex: Int): RectF {
        ensureOpen()
        requireValidPage(pageIndex)
        return withContext(renderDispatcher) {
            ensureOpen()
            // RectF is mutable; hand back a copy so callers can't corrupt the cache.
            val cached = pageSizeCache.getOrPut(pageIndex) { document.pageSize(pageIndex) }
            RectF(cached)
        }
    }

    suspend fun renderRegion(
        pageIndex: Int,
        pageRect: RectF,
        zoom: Float,
        backgroundColor: Int,
    ): Bitmap {
        ensureOpen()
        requireValidPage(pageIndex)
        return withContext(renderDispatcher) {
            ensureOpen()
            document.renderRegion(pageIndex, pageRect, zoom, backgroundColor)
        }
    }

    suspend fun linkTargets(pageIndex: Int): List<LinkTarget> {
        ensureOpen()
        requireValidPage(pageIndex)
        return withContext(renderDispatcher) {
            ensureOpen()
            linkTargetCache.getOrPut(pageIndex) { document.linkTargets(pageIndex) }
        }
    }

    suspend fun pageText(pageIndex: Int): PageText {
        ensureOpen()
        requireValidPage(pageIndex)
        return withContext(renderDispatcher) {
            ensureOpen()
            pageTextCache.getOrPut(pageIndex) { document.pageText(pageIndex) }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        // Dispatched onto the serialized render dispatcher so the native document is
        // never closed while a render is using it. Queued renders observe `closed`
        // via ensureOpen() and bail out before touching the document.
        closeScope.launch {
            document.close()
            pageCache.clear()
            linkTargetCache.clear()
            pageTextCache.clear()
            pageSizeCache.clear()
        }
    }

    private fun ensureOpen() {
        check(!closed.get()) { "ReaderBook is closed" }
    }

    private fun requireValidPage(pageIndex: Int) {
        require(pageIndex in 0 until pageCount) {
            "Invalid page index $pageIndex (pageCount=$pageCount)"
        }
    }

    companion object {
        private const val DEFAULT_CACHE_BYTES = 96L * 1024 * 1024
        private const val MAX_CACHED_TEXT_PAGES = 32
        private const val MAX_CACHED_LINK_PAGES = 32

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
    val backgroundColor: Int,
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
        fun cacheCapacity(maxBytes: Long): Int {
            require(maxBytes in 1..Int.MAX_VALUE.toLong()) {
                "Cache size $maxBytes out of range [1, ${Int.MAX_VALUE}]"
            }
            return maxBytes.toInt()
        }
    }
}
