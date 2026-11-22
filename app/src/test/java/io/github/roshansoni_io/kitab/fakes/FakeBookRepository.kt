package io.github.roshansoni_io.kitab.fakes

import android.graphics.Bitmap
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBookRepository : BookRepository {
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    override val books: StateFlow<List<Book>> = _books.asStateFlow()
    val removedUris = mutableListOf<String>()
    val progressUpdates = mutableListOf<Triple<String, Int, Int>>()

    var addResult: Book? = null
    var openResult: Book? = null
    var cover: Bitmap? = null

    override suspend fun initialize() = Unit

    fun addInitialBooks(vararg initialBooks: Book) {
        _books.value = _books.value + initialBooks
    }

    override suspend fun addBook(uri: String): Book? {
        addResult?.let { addedBook ->
            _books.value = listOf(addedBook) + _books.value.filterNot { it.uri == addedBook.uri }
        }
        return addResult
    }

    override suspend fun openBook(uri: String): Book? {
        openResult?.let { openedBook ->
            _books.value = listOf(openedBook) + _books.value.filterNot { it.uri == openedBook.uri }
        }
        return openResult
    }

    override suspend fun updateProgress(
        uri: String,
        lastPage: Int,
        pageCount: Int,
    ) {
        progressUpdates += Triple(uri, lastPage, pageCount)
        replace(uri) { it.copy(lastPage = lastPage, pageCount = pageCount) }
    }

    override suspend fun setStarred(
        uri: String,
        starred: Boolean,
    ) {
        replace(uri) { it.copy(starred = starred) }
    }

    override suspend fun removeBook(uri: String) {
        removedUris += uri
        _books.value = _books.value.filterNot { it.uri == uri }
    }

    override suspend fun loadCover(book: Book): Bitmap? = cover

    private fun replace(
        uri: String,
        transform: (Book) -> Book,
    ) {
        val currentBooks = _books.value
        val index = currentBooks.indexOfFirst { it.uri == uri }
        if (index >= 0) {
            _books.value = currentBooks.toMutableList().also { updatedBooks ->
                updatedBooks[index] = transform(currentBooks[index])
            }
        }
    }
}
