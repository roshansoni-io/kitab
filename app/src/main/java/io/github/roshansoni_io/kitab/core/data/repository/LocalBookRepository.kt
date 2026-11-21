package io.github.roshansoni_io.kitab.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import io.github.roshansoni_io.kitab.core.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class LocalBookRepository(
    context: Context,
    private val coverRenderer: PdfCoverRenderer,
) : BookRepository {
    private val appContext = context.applicationContext
    private val libraryDir get() = File(Environment.getExternalStorageDirectory(), LIBRARY_DIR)
    private val openedDir get() = File(appContext.filesDir, OPENED_DIR)
    private val coversDir get() = File(appContext.filesDir, COVERS_DIR)
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val coverMutex = Mutex()
    private val booksMutex = Mutex()
    private val _books = MutableStateFlow(readBooks())
    override val books: StateFlow<List<Book>> = _books.asStateFlow()

    override suspend fun addBook(uri: String): Book? =
        withContext(Dispatchers.IO) {
            val documentUri = Uri.parse(uri)
            val displayName = queryDisplayName(documentUri) ?: DEFAULT_BOOK_TITLE
            val existing = findBook(uri)
            val stored =
                copyTo(documentUri, libraryDir, displayName)
                    ?: return@withContext null
            existing
                ?.cachedPath
                ?.let(::File)
                ?.takeIf { it.exists() && it.path != stored.path }
                ?.delete()
            val book =
                (existing ?: Book(uri = uri, cachedPath = stored.path, title = displayName))
                    .copy(
                        cachedPath = stored.path,
                        title = displayName,
                        inLibrary = true,
                    )
            prependRecent(book)
            book
        }

    override suspend fun openBook(uri: String): Book? =
        withContext(Dispatchers.IO) {
            findBook(uri)?.let { return@withContext it }
            val documentUri = Uri.parse(uri)
            val displayName = queryDisplayName(documentUri) ?: DEFAULT_BOOK_TITLE
            val stored =
                copyTo(documentUri, openedDir, displayName)
                    ?: return@withContext null
            val book =
                Book(
                    uri = uri,
                    cachedPath = stored.path,
                    title = displayName,
                    inLibrary = false,
                )
            prependRecent(book)
            book
        }

    override suspend fun updateProgress(
        uri: String,
        lastPage: Int,
        pageCount: Int,
    ): Unit =
        withContext(Dispatchers.IO) {
            updateBooks { currentBooks ->
                currentBooks.map { book ->
                    if (book.uri == uri) {
                        book.copy(
                            lastPage = lastPage.coerceAtLeast(0),
                            pageCount = pageCount,
                            lastOpenedAt = System.currentTimeMillis(),
                        )
                    } else {
                        book
                    }
                }
            }
        }

    override suspend fun setStarred(
        uri: String,
        starred: Boolean,
    ): Unit =
        withContext(Dispatchers.IO) {
            updateBooks { currentBooks ->
                currentBooks.map { book ->
                    if (book.uri == uri) book.copy(starred = starred) else book
                }
            }
        }

    override suspend fun removeBook(uri: String): Unit =
        withContext(Dispatchers.IO) {
            val book = findBook(uri) ?: return@withContext
            File(book.cachedPath).takeIf { it.exists() }?.delete()
            coverFile(book).delete()
            updateBooks { currentBooks -> currentBooks.filterNot { it.uri == uri } }
        }

    fun coverFile(book: Book): File {
        coversDir.mkdirs()
        return File(coversDir, "${coverName(book)}.$COVER_EXTENSION")
    }

    override suspend fun loadCover(book: Book): Bitmap? =
        withContext(Dispatchers.IO) {
            coverMutex.withLock {
                val file = coverFile(book)
                if (!file.exists()) {
                    runCatching { renderCover(book, file) }
                        .onFailure { file.delete() }
                        .getOrNull()
                        ?: return@withLock null
                }
                BitmapFactory.decodeFile(file.path)
            }
        }

    private fun renderCover(
        book: Book,
        destination: File,
    ) {
        val bitmap = coverRenderer.renderFirstPage(File(book.cachedPath), COVER_WIDTH_PX) ?: return
        FileOutputStream(destination).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Could not write the book cover image."
            }
        }
    }

    private fun findBook(uri: String): Book? = _books.value.firstOrNull { it.uri == uri }

    private suspend fun updateBooks(transform: (List<Book>) -> List<Book>) {
        booksMutex.withLock {
            val updatedBooks = transform(_books.value)
            prefs.edit().putString(RECENTS_KEY, encodeBooks(updatedBooks)).apply()
            _books.value = updatedBooks
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) {
                val raw = cursor.getString(nameColumn)?.removeSuffix(".pdf")
                raw?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        }

    private fun copyTo(
        uri: Uri,
        directory: File,
        displayName: String,
    ): File? {
        directory.mkdirs()
        val destination = File(directory, fileName(displayName))
        return runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use(input::copyTo)
                destination
            } ?: throw IOException("Cannot open stream for $uri")
        }.onFailure {
            destination.delete()
        }.getOrNull()
    }

    private fun fileName(displayName: String): String {
        val base =
            displayName
                .replace(ILLEGAL_FILE_NAME_CHARACTERS, "_")
                .trim()
                .take(MAX_FILE_NAME_LENGTH)
                .ifBlank { DEFAULT_BOOK_TITLE }
        return "$base-${UUID.randomUUID().toString().take(FILE_ID_LENGTH)}.$PDF_EXTENSION"
    }

    private suspend fun prependRecent(book: Book) {
        updateBooks { currentBooks ->
            listOf(book) + currentBooks.filterNot { it.uri == book.uri }
        }
    }

    private fun readBooks(): List<Book> = decodeBooks(prefs.getString(RECENTS_KEY, null))

    private fun decodeBooks(json: String?): List<Book> {
        val entries = emptyList<Book>().toMutableList()
        val array = runCatching { JSONArray(json ?: "[]") }.getOrNull()
        if (array != null) {
            for (index in 0 until array.length()) {
                entries += decodeBook(array.optJSONObject(index))
            }
        }
        return entries
    }

    private fun decodeBook(entry: JSONObject?): Book =
        Book(
            uri = entry?.optString(BOOK_URI) ?: "",
            cachedPath = entry?.optString(BOOK_CACHED_PATH) ?: "",
            title = entry?.optString(BOOK_TITLE) ?: "",
            lastPage = entry?.optInt(BOOK_LAST_PAGE) ?: 0,
            pageCount = entry?.optInt(BOOK_PAGE_COUNT) ?: 0,
            lastOpenedAt = entry?.optLong(BOOK_LAST_OPENED_AT) ?: 0,
            inLibrary = entry?.optBoolean(BOOK_IN_LIBRARY) ?: false,
            starred = entry?.optBoolean(BOOK_STARRED) ?: false,
        )

    private fun encodeBooks(books: List<Book>): String =
        JSONArray()
            .apply {
                books.forEach { book ->
                    put(
                        JSONObject()
                            .put(BOOK_URI, book.uri)
                            .put(BOOK_CACHED_PATH, book.cachedPath)
                            .put(BOOK_TITLE, book.title)
                            .put(BOOK_LAST_PAGE, book.lastPage)
                            .put(BOOK_PAGE_COUNT, book.pageCount)
                            .put(BOOK_LAST_OPENED_AT, book.lastOpenedAt)
                            .put(BOOK_IN_LIBRARY, book.inLibrary)
                            .put(BOOK_STARRED, book.starred),
                    )
                }
            }.toString()

    private fun coverName(book: Book): String = Integer.toHexString(book.uri.hashCode())

    private companion object {
        const val PREFS_NAME = "kitab_books"
        const val RECENTS_KEY = "recent_books"
        const val LIBRARY_DIR = "Kitab"
        const val OPENED_DIR = "opened"
        const val COVERS_DIR = "covers"
        const val PDF_EXTENSION = "pdf"
        const val COVER_EXTENSION = "png"
        const val COVER_WIDTH_PX = 300
        const val DEFAULT_BOOK_TITLE = "Book"
        const val MAX_FILE_NAME_LENGTH = 48
        const val FILE_ID_LENGTH = 8
        val ILLEGAL_FILE_NAME_CHARACTERS = Regex("[^A-Za-z0-9 ._-]")
        const val BOOK_URI = "uri"
        const val BOOK_CACHED_PATH = "cachedPath"
        const val BOOK_TITLE = "title"
        const val BOOK_LAST_PAGE = "lastPage"
        const val BOOK_PAGE_COUNT = "pageCount"
        const val BOOK_LAST_OPENED_AT = "lastOpenedAt"
        const val BOOK_IN_LIBRARY = "inLibrary"
        const val BOOK_STARRED = "starred"
    }
}
