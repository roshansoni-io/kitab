package io.github.roshansoni_io.kitab.feature.reader.epub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.publication.services.PositionsService
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.util.Locale

internal data class ChapterRange(
    val hrefKey: String,
    val index: Int,
    val startProgression: Double,
    val endProgression: Double,
)

/**
 * An opened EPUB publication plus the collaborators the navigator fragment needs.
 * Kept in memory because [EpubNavigatorFactory] cannot be serialized for fragment
 * restoration.
 */
internal class EpubSession(
    val path: String,
    val publication: Publication,
    val navigatorFactory: EpubNavigatorFactory,
    initialPreferences: EpubPreferences,
    val initialLocator: Locator?,
    val chapterRanges: List<ChapterRange>,
    val totalPositions: Int,
) : Closeable {
    private val rangesByHref = chapterRanges.associateBy(ChapterRange::hrefKey)
    val isFixedLayout: Boolean = publication.metadata.presentation.layout == EpubLayout.FIXED

    @Volatile
    var preferences: EpubPreferences = initialPreferences

    @Volatile
    var navigator: EpubNavigatorFragment? = null

    @Volatile
    var onProgress: ((locatorJson: String, progression: Double) -> Unit)? = null

    @Volatile
    var onCenterTap: (() -> Unit)? = null

    @Volatile
    var onExternalLink: ((String) -> Unit)? = null

    @Volatile
    private var lastPageHrefKey: String? = null

    @Volatile
    private var lastPageIndex: Int? = null

    @Volatile
    private var lastTotalPages: Int? = null

    val locator: MutableStateFlow<Locator?> = MutableStateFlow(initialLocator)
    val progression: MutableStateFlow<Double> =
        MutableStateFlow(
            computeSmoothProgression(
                locator = initialLocator,
                pageIndex = null,
                totalPages = null,
            ),
        )

    fun onPageChanged(
        pageIndex: Int,
        totalPages: Int,
        currentLocator: Locator,
    ) {
        lastPageHrefKey = currentLocator.hrefKey()
        lastPageIndex = pageIndex
        lastTotalPages = totalPages
        locator.value = currentLocator
        progression.value =
            computeSmoothProgression(
                locator = currentLocator,
                pageIndex = pageIndex,
                totalPages = totalPages,
            )
    }

    fun onLocatorChanged(currentLocator: Locator) {
        val hrefKey = currentLocator.hrefKey()
        val matchingPageIndex = if (hrefKey == lastPageHrefKey) lastPageIndex else null
        val matchingTotalPages = if (hrefKey == lastPageHrefKey) lastTotalPages else null
        locator.value = currentLocator
        progression.value =
            computeSmoothProgression(
                locator = currentLocator,
                pageIndex = matchingPageIndex,
                totalPages = matchingTotalPages,
            )
    }

    fun publishProgress(currentLocator: Locator) {
        val currentHref = currentLocator.hrefKey()
        val storedHref = locator.value?.hrefKey()
        val savedProgression =
            if (currentLocator == locator.value || currentHref == storedHref) {
                progression.value
            } else {
                computeSmoothProgression(currentLocator, pageIndex = null, totalPages = null)
            }
        onProgress?.invoke(currentLocator.toJSON().toString(), savedProgression.coerceIn(0.0, 1.0))
    }

    suspend fun seekTo(targetProgression: Double): Boolean {
        val clamped = targetProgression.coerceIn(0.0, 1.0)
        val target =
            withContext(Dispatchers.IO) {
                resolveTargetLocator(clamped)
            } ?: return false
        val navigated =
            withContext(Dispatchers.Main.immediate) {
                navigator?.go(target) == true
            }
        if (navigated) {
            lastPageHrefKey = null
            lastPageIndex = null
            lastTotalPages = null
            locator.value = target
            progression.value = clamped
        }
        return navigated
    }

    private suspend fun resolveTargetLocator(clamped: Double): Locator? {
        val positionBase = publication.locateProgression(clamped)
        if (chapterRanges.isNotEmpty()) {
            val range =
                chapterRanges.lastOrNull { it.startProgression <= clamped }
                    ?: chapterRanges.first()
            val span = (range.endProgression - range.startProgression).coerceAtLeast(1e-6)
            val chapterProg = ((clamped - range.startProgression) / span).coerceIn(0.0, 1.0)
            if (positionBase != null && positionBase.hrefKey() == range.hrefKey) {
                return positionBase.copy(
                    locations =
                        positionBase.locations.copy(
                            progression = chapterProg,
                            totalProgression = clamped,
                        ),
                )
            }
            val link = publication.readingOrder.getOrNull(range.index)
            if (link != null) {
                return Locator(
                    href = link.url(),
                    mediaType = link.mediaType ?: MediaType.XHTML,
                    title = link.title,
                    locations =
                        Locator.Locations(
                            progression = chapterProg,
                            totalProgression = clamped,
                        ),
                )
            }
        }
        if (positionBase != null) return positionBase
        val readingOrder = publication.readingOrder
        if (readingOrder.isEmpty()) return null
        val index = (clamped * (readingOrder.size - 1)).toInt().coerceIn(0, readingOrder.lastIndex)
        val link = readingOrder[index]
        return Locator(
            href = link.url(),
            mediaType = link.mediaType ?: MediaType.XHTML,
            title = link.title,
            locations = Locator.Locations(totalProgression = clamped),
        )
    }

    private fun computeSmoothProgression(
        locator: Locator?,
        pageIndex: Int?,
        totalPages: Int?,
    ): Double {
        if (locator == null) return 0.0
        val hrefKey = locator.hrefKey()
        val range = rangesByHref[hrefKey]
        val chapterFraction =
            smoothChapterFraction(
                pageIndex = pageIndex,
                totalPages = totalPages,
                isLastChapter = range == null || range.index >= chapterRanges.lastIndex,
                locatorProgression = locator.locations.progression,
            )
        if (range != null && chapterFraction != null) {
            val span = (range.endProgression - range.startProgression).coerceAtLeast(0.0)
            return (range.startProgression + chapterFraction * span).coerceIn(0.0, 1.0)
        }
        locator.locations.totalProgression?.let { return it.coerceIn(0.0, 1.0) }
        val position = locator.locations.position
        if (position != null && totalPositions > 1) {
            return ((position - 1).toDouble() / (totalPositions - 1).toDouble()).coerceIn(0.0, 1.0)
        }
        if (range != null) return range.startProgression.coerceIn(0.0, 1.0)
        return 0.0
    }

    override fun close() {
        publication.close()
    }
}

