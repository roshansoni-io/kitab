package io.github.roshansoni_io.kitab.core.data.repository

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import io.github.roshansoni_io.kitab.core.data.model.Book
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class LocalBookRepositoryTest {
    private val application = RuntimeEnvironment.getApplication() as Application
    private lateinit var repository: LocalBookRepository
    private var renderCount = 0

    @Before
    fun setUp() {
        application.getSharedPreferences("kitab_books", 0).edit().clear().commit()
        application.getSharedPreferences("kitab_book_progress", 0).edit().clear().commit()
        File(application.filesDir, "books").deleteRecursively()
        File(application.filesDir, "thumbnails").deleteRecursively()
        renderCount = 0
        repository =
            LocalBookRepository(application) { _, _ ->
                renderCount++
                Bitmap.createBitmap(3, 4, Bitmap.Config.ARGB_8888)
            }
    }

    @Test
    fun addBook_storesPdfAndThumbnailInPrivateDirectories() = runBlocking {
        val book = addFakeBook()
        val stored = File(book.cachedPath)

        assertTrue(stored.exists())
        assertTrue(book.inLibrary)
        assertEquals(booksDirectory(), stored.parentFile)
        assertTrue(repository.thumbnailFile(book).isFile)
        assertEquals(thumbnailsDirectory(), repository.thumbnailFile(book).parentFile)
        assertEquals(listOf(book.uri), repository.books.value.filter(Book::inLibrary).map(Book::uri))
        assertEquals(1, renderCount)
    }

    @Test
    fun openBook_storesBookPrivatelyWithoutAddingItToLibrary() = runBlocking {
        val book = openFakeBook(EXTERNAL_URI)

        assertFalse(book.inLibrary)
        assertTrue(File(book.cachedPath).exists())
        assertEquals(booksDirectory(), File(book.cachedPath).parentFile)
        assertEquals(listOf(book.uri), repository.books.value.map(Book::uri))
        assertTrue(repository.books.value.none(Book::inLibrary))
    }

    @Test
    fun openBook_reusesExistingEntry() = runBlocking {
        val first = openFakeBook(EXTERNAL_URI)
        val second = openFakeBook(EXTERNAL_URI)

        assertEquals(first.cachedPath, second.cachedPath)
        assertEquals(1, repository.books.value.size)
    }

    @Test
    fun addBook_reusesPrivateCachedCopyAndGeneratesThumbnail() = runBlocking {
        val opened = openFakeBook(EXTERNAL_URI)

        val added = checkNotNull(repository.addBook(EXTERNAL_URI.toString()))

        assertEquals(opened.cachedPath, added.cachedPath)
        assertTrue(added.inLibrary)
        assertTrue(repository.thumbnailFile(added).isFile)
        assertEquals(1, renderCount)
    }

    @Test
    fun openBook_migratesLegacyCachedFileIntoBooksDirectory() = runBlocking {
        val legacyFile = File(application.filesDir, "legacy.pdf").apply { writeBytes(FAKE_PDF_BYTES) }
        storeBookRecord(EXTERNAL_URI.toString(), legacyFile.path)
        repository = createRepository()

        val book = checkNotNull(repository.openBook(EXTERNAL_URI.toString()))

        assertEquals(booksDirectory(), File(book.cachedPath).parentFile)
        assertTrue(File(book.cachedPath).isFile)
        assertTrue(legacyFile.exists())
    }

    @Test
    fun openBook_recopiesWhenCachedPathIsMissing() = runBlocking {
        storeBookRecord(EXTERNAL_URI.toString(), File(application.filesDir, "missing.pdf").path)
        repository = createRepository()
        register(EXTERNAL_URI)

        val book = checkNotNull(repository.openBook(EXTERNAL_URI.toString()))

        assertTrue(File(book.cachedPath).isFile)
        assertEquals(booksDirectory(), File(book.cachedPath).parentFile)
    }

    @Test
    fun updateProgress_persistsReadingLocation() = runBlocking {
        val book = addFakeBook()

        repository.updateProgress(book.uri, lastPage = 12, pageCount = 320)

        val recent = repository.books.value.first { it.uri == book.uri }
        assertEquals(12, recent.lastPage)
        assertEquals(320, recent.pageCount)
        assertTrue(recent.lastOpenedAt > 0)

        repository = createRepository()
        val restored = repository.books.value.first { it.uri == book.uri }
        assertEquals(12, restored.lastPage)
        assertEquals(320, restored.pageCount)
    }

    @Test
    fun setStarred_persistsState() = runBlocking {
        val book = addFakeBook()
        assertFalse(book.starred)

        repository.setStarred(book.uri, true)
        assertTrue(repository.books.value.first { it.uri == book.uri }.starred)

        repository.setStarred(book.uri, false)
        assertFalse(repository.books.value.first { it.uri == book.uri }.starred)
    }

    @Test
    fun removeBook_deletesPrivatePdfThumbnailAndEntry() = runBlocking {
        val book = addFakeBook()
        val stored = File(book.cachedPath)
        val thumbnail = repository.thumbnailFile(book)
        assertTrue(stored.exists())
        assertTrue(thumbnail.exists())

        repository.removeBook(book.uri)

        assertFalse(stored.exists())
        assertFalse(thumbnail.exists())
        assertNull(repository.books.value.firstOrNull { it.uri == book.uri })
    }

    @Test
    fun thumbnailFile_isStableAndUsesCollisionResistantBookId() = runBlocking {
        val first = addFakeBook()
        val second = first.copy(uri = "content://kitab.test/BB")
        val collidingLegacyUri = first.copy(uri = "content://kitab.test/Aa")

        assertEquals(repository.thumbnailFile(first).path, repository.thumbnailFile(first).path)
        assertNotEquals(repository.thumbnailFile(collidingLegacyUri).path, repository.thumbnailFile(second).path)
    }

    @Test
    fun invalidProviderUriReturnsFailureInsteadOfThrowing() = runBlocking {
        assertNull(repository.openBook("content://missing.provider/book.pdf"))
    }

    private fun addFakeBook(): Book {
        register(FAKE_URI)
        return runBlocking {
            checkNotNull(repository.addBook(FAKE_URI.toString()))
        }
    }

    private fun openFakeBook(uri: Uri): Book {
        register(uri)
        return runBlocking {
            checkNotNull(repository.openBook(uri.toString()))
        }
    }

    private fun register(uri: Uri) {
        Shadows.shadowOf(application.contentResolver).registerInputStream(uri, ByteArrayInputStream(FAKE_PDF_BYTES))
    }

    private fun storeBookRecord(
        uri: String,
        cachedPath: String,
    ) {
        val record = JSONObject().put("uri", uri).put("cachedPath", cachedPath).put("title", "Book")
        application
            .getSharedPreferences("kitab_books", 0)
            .edit()
            .putString("recent_books", JSONArray().put(record).toString())
            .commit()
    }

    private fun createRepository() =
        LocalBookRepository(application) { _, _ ->
            renderCount++
            Bitmap.createBitmap(3, 4, Bitmap.Config.ARGB_8888)
        }

    private fun booksDirectory(): File = File(application.filesDir, "books")

    private fun thumbnailsDirectory(): File = File(application.filesDir, "thumbnails")

    private companion object {
        val FAKE_URI: Uri = Uri.parse("content://kitab.test/sample.pdf")
        val EXTERNAL_URI: Uri = Uri.parse("content://kitab.test/external.pdf")
        val FAKE_PDF_BYTES = byteArrayOf(1, 2, 3)
    }
}
