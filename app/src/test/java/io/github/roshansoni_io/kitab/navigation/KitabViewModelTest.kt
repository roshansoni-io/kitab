package io.github.roshansoni_io.kitab.navigation

import androidx.lifecycle.SavedStateHandle
import io.github.roshansoni_io.kitab.MainDispatcherRule
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.fakes.FakeBookRepository
import io.github.roshansoni_io.kitab.fakes.FakeStorageAccess
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
    private val storageAccess = FakeStorageAccess()

    @Test
    fun externalUri_withoutStorageAccess_showsAccessDialog() {
        storageAccess.granted = false
        val viewModel = createViewModel()

        viewModel.onEvent(KitabEvent.ExternalUriReceived("content://book"))

        assertEquals(KitabDialog.StorageAccessRequired, viewModel.uiState.value.dialog)
    }

    @Test
    fun externalUri_withStorageAccess_navigatesToReader() = runTest {
        repository.openResult = book()
        val viewModel = createViewModel()
        val effect = async { viewModel.effects.first() }

        viewModel.onEvent(KitabEvent.ExternalUriReceived("content://book"))

        assertEquals(KitabEffect.NavigateToReader("content://book"), effect.await())
    }

    @Test
    fun storageAccessResult_opensPendingExternalBook() = runTest {
        storageAccess.granted = false
        repository.openResult = book()
        val viewModel = createViewModel()
        viewModel.onEvent(KitabEvent.ExternalUriReceived("content://book"))
        val effect = async { viewModel.effects.first() }

        storageAccess.granted = true
        viewModel.onEvent(KitabEvent.StorageAccessResult(isGranted = true))

        assertEquals(KitabEffect.NavigateToReader("content://book"), effect.await())
    }

    @Test
    fun addBook_withoutStorageAccess_showsAccessDialog() {
        storageAccess.granted = false
        val viewModel = createViewModel()

        viewModel.onEvent(KitabEvent.AddBookRequested)

        assertEquals(KitabDialog.StorageAccessRequired, viewModel.uiState.value.dialog)
    }

    @Test
    fun requestStorageAccess_emitsSettingsEffect() = runTest {
        storageAccess.granted = false
        val viewModel = createViewModel()
        val effect = async { viewModel.effects.first() }

        viewModel.onEvent(KitabEvent.RequestStorageAccess)

        assertEquals(KitabEffect.OpenStorageAccessSettings, effect.await())
    }

    @Test
    fun addBook_withStorageAccess_opensDocumentPicker() = runTest {
        val viewModel = createViewModel()
        val effect = async { viewModel.effects.first() }

        viewModel.onEvent(KitabEvent.AddBookRequested)

        assertEquals(KitabEffect.OpenDocumentPicker, effect.await())
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
    fun openExternalLink_emitsEffect() = runTest {
        val viewModel = createViewModel()
        val effect = async { viewModel.effects.first() }

        viewModel.onEvent(KitabEvent.OpenExternalLink("https://example.com"))

        assertEquals(KitabEffect.OpenExternalLink("https://example.com"), effect.await())
    }

    private fun createViewModel(): KitabViewModel =
        KitabViewModel(
            bookRepository = repository,
            storageAccess = storageAccess,
            savedStateHandle = SavedStateHandle(),
        )

    private fun book(): Book =
        Book(
            uri = "content://book",
            cachedPath = "",
            title = "Book",
            inLibrary = true,
        )
}
