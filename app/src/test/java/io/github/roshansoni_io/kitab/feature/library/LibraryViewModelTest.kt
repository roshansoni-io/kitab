package io.github.roshansoni_io.kitab.feature.library

import androidx.lifecycle.SavedStateHandle
import io.github.roshansoni_io.kitab.MainDispatcherRule
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.fakes.FakeBookRepository
import io.github.roshansoni_io.kitab.fakes.FakeSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository()

    @Test
    fun initialLoad_populatesLibraryBooksOnly() {
        repository.addInitialBooks(
            libraryBook("a", "Alpha"),
            libraryBook("b", "Beta"),
            recentOnlyBook("c", "Gamma"),
        )

        val viewModel = createViewModel()

        assertEquals(setOf("a", "b"), viewModel.uiState.value.books.map(Book::uri).toSet())
        assertEquals(2, viewModel.uiState.value.totalBookCount)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun starredFilter_showsOnlyStarredBooks() {
        repository.addInitialBooks(
            libraryBook("a", "Alpha", isStarred = true),
            libraryBook("b", "Beta"),
        )
        val viewModel = createViewModel()

        viewModel.onFilterChanged(BookFilter.Starred)

        assertEquals(listOf("a"), viewModel.uiState.value.books.map(Book::uri))
    }

    @Test
    fun search_filtersBooksByTitle() {
        repository.addInitialBooks(libraryBook("a", "Alpha"), libraryBook("b", "Beta"))
        val viewModel = createViewModel()

        viewModel.onSearchVisibilityChanged(isVisible = true)
        viewModel.onSearchQueryChanged("alp")

        assertEquals(listOf("a"), viewModel.uiState.value.books.map(Book::uri))
    }

    @Test
    fun starringBook_updatesRepositoryBackedState() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel()

        viewModel.onStarRequested(viewModel.uiState.value.books.first())

        assertTrue(viewModel.uiState.value.books.first().starred)
    }

    @Test
    fun confirmingRemoval_removesBookFromState() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel()

        viewModel.onRemoveRequested(viewModel.uiState.value.books.first())
        viewModel.onRemovalConfirmed()

        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertEquals(listOf("a"), repository.removedUris)
    }

    private fun createViewModel(): LibraryViewModel =
        LibraryViewModel(
            bookRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            savedStateHandle = SavedStateHandle(),
        )

    private fun libraryBook(
        uri: String,
        title: String,
        isStarred: Boolean = false,
    ): Book =
        Book(
            uri = uri,
            cachedPath = "",
            title = title,
            inLibrary = true,
            starred = isStarred,
            lastOpenedAt = 1,
        )

    private fun recentOnlyBook(
        uri: String,
        title: String,
    ): Book =
        Book(
            uri = uri,
            cachedPath = "",
            title = title,
            lastOpenedAt = 1,
        )
}
