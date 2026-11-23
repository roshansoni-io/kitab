package io.github.roshansoni_io.kitab.navigation

import io.github.roshansoni_io.kitab.MainDispatcherRule
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.fakes.FakeBookRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KitabViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository()

    @Test
    fun externalUri_navigatesToReaderWithoutStoragePermission() = runTest {
        repository.openResult = book()
        val viewModel = createViewModel()
        val effect = async { viewModel.effects.first() }

        viewModel.onEvent(KitabEvent.ExternalUriReceived("content://book"))

        assertEquals(KitabEffect.NavigateToReader("content://book"), effect.await())
    }

    @Test
    fun pickedDocument_addsBookAndShowsSnackbar() = runTest {
        repository.addResult = book()
        val viewModel = createViewModel()
        val effect = async { viewModel.effects.first() }

        viewModel.onEvent(KitabEvent.DocumentPicked("content://book"))

        assertEquals(KitabEffect.ShowSnackbar("Added to library"), effect.await())
        assertEquals(listOf("content://book"), repository.books.value.map(Book::uri))
    }

    @Test
    fun progressChanged_isPersisted() {
        val viewModel = createViewModel()

        viewModel.onEvent(
            KitabEvent.ProgressChanged(
                bookUri = "content://book",
                page = 4,
                pageCount = 20,
            ),
        )

        assertEquals(listOf(Triple("content://book", 4, 20)), repository.progressUpdates)
    }

    @Test
    fun epubProgressChanged_isPersisted() {
        val viewModel = createViewModel()

        viewModel.onEvent(
            KitabEvent.EpubProgressChanged(
                bookUri = "content://book",
                locatorJson = """{"href":"chapter1.xhtml"}""",
                progression = 0.25,
            ),
        )

        assertEquals(
            listOf(Triple("content://book", """{"href":"chapter1.xhtml"}""", 0.25)),
            repository.epubProgressUpdates,
        )
    }

    private fun createViewModel() = KitabViewModel(bookRepository = repository)

    private fun book() =
        Book(
            uri = "content://book",
            cachedPath = "",
            title = "Book",
            inLibrary = true,
        )
}