internal object EpubSessionRegistry {
    private val lock = Any()
    private var session: EpubSession? = null

    fun current(path: String): EpubSession? =
        synchronized(lock) {
            session?.takeIf { it.path == path }
        }

    fun install(newSession: EpubSession) {
        synchronized(lock) {
            if (session !== newSession) {
                session?.close()
            }
            session = newSession
        }
    }

    fun clear(path: String): EpubSession? =
        synchronized(lock) {
            session
                ?.takeIf { it.path == path }
                ?.also { session = null }
        }
}

private object EpubReaderClients {
    @Volatile
    private var assetRetriever: AssetRetriever? = null

    @Volatile
    private var publicationOpener: PublicationOpener? = null

    fun get(context: Context): Pair<AssetRetriever, PublicationOpener> {
        val existingRetriever = assetRetriever
        val existingOpener = publicationOpener
        if (existingRetriever != null && existingOpener != null) {
            return existingRetriever to existingOpener
        }
        return synchronized(this) {
            val retriever = assetRetriever
            val opener = publicationOpener
            if (retriever != null && opener != null) {
                retriever to opener
            } else {
                val appContext = context.applicationContext
                val httpClient = DefaultHttpClient()
                val newRetriever = AssetRetriever(appContext.contentResolver, httpClient)
                val newOpener =
                    PublicationOpener(
                        publicationParser =
                            DefaultPublicationParser(
                                appContext,
                                httpClient,
                                newRetriever,
                                pdfFactory = null,
                            ),
                    )
                assetRetriever = newRetriever
                publicationOpener = newOpener
                newRetriever to newOpener
            }
        }
    }
}

private class CachingPositionsService(
    private val delegate: PositionsService?,
    private val cacheFile: File,
) : PositionsService {
    private val mutex = Mutex()

    @Volatile
    private var memoryCache: List<List<Locator>>? = null

    override suspend fun positionsByReadingOrder(): List<List<Locator>> {
        memoryCache?.let { return it }
        return mutex.withLock {
            memoryCache?.let { return@withLock it }
            val diskLoaded =
                withContext(Dispatchers.IO) {
                    readPositionsCache(cacheFile)
                }
            if (diskLoaded != null) {
                memoryCache = diskLoaded
                return@withLock diskLoaded
            }
            val computed = delegate?.positionsByReadingOrder().orEmpty()
            if (computed.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    writePositionsCache(cacheFile, computed)
                }
            }
            memoryCache = computed
            computed
        }
    }

    override suspend fun positions(): List<Locator> = positionsByReadingOrder().flatten()

    override fun close() {
        delegate?.close()
    }
}

