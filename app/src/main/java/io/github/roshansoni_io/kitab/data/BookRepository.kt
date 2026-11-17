package io.github.roshansoni_io.kitab.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import io.github.roshansoni_io.kitab.reader.MuPdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class BookRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val libraryDir get() = File(Environment.getExternalStorageDirectory(), LIBRARY_DIR)
    private val openedDir get() = File(appContext.filesDir, OPENED_DIR)
    private val coversDir get() = File(appContext.filesDir, COVERS_DIR)
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val coverMutex = Mutex()

    suspend fun addBook(uri: Uri): Book? =
        withContext(Dispatchers.IO) {
            val displayName = queryDisplayName(uri) ?: DEFAULT_BOOK_TITLE
            val existing = findBook(uri.toString())
            val stored =
                copyTo(uri, libraryDir, displayName)
                    ?: return@withContext null
            existing
                ?.cachedPath
                ?.let(::File)
                ?.takeIf { it.exists() && it.path != stored.path }
                ?.delete()
            val book =
                (existing ?: Book(uri = uri.toString(), cachedPath = stored.path, title = displayName))
                    .copy(
                        cachedPath = stored.path,
                        title = displayName,
                        inLibrary = true,
                    )
            prependRecent(book)
            book
        }

    suspend fun openBook(uri: Uri): Book? =
        withContext(Dispatchers.IO) {
            findBook(uri.toString())?.let { return@withContext it }
            val displayName = queryDisplayName(uri) ?: DEFAULT_BOOK_TITLE
            val stored =
                copyTo(uri, openedDir, displayName)
                    ?: return@withContext null
            val book =
                Book(
                    uri = uri.toString(),
                    cachedPath = stored.path,
                    title = displayName,
                    inLibrary = false,
                )
            prependRecent(book)
            book
        }

    fun recentBooks(): List<Book> = decodeBooks(prefs.getString(RECENTS_KEY, null))

    fun libraryBooks(): List<Book> = recentBooks().filter(Book::inLibrary)

    suspend fun updateProgress(
        uri: String,
        lastPage: Int,
        pageCount: Int,
    ): Unit =
        withContext(Dispatchers.IO) {
            replaceBook(uri) { book ->
                book.copy(
                    lastPage = lastPage.coerceAtLeast(0),
                    pageCount = pageCount,
                    lastOpenedAt = System.currentTimeMillis(),
                )
            }
        }

    suspend fun removeBook(uri: String): Unit =
        withContext(Dispatchers.IO) {
            val book = findBook(uri) ?: return@withContext
            File(book.cachedPath).takeIf { it.exists() }?.delete()
            coverFile(book).delete()
            replaceBook(uri) { null }
        }

    fun coverFile(book: Book): File {
        coversDir.mkdirs()
        return File(coversDir, "${coverName(book)}.$COVER_EXTENSION")
    }

    suspend fun loadCover(book: Book): Bitmap? =
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
        val document = MuPdfDocument.open(File(book.cachedPath))
        try {
            if (document.pageCount < 1) return
            document
                .renderPage(pageIndex = 0, targetWidthPx = COVER_WIDTH_PX, backgroundColor = Color.WHITE)
                .compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(destination))
        } finally {
            document.close()
        }
    }

    private fun findBook(uri: String): Book? = recentBooks().firstOrNull { it.uri == uri }

    private val prefsMutex = Mutex()

    private suspend fun replaceBook(
        uri: String,
        transform: (Book) -> Book?,
    ) {
        prefsMutex.withLock {
            recentBooks()
                .mapNotNull { book -> if (book.uri == uri) transform(book) else book }
                .let(::encodeBooks)
                .let { prefs.edit().putString(RECENTS_KEY, it).apply() }
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
        prefsMutex.withLock {
            (listOf(book) + recentBooks().filterNot { it.uri == book.uri })
                .let(::encodeBooks)
                .let { prefs.edit().putString(RECENTS_KEY, it).apply() }
        }
    }

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
                            .put(BOOK_IN_LIBRARY, book.inLibrary),
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
    }
}
