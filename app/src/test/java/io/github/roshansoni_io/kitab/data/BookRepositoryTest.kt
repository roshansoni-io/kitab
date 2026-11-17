package io.github.roshansoni_io.kitab.data

import android.app.Application
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class BookRepositoryTest {
    private val application = RuntimeEnvironment.getApplication() as Application
    private val repository = BookRepository(application)

    @Test
    fun addBook_stores_pdf_in_library_directory() = runBlocking {
        val book = addFakeBook()

        val stored = File(book.cachedPath)
        assertTrue(stored.exists())
        assertTrue(book.inLibrary)
        assertEquals(libraryDirectory(), stored.parentFile)
        assertEquals(listOf(book.uri), repository.libraryBooks().map(Book::uri))
    }

    @Test
    fun openBook_records_book_without_adding_it_to_library() = runBlocking {
        val book = openFakeBook(EXTERNAL_URI)

        assertFalse(book.inLibrary)
        assertTrue(File(book.cachedPath).exists())
        assertEquals(openedDirectory(), File(book.cachedPath).parentFile)
        assertEquals(listOf(book.uri), repository.recentBooks().map(Book::uri))
        assertTrue(repository.libraryBooks().isEmpty())
    }

    @Test
    fun openBook_reuses_existing_entry() = runBlocking {
        val first = openFakeBook(EXTERNAL_URI)
        val second = openFakeBook(EXTERNAL_URI)

        assertEquals(first.cachedPath, second.cachedPath)
        assertEquals(1, repository.recentBooks().size)
    }

    @Test
    fun updateProgress_persists_reading_location() = runBlocking {
        val book = addFakeBook()

        repository.updateProgress(book.uri, lastPage = 12, pageCount = 320)

        val recent = repository.recentBooks().first { it.uri == book.uri }
        assertEquals(12, recent.lastPage)
        assertEquals(320, recent.pageCount)
        assertTrue(recent.lastOpenedAt > 0)
    }

    @Test
    fun removeBook_deletes_stored_pdf_and_entry() = runBlocking {
        val book = addFakeBook()
        val stored = File(book.cachedPath)
        check(stored.exists())

        repository.removeBook(book.uri)

        assertFalse(stored.exists())
        assertNull(repository.recentBooks().firstOrNull { it.uri == book.uri })
    }

    @Test
    fun coverFile_is_stable_per_book_uri() {
        val book = runBlocking { addFakeBook() }

        assertEquals(repository.coverFile(book).path, repository.coverFile(book).path)
    }

    private fun addFakeBook(): Book {
        register(FAKE_URI)
        return runBlocking {
            checkNotNull(repository.addBook(FAKE_URI))
        }
    }

    private fun openFakeBook(uri: Uri): Book {
        register(uri)
        return runBlocking {
            checkNotNull(repository.openBook(uri))
        }
    }

    private fun register(uri: Uri) {
        Shadows
            .shadowOf(application.contentResolver)
            .registerInputStream(uri, ByteArrayInputStream(FAKE_PDF_BYTES))
    }

    private fun libraryDirectory(): File = File(Environment.getExternalStorageDirectory(), "Kitab")

    private fun openedDirectory(): File = File(application.filesDir, "opened")

    private companion object {
        val FAKE_URI: Uri = Uri.parse("content://kitab.test/sample.pdf")
        val EXTERNAL_URI: Uri = Uri.parse("content://kitab.test/external.pdf")
        val FAKE_PDF_BYTES = byteArrayOf(1, 2, 3)
    }
}
