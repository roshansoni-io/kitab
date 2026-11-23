package io.github.roshansoni_io.kitab.feature.reader.epub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.locateProgression
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.Closeable
import java.io.File

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
) : Closeable {
    @Volatile
    var preferences: EpubPreferences = initialPreferences

    @Volatile
    var navigator: EpubNavigatorFragment? = null

    @Volatile
    var onProgress: ((Locator) -> Unit)? = null

    @Volatile
    var onCenterTap: (() -> Unit)? = null

    @Volatile
    var onExternalLink: ((String) -> Unit)? = null

    val locator: MutableStateFlow<Locator?> = MutableStateFlow(null)

    suspend fun seekTo(progression: Double) {
        val target = publication.locateProgression(progression.coerceIn(0.0, 1.0)) ?: return
        navigator?.go(target)
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

internal suspend fun openEpubSession(
    context: Context,
    path: String,
    preferences: EpubPreferences,
    initialLocator: Locator?,
): EpubSession =
    withContext(Dispatchers.IO) {
        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
        val publicationOpener =
            PublicationOpener(
                publicationParser =
                    DefaultPublicationParser(
                        context,
                        httpClient,
                        assetRetriever,
                        pdfFactory = null,
                    ),
            )
        val asset =
            assetRetriever.retrieve(File(path)).getOrElse { failure ->
                throw EpubOpenException(path, failure.toString())
            }
        val publication =
            publicationOpener.open(asset, allowUserInteraction = false).getOrElse { failure ->
                throw EpubOpenException(path, failure.toString())
            }
        try {
            currentCoroutineContext().ensureActive()
            EpubSession(
                path = path,
                publication = publication,
                navigatorFactory = EpubNavigatorFactory(publication),
                initialPreferences = preferences,
                initialLocator = initialLocator,
            )
        } catch (error: Throwable) {
            publication.close()
            throw error
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
