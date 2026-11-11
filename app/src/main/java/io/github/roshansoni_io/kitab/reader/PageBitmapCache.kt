package io.github.roshansoni_io.kitab.reader

import android.graphics.Bitmap
import android.util.LruCache

internal data class PageCacheKey(
    val pageIndex: Int,
    val targetWidthPx: Int,
    val backgroundColor: Int,
)

internal class PageBitmapCache(
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