internal suspend fun openEpubSession(
    context: Context,
    path: String,
    preferences: EpubPreferences,
    initialLocator: Locator?,
): EpubSession =
    withContext(Dispatchers.IO) {
        val (assetRetriever, publicationOpener) = EpubReaderClients.get(context)
        val file = File(path)
        val cacheFile = positionsCacheFile(context, file)
        val asset =
            assetRetriever.retrieve(file).getOrElse { failure ->
                throw EpubOpenException(path, failure.toString())
            }
        val publication =
            publicationOpener
                .open(
                    asset = asset,
                    allowUserInteraction = false,
                    onCreatePublication = {
                        servicesBuilder.decorate(PositionsService::class) { factory ->
                            { serviceContext ->
                                CachingPositionsService(
                                    delegate = factory?.invoke(serviceContext) as? PositionsService,
                                    cacheFile = cacheFile,
                                )
                            }
                        }
                    },
                ).getOrElse { failure ->
                    throw EpubOpenException(path, failure.toString())
                }
        try {
            currentCoroutineContext().ensureActive()
            val positionsByOrder = publication.positionsByReadingOrder()
            currentCoroutineContext().ensureActive()
            val chapterRanges = buildChapterRanges(publication, positionsByOrder)
            val totalPositions = positionsByOrder.sumOf { it.size }
            EpubSession(
                path = path,
                publication = publication,
                navigatorFactory = EpubNavigatorFactory(publication),
                initialPreferences = preferences,
                initialLocator = initialLocator,
                chapterRanges = chapterRanges,
                totalPositions = totalPositions,
            )
        } catch (error: Throwable) {
            publication.close()
            throw error
        }
    }

internal fun smoothChapterFraction(
    pageIndex: Int?,
    totalPages: Int?,
    isLastChapter: Boolean,
    locatorProgression: Double?,
): Double? =
    when {
        pageIndex != null && totalPages != null && totalPages > 0 -> {
            when {
                totalPages == 1 -> 0.0
                !isLastChapter -> pageIndex.toDouble() / totalPages.toDouble()
                else -> pageIndex.toDouble() / (totalPages - 1).toDouble()
            }
        }

        else -> {
            locatorProgression
        }
    }?.coerceIn(0.0, 1.0)

internal fun buildChapterRanges(
    publication: Publication,
    positionsByOrder: List<List<Locator>>,
): List<ChapterRange> =
    buildChapterRanges(
        hrefKeys = publication.readingOrder.map { it.url().removeFragment().toString() },
        positionStarts =
            List(publication.readingOrder.size) { i ->
                positionsByOrder
                    .getOrNull(i)
                    ?.firstOrNull()
                    ?.locations
                    ?.totalProgression
            },
    )

internal fun buildChapterRanges(
    hrefKeys: List<String>,
    positionStarts: List<Double?>,
): List<ChapterRange> {
    if (hrefKeys.isEmpty()) return emptyList()
    val count = hrefKeys.size
    val hasTotalProgressions = positionStarts.any { it != null }
    var lastStart = 0.0
    val starts =
        DoubleArray(count) { i ->
            if (!hasTotalProgressions) {
                i.toDouble() / count.toDouble()
            } else {
                val positionStart = positionStarts.getOrNull(i)?.coerceIn(0.0, 1.0)
                val nextStart = (positionStart ?: lastStart).coerceAtLeast(lastStart)
                lastStart = nextStart
                nextStart
            }
        }
    return List(count) { i ->
        val end = if (i < count - 1) starts[i + 1] else 1.0
        ChapterRange(
            hrefKey = hrefKeys[i],
            index = i,
            startProgression = starts[i],
            endProgression = end.coerceAtLeast(starts[i]),
        )
    }
}

private fun positionsCacheFile(
    context: Context,
    epubFile: File,
): File {
    val dir = File(context.applicationContext.cacheDir, "epub_positions")
    dir.mkdirs()
    val key = "${epubFile.absolutePath}:${epubFile.length()}:${epubFile.lastModified()}"
    val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
    val hex = digest.joinToString("") { byte -> "%02x".format(Locale.ROOT, byte) }
    return File(dir, "$hex.json")
}

private fun readPositionsCache(file: File): List<List<Locator>>? {
    if (!file.isFile) return null
    return runCatching {
        val outer = JSONArray(file.readText())
        List(outer.length()) { i ->
            val inner = outer.getJSONArray(i)
            List(inner.length()) { j ->
                Locator.fromJSON(inner.getJSONObject(j))
                    ?: error("Invalid cached locator")
            }
        }
    }.getOrNull()
}

private fun writePositionsCache(
    file: File,
    positions: List<List<Locator>>,
) {
    runCatching {
        val outer = JSONArray()
        for (chapter in positions) {
            val inner = JSONArray()
            for (locator in chapter) {
                inner.put(locator.toJSON())
            }
            outer.put(inner)
        }
        val parent = file.parentFile ?: return
        val tmp = File.createTempFile("${file.name}.", ".tmp", parent)
        try {
            tmp.writeText(outer.toString())
            if (!tmp.renameTo(file)) {
                file.writeText(outer.toString())
            }
        } finally {
            tmp.delete()
        }
    }
}

internal class EpubOpenException(
    path: String,
    reason: String,
) : Exception("Cannot open EPUB at $path ($reason)")

internal fun parseLocator(json: String?): Locator? =
    json
        ?.takeIf(String::isNotBlank)
        ?.let { runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull() }

private fun Locator.hrefKey(): String = href.removeFragment().toString()
