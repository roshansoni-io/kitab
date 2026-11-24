package io.github.roshansoni_io.kitab.feature.collection

import androidx.lifecycle.SavedStateHandle
import io.github.roshansoni_io.kitab.MainDispatcherRule
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.data.model.BookSortOrder
import io.github.roshansoni_io.kitab.fakes.FakeBookRepository
import io.github.roshansoni_io.kitab.fakes.FakeSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun recentSource_readsAndWritesListViewPreference() {
        val settingsRepository = FakeSettingsRepository(AppSettings(libraryListView = true))
        val viewModel = createViewModel(BookCollectionSource.Recent, settingsRepository)

        assertTrue(viewModel.uiState.value.isListView)

        viewModel.onListViewChanged(false)

        assertFalse(viewModel.uiState.value.isListView)
        assertFalse(settingsRepository.settings.value.libraryListView)
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

    @Test
    fun sortOrder_titleAsc() {
        repository.addInitialBooks(
            libraryBook("b", "Beta"),
            libraryBook("a", "Alpha"),
            libraryBook("c", "Gamma"),
        )
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onSortOrderChanged(BookSortOrder.TitleAsc)

        assertEquals(listOf("a", "b", "c"), viewModel.uiState.value.books.map(Book::uri))
    }

    @Test
    fun sortOrder_titleDesc() {
        repository.addInitialBooks(
            libraryBook("b", "Beta"),
            libraryBook("a", "Alpha"),
            libraryBook("c", "Gamma"),
        )
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onSortOrderChanged(BookSortOrder.TitleDesc)

        assertEquals(listOf("c", "b", "a"), viewModel.uiState.value.books.map(Book::uri))
    }

    @Test
    fun sortOrder_progress() {
        repository.addInitialBooks(
            libraryBook("a", "Alpha").copy(pageCount = 100, lastPage = 10),
            libraryBook("b", "Beta").copy(pageCount = 100, lastPage = 80),
            libraryBook("c", "Gamma").copy(pageCount = 100, lastPage = 40),
        )
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onSortOrderChanged(BookSortOrder.Progress)

        assertEquals(listOf("b", "c", "a"), viewModel.uiState.value.books.map(Book::uri))
    }

    @Test
    fun renameBook_updatesTitleAndDismissesDialog() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel(BookCollectionSource.Library)

        val book = viewModel.uiState.value.books.first()
        viewModel.onRenameRequested(book)
        assertEquals(book, viewModel.uiState.value.pendingRename)

        viewModel.onRenameConfirmed("New Title")
        assertNull(viewModel.uiState.value.pendingRename)
        assertEquals("New Title", viewModel.uiState.value.books.first().title)
    }

    @Test
    fun renameBookDismissed_clearsPendingRename() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel(BookCollectionSource.Library)

        val book = viewModel.uiState.value.books.first()
        viewModel.onRenameRequested(book)
        viewModel.onRenameDismissed()

        assertNull(viewModel.uiState.value.pendingRename)
    }

    @Test
    fun detailsDialog_showsAndDismisses() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel(BookCollectionSource.Library)

        val book = viewModel.uiState.value.books.first()
        viewModel.onDetailsRequested(book)
        assertEquals(book, viewModel.uiState.value.pendingDetails)

        viewModel.onDetailsDismissed()
        assertNull(viewModel.uiState.value.pendingDetails)
    }

    @Test
    fun resetProgress_resetsReadingProgress() {
        repository.addInitialBooks(libraryBook("a", "Alpha").copy(pageCount = 100, lastPage = 50))
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onResetProgressRequested(viewModel.uiState.value.books.first())

        assertEquals(0, viewModel.uiState.value.books.first().lastPage)
    }

    @Test
    fun markCompleted_marksBookAsCompleted() {
        repository.addInitialBooks(libraryBook("a", "Alpha").copy(pageCount = 100, lastPage = 10))
        val viewModel = createViewModel(BookCollectionSource.Library)

        viewModel.onMarkCompletedRequested(viewModel.uiState.value.books.first())

        assertEquals(100, viewModel.uiState.value.books.first().lastPage)
    }

    @Test
    fun clearHistory_resetsRecentHistory() {
        repository.addInitialBooks(
            libraryBook("a", "Alpha", lastOpenedAt = 10),
            recentOnlyBook("b", "Beta", lastOpenedAt = 20),
        )
        val viewModel = createViewModel(BookCollectionSource.Recent)

        viewModel.onClearHistoryRequested()
        assertTrue(viewModel.uiState.value.pendingClearHistory)

        viewModel.onClearHistoryConfirmed()
        assertFalse(viewModel.uiState.value.pendingClearHistory)
        assertTrue(viewModel.uiState.value.books.isEmpty())
    }

    @Test
    fun clearHistoryDismissed_clearsPendingState() {
        val viewModel = createViewModel(BookCollectionSource.Recent)

        viewModel.onClearHistoryRequested()
        assertTrue(viewModel.uiState.value.pendingClearHistory)

        viewModel.onClearHistoryDismissed()
        assertFalse(viewModel.uiState.value.pendingClearHistory)
    }

    @Test
    fun deleteBookFile_removesBookPermanently() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel(BookCollectionSource.Library)

        val book = viewModel.uiState.value.books.first()
        viewModel.onDeleteFileRequested(book)
        assertEquals(book, viewModel.uiState.value.pendingFileDeletion)

        viewModel.onDeleteFileConfirmed()
        assertNull(viewModel.uiState.value.pendingFileDeletion)
        assertTrue(viewModel.uiState.value.books.isEmpty())
    }

    @Test
    fun deleteBookFileDismissed_clearsPendingState() {
        repository.addInitialBooks(libraryBook("a", "Alpha"))
        val viewModel = createViewModel(BookCollectionSource.Library)

        val book = viewModel.uiState.value.books.first()
        viewModel.onDeleteFileRequested(book)
        viewModel.onDeleteFileDismissed()

        assertNull(viewModel.uiState.value.pendingFileDeletion)
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
