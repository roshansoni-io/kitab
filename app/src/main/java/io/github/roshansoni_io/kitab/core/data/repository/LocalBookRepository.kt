package io.github.roshansoni_io.kitab.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.platform.BookCoverRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

class LocalBookRepository(
    context: Context,
    private val coverRenderer: BookCoverRenderer,
) : BookRepository {
    private val appContext = context.applicationContext
    private val booksDir get() = File(appContext.filesDir, BOOKS_DIR)
    private val thumbnailsDir get() = File(appContext.filesDir, THUMBNAILS_DIR)
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val bookJson =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    private val mutationMutex = Mutex()
    private val initializationMutex = Mutex()
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    override val books: StateFlow<List<Book>> = _books.asStateFlow()
    private var initialized = false

    override suspend fun initialize() {
        initializationMutex.withLock {
            if (initialized) return
            val loadedBooks = withContext(Dispatchers.IO) { readBooks() }
            _books.value = loadedBooks
            initialized = true
        }
    }

    override suspend fun addBook(uri: String): Book? =
        withContext(Dispatchers.IO) {
            initialize()
            withBookLock {
                val existing = findBook(uri)
                val displayName = resolveDisplayName(Uri.parse(uri))
                val title = existing?.title ?: displayName?.let(::displayTitle) ?: DEFAULT_BOOK_TITLE
                val stored = resolveStoredFile(uri, existing, displayName) ?: return@withBookLock null

                val book =
                    updateBooks { currentBooks ->
                        val current = currentBooks.firstOrNull { it.uri == uri } ?: existing
                        val updated =
                            (current ?: Book(uri = uri, cachedPath = stored.path, title = title)).copy(
                                cachedPath = stored.path,
                                title = title,
                                inLibrary = true,
                            )
                        listOf(updated) + currentBooks.filterNot { it.uri == uri }
                    }.first { it.uri == uri }
                createThumbnail(book)
                book
            }
        }

    override suspend fun openBook(uri: String): Book? =
        withContext(Dispatchers.IO) {
            initialize()
            withBookLock {
                val existing = findBook(uri)
                val displayName = resolveDisplayName(Uri.parse(uri))
                val title = existing?.title ?: displayName?.let(::displayTitle) ?: DEFAULT_BOOK_TITLE
                val stored = resolveStoredFile(uri, existing, displayName) ?: return@withBookLock null

                updateBooks { currentBooks ->
                    val current = currentBooks.firstOrNull { it.uri == uri } ?: existing
                    val updated =
                        (current ?: Book(uri = uri, cachedPath = stored.path, title = title)).copy(
                            cachedPath = stored.path,
                            title = title,
                        )
                    listOf(updated) + currentBooks.filterNot { it.uri == uri }
                }.first { it.uri == uri }
            }
        }

    override suspend fun updateProgress(
        uri: String,
        lastPage: Int,
        pageCount: Int,
    ): Unit =
        withContext(Dispatchers.IO) {
            initialize()
            withBookLock {
                updateBooks { currentBooks ->
                    currentBooks.map { book ->
                        if (book.uri == uri) {
                            book.copy(
                                lastPage = lastPage.coerceAtLeast(0),
                                pageCount = pageCount.coerceAtLeast(0),
                                lastOpenedAt = System.currentTimeMillis(),
                            )
                        } else {
                            book
                        }
                    }
                }
            }
        }

    override suspend fun updateEpubProgress(
        uri: String,
        locatorJson: String,
        progression: Double,
    ): Unit =
        withContext(Dispatchers.IO) {
            initialize()
            withBookLock {
                updateBooks { currentBooks ->
                    currentBooks.map { book ->
                        if (book.uri == uri) {
                            book.copy(
                                epubLocator = locatorJson,
                                epubProgress = progression.coerceIn(0.0, 1.0),
                                lastOpenedAt = System.currentTimeMillis(),
                            )
                        } else {
                            book
                        }
                    }
                }
            }
        }

    override suspend fun setStarred(
        uri: String,
        starred: Boolean,
    ): Unit =
        withContext(Dispatchers.IO) {
            initialize()
            withBookLock {
                updateBooks { currentBooks ->
                    currentBooks.map { book ->
                        if (book.uri == uri) book.copy(starred = starred) else book
                    }
                }
            }
        }

    override suspend fun removeBook(uri: String): Unit =
        withContext(Dispatchers.IO) {
            initialize()
            withBookLock {
                val book = findBook(uri) ?: return@withBookLock
                updateBooks { currentBooks -> currentBooks.filterNot { it.uri == uri } }
                book.cachedPath
                    .let(::File)
                    .takeIf { isInside(it, booksDir) }
                    ?.delete()
                thumbnailFile(book).delete()
            }
        }

    fun thumbnailFile(book: Book): File {
        thumbnailsDir.mkdirs()
        return File(thumbnailsDir, "${stableId(book.uri)}.$THUMBNAIL_EXTENSION")
    }

    override suspend fun loadCover(book: Book): Bitmap? =
        withContext(Dispatchers.IO) {
            initialize()
            withBookLock {
                val current = findBook(book.uri) ?: book
                val displayName = resolveDisplayName(Uri.parse(current.uri))
                val privateFile = resolveStoredFile(current.uri, current, displayName) ?: return@withBookLock null
                val updatedBook = current.copy(cachedPath = privateFile.path)
                if (updatedBook.cachedPath != current.cachedPath) {
                    updateBooks { currentBooks ->
                        currentBooks.map { if (it.uri == book.uri) it.copy(cachedPath = privateFile.path) else it }
                    }
                }
                val thumbnail = thumbnailFile(updatedBook)
                coverRenderer.loadOrCreateThumbnail(
                    pdfFile = File(updatedBook.cachedPath),
                    thumbnailFile = thumbnail,
                    targetWidthPx = THUMBNAIL_WIDTH_PX,
                )
            }
        }

    private fun resolveStoredFile(
        uri: String,
        existing: Book?,
        displayName: String?,
    ): File? {
        val cachedFile = existing?.cachedPath?.let(::File)
        val extension = extensionOf(displayName) ?: cachedFile?.takeIf(File::isFile)?.let(::detectExtension)
        val cachedExtensionMatches =
            extension == null || cachedFile?.extension.equals(extension, ignoreCase = true)
        if (cachedFile?.isFile == true && isInside(cachedFile, booksDir) && cachedExtensionMatches) {
            return cachedFile
        }
        val copyName =
            displayName
                ?: "${existing?.title ?: DEFAULT_BOOK_TITLE}.${extension ?: PDF_EXTENSION}"
        val copied =
            cachedFile?.takeIf(File::isFile)?.let { source ->
                copyFile(source, copyName)?.also {
                    if (isInside(source, booksDir)) source.delete()
                }
            }
        return copied ?: copyUri(Uri.parse(uri), copyName)
    }

    private fun detectExtension(file: File): String? =
        try {
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                if (input.read(header) < header.size) {
                    null
                } else {
                    when {
                        header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte() -> EPUB_EXTENSION
                        String(header, Charsets.US_ASCII) == "%PDF" -> PDF_EXTENSION
                        else -> null
                    }
                }
            }
        } catch (_: Exception) {
            null
        }

    private fun createThumbnail(book: Book): Boolean =
        coverRenderer.saveThumbnail(
            pdfFile = File(book.cachedPath),
            thumbnailFile = thumbnailFile(book),
            targetWidthPx = THUMBNAIL_WIDTH_PX,
        )

    private fun findBook(uri: String): Book? = _books.value.firstOrNull { it.uri == uri }

    private suspend fun updateBooks(transform: (List<Book>) -> List<Book>): List<Book> =
        transform(_books.value).also { updatedBooks ->
            prefs.edit().putString(BOOKS_KEY, encodeBooks(updatedBooks)).apply()
            _books.value = updatedBooks
        }

    private suspend fun <T> withBookLock(action: suspend () -> T): T = mutationMutex.withLock { action() }

    private fun queryDisplayName(uri: Uri): String? =
        try {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameColumn)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }

    private fun resolveDisplayName(uri: Uri): String? {
        val displayName = queryDisplayName(uri)
        if (extensionOf(displayName) != null) return displayName
        val mimeType = runCatching { appContext.contentResolver.getType(uri) }.getOrNull()
        val extension =
            when (mimeType) {
                EPUB_MIME_TYPE -> EPUB_EXTENSION
                PDF_MIME_TYPE -> PDF_EXTENSION
                else -> return displayName
            }
        val base =
            displayName
                ?.substringBeforeLast('.', displayName)
                ?.takeIf(String::isNotBlank)
                ?: DEFAULT_BOOK_TITLE
        return "$base.$extension"
    }

    private fun displayTitle(displayName: String): String = displayName.substringBeforeLast('.', displayName).ifBlank { DEFAULT_BOOK_TITLE }

    private fun extensionOf(displayName: String?): String? =
        displayName
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.takeIf { it in SUPPORTED_BOOK_EXTENSIONS }

    private fun copyUri(
        uri: Uri,
        displayName: String,
    ): File? =
        copyToBooks(displayName) { destination ->
            val input =
                try {
                    appContext.contentResolver.openInputStream(uri)
                } catch (_: Exception) {
                    null
                } ?: throw IOException("Could not read the selected book.")
            input.use { stream -> destination.outputStream().use(stream::copyTo) }
        }

    private fun copyFile(
        source: File,
        displayName: String,
    ): File? =
        copyToBooks(displayName) { destination ->
            FileInputStream(source).use { input ->
                destination.outputStream().use(input::copyTo)
            }
        }

    private inline fun copyToBooks(
        displayName: String,
        copy: (File) -> Unit,
    ): File? {
        if (!booksDir.exists() && !booksDir.mkdirs()) return null
        val destination = File(booksDir, fileName(displayName))
        return try {
            copy(destination)
            destination
        } catch (_: Exception) {
            destination.delete()
            null
        }
    }

    private fun fileName(displayName: String): String {
        val base =
            displayName
                .replace(ILLEGAL_FILE_NAME_CHARACTERS, "_")
                .trim()
                .take(MAX_FILE_NAME_LENGTH)
                .ifBlank { DEFAULT_BOOK_TITLE }
        val extension = extensionOf(displayName) ?: PDF_EXTENSION
        val nameWithoutExtension = base.substringBeforeLast('.', base)
        return "$nameWithoutExtension-${UUID.randomUUID()}.$extension"
    }

    private fun isInside(
        file: File,
        directory: File,
    ): Boolean =
        runCatching {
            file.canonicalPath.startsWith(directory.canonicalPath + File.separator)
        }.getOrDefault(false)

    private fun readBooks(): List<Book> {
        val books = decodeBooks(prefs.getString(BOOKS_KEY, null))
        val legacyProgress = appContext.getSharedPreferences(PROGRESS_PREFS_NAME, Context.MODE_PRIVATE)
        if (legacyProgress.all.isEmpty()) return books
        val migrated =
            books.map { book ->
                val key = "${stableId(book.uri)}_"
                if (legacyProgress.contains("$key$PAGE_SUFFIX")) {
                    book.copy(
                        lastPage = legacyProgress.getInt("$key$PAGE_SUFFIX", book.lastPage),
                        pageCount = legacyProgress.getInt("$key$COUNT_SUFFIX", book.pageCount),
                        lastOpenedAt = legacyProgress.getLong("$key$OPENED_AT_SUFFIX", book.lastOpenedAt),
                    )
                } else {
                    book
                }
            }
        prefs.edit().putString(BOOKS_KEY, encodeBooks(migrated)).commit()
        legacyProgress.edit().clear().commit()
        return migrated
    }

    private fun decodeBooks(json: String?): List<Book> {
        val entries =
            try {
                bookJson.parseToJsonElement(json ?: "[]") as? JsonArray ?: return emptyList()
            } catch (_: SerializationException) {
                return emptyList()
            }
        return entries.mapNotNull { entry ->
            runCatching { bookJson.decodeFromJsonElement<Book>(entry) }
                .getOrNull()
                ?.takeIf { it.uri.isNotBlank() && it.cachedPath.isNotBlank() }
                ?.let { book ->
                    book.copy(
                        title = book.title.takeIf(String::isNotBlank) ?: DEFAULT_BOOK_TITLE,
                        lastPage = book.lastPage.coerceAtLeast(0),
                        pageCount = book.pageCount.coerceAtLeast(0),
                        epubProgress = book.epubProgress?.coerceIn(0.0, 1.0),
                        lastOpenedAt = book.lastOpenedAt.coerceAtLeast(0),
                    )
                }
        }
    }

    private fun encodeBooks(books: List<Book>): String = bookJson.encodeToString(books)

    private fun stableId(value: String): String =
        // Keep existing thumbnail names stable and avoid collisions across URI values.
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private companion object {
        const val PREFS_NAME = "kitab_books"
        const val PROGRESS_PREFS_NAME = "kitab_book_progress"
        const val BOOKS_KEY = "recent_books"
        const val BOOKS_DIR = "books"
        const val THUMBNAILS_DIR = "thumbnails"
        const val PDF_EXTENSION = "pdf"
        const val EPUB_EXTENSION = "epub"
        const val PDF_MIME_TYPE = "application/pdf"
        const val EPUB_MIME_TYPE = "application/epub+zip"
        val SUPPORTED_BOOK_EXTENSIONS = setOf(PDF_EXTENSION, EPUB_EXTENSION)
        const val THUMBNAIL_EXTENSION = "png"
        const val THUMBNAIL_WIDTH_PX = 300
        const val DEFAULT_BOOK_TITLE = "Book"
        const val MAX_FILE_NAME_LENGTH = 48
        const val PAGE_SUFFIX = "page"
        const val COUNT_SUFFIX = "count"
        const val OPENED_AT_SUFFIX = "opened_at"
        val ILLEGAL_FILE_NAME_CHARACTERS = Regex("[^A-Za-z0-9 ._-]")
    }
}
