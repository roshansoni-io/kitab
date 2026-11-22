package io.github.roshansoni_io.kitab.feature.collection

import androidx.lifecycle.SavedStateHandle
import io.github.roshansoni_io.kitab.MainDispatcherRule
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.fakes.FakeBookRepository
import io.github.roshansoni_io.kitab.fakes.FakeSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BookCollectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookRepository()
    private val settings = FakeSettingsRepository()

    @Test
    fun recentSource_showsAllBooksSortedByLastOpenedAt() {
        repository.addInitialBooks(
            libraryBook("a", "Alpha", lastOpenedAt = 10),
            recentOnlyBook("b", "Beta", lastOpenedAt = 20),
        )

        val viewModel = createViewModel(BookCollectionSource.Recent)

        assertEquals(listOf("b", "a"), viewModel.uiState.value.books.map(Book::uri))
        assertEquals(2, viewModel.uiState.value.totalBookCount)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun librarySource_showsOnlyLibraryBooks() {
        repository.addInitialBooks(
            libraryBook("a", "Alpha"),
            libraryBook("b", "Beta"),
            recentOnlyBook("c", "Gamma"),
        )

        val viewModel = createViewModel(BookCollectionSource.Library)

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
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onFilterChanged(BookFilter.Starred)

        assertEquals(listOf("a"), viewModel.uiState.value.books.map(Book::uri))
    }

    @Test
    fun search_filtersBooksByTitle() {
        repository.addInitialBooks(libraryBook("a", "Alpha"), libraryBook("b", "Beta"))
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onSearchVisibilityChanged(isVisible = true)
        viewModel.onSearchQueryChanged("alp")

        assertEquals(listOf("a"), viewModel.uiState.value.books.map(Book::uri))
    }

    @Test
    fun starringBook_updatesRepositoryBackedState() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onStarRequested(viewModel.uiState.value.books.first())

        assertTrue(viewModel.uiState.value.books.first().starred)
    }

    @Test
    fun confirmingRemoval_removesBookFromState() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onRemoveRequested(viewModel.uiState.value.books.first())
        viewModel.onRemovalConfirmed()

        assertTrue(viewModel.uiState.value.books.isEmpty())
        assertEquals(listOf("a"), repository.removedUris)
    }

    @Test
    fun librarySource_readsAndWritesListViewPreference() {
        val settingsRepository = FakeSettingsRepository(AppSettings(libraryListView = true))
        val viewModel = createViewModel(BookCollectionSource.Library, settingsRepository)

        assertTrue(viewModel.uiState.value.isListView)

        viewModel.onListViewChanged(false)

        assertFalse(viewModel.uiState.value.isListView)
        assertFalse(settingsRepository.settings.value.libraryListView)
    }

    @Test
    fun recentSource_ignoresListViewPreference() {
        val settingsRepository = FakeSettingsRepository(AppSettings(libraryListView = true))
        val viewModel = createViewModel(BookCollectionSource.Recent, settingsRepository)

        assertFalse(viewModel.uiState.value.isListView)

        viewModel.onListViewChanged(true)

        assertFalse(viewModel.uiState.value.isListView)
        assertTrue(settingsRepository.settings.value.libraryListView)
    }

    @Test
    fun libraryRecentFilter_sortsByLastOpenedAt() {
        repository.addInitialBooks(
            libraryBook("a", "Alpha", lastOpenedAt = 10),
            libraryBook("b", "Beta", lastOpenedAt = 20),
        )
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onFilterChanged(BookFilter.Recent)

        assertEquals(listOf("b", "a"), viewModel.uiState.value.books.map(Book::uri))
    }

    private fun createViewModel(
        source: BookCollectionSource,
        settingsRepository: FakeSettingsRepository = settings,
    ): BookCollectionViewModel =
        BookCollectionViewModel(
            bookRepository = repository,
            settingsRepository = settingsRepository,
            source = source,
            savedStateHandle = SavedStateHandle(),
        )

    private fun libraryBook(
        uri: String,
        title: String,
        isStarred: Boolean = false,
        lastOpenedAt: Long = 1,
    ): Book =
        Book(
            uri = uri,
            cachedPath = "",
            title = title,
            inLibrary = true,
            starred = isStarred,
            lastOpenedAt = lastOpenedAt,
        )

    private fun recentOnlyBook(
        uri: String,
        title: String,
        lastOpenedAt: Long = 1,
    ): Book =
        Book(
            uri = uri,
            cachedPath = "",
            title = title,
            lastOpenedAt = lastOpenedAt,
        )
}
