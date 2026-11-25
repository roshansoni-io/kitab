package io.github.roshansoni_io.kitab.core.data.repository

import android.graphics.Bitmap
import io.github.roshansoni_io.kitab.core.data.model.Book
import kotlinx.coroutines.flow.StateFlow

interface BookRepository {
    val books: StateFlow<List<Book>>

    suspend fun initialize()

    suspend fun addBook(uri: String): Book?

    suspend fun openBook(uri: String): Book?

    suspend fun updateProgress(
        uri: String,
        lastPage: Int,
        pageCount: Int,
    )

    suspend fun updateEpubProgress(
        uri: String,
        locatorJson: String,
        progression: Double,
    )

    suspend fun removeBook(uri: String)

    suspend fun removeFromLibrary(uri: String)

    suspend fun renameBook(
        uri: String,
        newTitle: String,
    )

    suspend fun resetProgress(uri: String)

    suspend fun markCompleted(uri: String)

    suspend fun clearRecentHistory()

    suspend fun deleteBookFile(uri: String)

    suspend fun loadCover(book: Book): Bitmap?
}
