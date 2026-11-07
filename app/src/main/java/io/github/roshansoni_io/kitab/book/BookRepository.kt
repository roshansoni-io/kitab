package io.github.roshansoni_io.kitab.book

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

class BookRepository(context: Context) {

    private val appContext = context.applicationContext
    private val booksDir get() = File(appContext.filesDir, BOOKS_DIR)
    private val prefs get() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun importBook(uri: Uri): Book? = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: DEFAULT_BOOK_TITLE
        val cachedFile = copyToAppStorage(uri)
            ?: return@withContext null
        val book = Book(
            uri = uri.toString(),
            cachedPath = cachedFile.path,
            title = displayName,
        )
        prependRecent(book)
        book
    }

    fun recentBooks(): List<Book> =
        decodeBooks(prefs.getString(RECENTS_KEY, null))

    fun removeRecent(uri: String) {
        recentBooks()
            .filterNot { it.uri == uri }
            .let { encodeBooks(it) }
            .let { prefs.edit().putString(RECENTS_KEY, it).apply() }
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

    private fun copyToAppStorage(uri: Uri): File? {
        booksDir.mkdirs()
        val destination = File(booksDir, "${UUID.randomUUID()}.$PDF_EXTENSION")
        return runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use(input::copyTo)
                destination
            } ?: throw IOException("Cannot open stream for $uri")
        }.onFailure {
            destination.delete()
        }.getOrNull()
    }

    private fun prependRecent(book: Book) {
        recentBooks()
            .filterNot { it.uri == book.uri }
            .take(MAX_RECENTS - 1)
            .plus(book)
            .let { encodeBooks(it) }
            .let { prefs.edit().putString(RECENTS_KEY, it).apply() }
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
        )

    private fun encodeBooks(books: List<Book>): String = JSONArray().apply {
        books.forEach { book ->
            put(
                JSONObject()
                    .put(BOOK_URI, book.uri)
                    .put(BOOK_CACHED_PATH, book.cachedPath)
                    .put(BOOK_TITLE, book.title),
            )
        }
    }.toString()

    private companion object {
        const val PREFS_NAME = "kitab_books"
        const val RECENTS_KEY = "recent_books"
        const val BOOKS_DIR = "books"
        const val PDF_EXTENSION = "pdf"
        const val DEFAULT_BOOK_TITLE = "Book"
        const val MAX_RECENTS = 30
        const val BOOK_URI = "uri"
        const val BOOK_CACHED_PATH = "cachedPath"
        const val BOOK_TITLE = "title"
    }
}
