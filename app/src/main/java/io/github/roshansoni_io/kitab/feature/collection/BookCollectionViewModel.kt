package io.github.roshansoni_io.kitab.feature.collection

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.data.model.BookSortOrder
import io.github.roshansoni_io.kitab.core.data.repository.BookRepository
import io.github.roshansoni_io.kitab.core.data.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class BookCollectionUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val totalBookCount: Int = 0,
    val filter: BookFilter = BookFilter.All,
    val sortOrder: BookSortOrder = BookSortOrder.LastOpened,
    val isListView: Boolean = false,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val covers: Map<String, ImageBitmap> = emptyMap(),
    val pendingRemoval: Book? = null,
    val pendingRename: Book? = null,
    val pendingDetails: Book? = null,
    val pendingClearHistory: Boolean = false,
    val pendingFileDeletion: Book? = null,
)

internal sealed interface BookCollectionEffect {
    data class ShowSnackbar(
        val message: String,
    ) : BookCollectionEffect
}

internal class BookCollectionViewModel(
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository,
    private val source: BookCollectionSource,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val filterKey = "${source.savedStateKeyPrefix}_filter"
    private val sortOrderKey = "${source.savedStateKeyPrefix}_sort_order"
    private val searchActiveKey = "${source.savedStateKeyPrefix}_search_active"
    private val searchQueryKey = "${source.savedStateKeyPrefix}_search_query"

    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<BookCollectionUiState> = _uiState.asStateFlow()

    private val _effects = Channel<BookCollectionEffect>(Channel.BUFFERED)
    val effects: Flow<BookCollectionEffect> = _effects.receiveAsFlow()

    private val coverRequests = mutableSetOf<String>()
    private var sourceBooks = emptyList<Book>()

    init {
        viewModelScope.launch {
            bookRepository.initialize()
            bookRepository.books.collect { allBooks ->
                publishBooks(allBooks)
            }
        }
    }

    fun onFilterChanged(filter: BookFilter) {
        savedStateHandle[filterKey] = filter.name
        _uiState.update { currentState ->
            currentState.copy(
                filter = filter,
                books =
                    visibleBooks(
                        books = sourceBooks,
                        filter = filter,
                        searchQuery = currentState.searchQuery,
                        sortOrder = currentState.sortOrder,
                    ),
            )
        }
    }

    fun onSortOrderChanged(sortOrder: BookSortOrder) {
        savedStateHandle[sortOrderKey] = sortOrder.name
        _uiState.update { currentState ->
            currentState.copy(
                sortOrder = sortOrder,
                books =
                    visibleBooks(
                        books = sourceBooks,
                        filter = currentState.filter,
                        searchQuery = currentState.searchQuery,
                        sortOrder = sortOrder,
                    ),
            )
        }
    }

    fun onListViewChanged(isListView: Boolean) {
        if (!source.supportsListView) return
        settingsRepository.setLibraryListView(isListView)
        _uiState.update { it.copy(isListView = isListView) }
    }

    fun onSearchVisibilityChanged(isVisible: Boolean) {
        savedStateHandle[searchActiveKey] = isVisible
        _uiState.update { currentState ->
            val searchQuery = if (isVisible) currentState.searchQuery else ""
            savedStateHandle[searchQueryKey] = searchQuery
            currentState.copy(
                isSearchActive = isVisible,
                searchQuery = searchQuery,
                books =
                    visibleBooks(
                        books = sourceBooks,
                        filter = currentState.filter,
                        searchQuery = searchQuery,
                        sortOrder = currentState.sortOrder,
                    ),
            )
        }
    }

    fun onSearchQueryChanged(searchQuery: String) {
        savedStateHandle[searchQueryKey] = searchQuery
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = searchQuery,
                books =
                    visibleBooks(
                        books = sourceBooks,
                        filter = currentState.filter,
                        searchQuery = searchQuery,
                        sortOrder = currentState.sortOrder,
                    ),
            )
        }
    }

    fun onStarRequested(book: Book) {
        viewModelScope.launch {
            bookRepository.setStarred(book.uri, !book.starred)
        }
    }

    fun onCoverRequested(book: Book) {
        if (_uiState.value.covers.containsKey(book.uri) || !coverRequests.add(book.uri)) return
        viewModelScope.launch {
            try {
                val image =
                    bookRepository.loadCover(book)?.let { bitmap ->
                        withContext(Dispatchers.Default) { bitmap.asImageBitmap() }
                    }
                if (image != null) {
                    _uiState.update { current -> current.copy(covers = current.covers + (book.uri to image)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // A failed cover request leaves the white placeholder visible.
            } finally {
                coverRequests.remove(book.uri)
            }
        }
    }

    fun onAddToLibrary(book: Book) {
        viewModelScope.launch {
            if (bookRepository.addBook(book.uri) == null) {
                _effects.send(BookCollectionEffect.ShowSnackbar("Could not add book to library"))
            } else {
                _effects.send(BookCollectionEffect.ShowSnackbar("Added to library"))
            }
        }
    }

    fun onRemoveRequested(book: Book) {
        _uiState.update { it.copy(pendingRemoval = book) }
    }

    fun onRemovalConfirmed() {
        val book = _uiState.value.pendingRemoval ?: return
        _uiState.update { it.copy(pendingRemoval = null) }
        viewModelScope.launch {
            bookRepository.removeBook(book.uri)
            _effects.send(BookCollectionEffect.ShowSnackbar("Removed from library"))
        }
    }

    fun onRemovalDismissed() {
        _uiState.update { it.copy(pendingRemoval = null) }
    }

    fun onRenameRequested(book: Book) {
        _uiState.update { it.copy(pendingRename = book) }
    }

    fun onRenameConfirmed(newTitle: String) {
        val book = _uiState.value.pendingRename ?: return
        _uiState.update { it.copy(pendingRename = null) }
        viewModelScope.launch {
            bookRepository.renameBook(book.uri, newTitle)
            _effects.send(BookCollectionEffect.ShowSnackbar("Book renamed"))
        }
    }

    fun onRenameDismissed() {
        _uiState.update { it.copy(pendingRename = null) }
    }

    fun onDetailsRequested(book: Book) {
        _uiState.update { it.copy(pendingDetails = book) }
    }

    fun onDetailsDismissed() {
        _uiState.update { it.copy(pendingDetails = null) }
    }

    fun onResetProgressRequested(book: Book) {
        viewModelScope.launch {
            bookRepository.resetProgress(book.uri)
            _effects.send(BookCollectionEffect.ShowSnackbar("Reading progress reset"))
        }
    }

    fun onMarkCompletedRequested(book: Book) {
        viewModelScope.launch {
            bookRepository.markCompleted(book.uri)
            _effects.send(BookCollectionEffect.ShowSnackbar("Marked as read"))
        }
    }

    fun onClearHistoryRequested() {
        _uiState.update { it.copy(pendingClearHistory = true) }
    }

    fun onClearHistoryConfirmed() {
        _uiState.update { it.copy(pendingClearHistory = false) }
        viewModelScope.launch {
            bookRepository.clearRecentHistory()
            _effects.send(BookCollectionEffect.ShowSnackbar("Reading history cleared"))
        }
    }

    fun onClearHistoryDismissed() {
        _uiState.update { it.copy(pendingClearHistory = false) }
    }

    fun onDeleteFileRequested(book: Book) {
        _uiState.update { it.copy(pendingFileDeletion = book) }
    }

    fun onDeleteFileConfirmed() {
        val book = _uiState.value.pendingFileDeletion ?: return
        _uiState.update { it.copy(pendingFileDeletion = null) }
        viewModelScope.launch {
            bookRepository.deleteBookFile(book.uri)
            _effects.send(BookCollectionEffect.ShowSnackbar("File deleted from device"))
        }
    }

    fun onDeleteFileDismissed() {
        _uiState.update { it.copy(pendingFileDeletion = null) }
    }

    private fun publishBooks(allBooks: List<Book>) {
        val books =
            when (source) {
                BookCollectionSource.Recent -> allBooks.filter { it.lastOpenedAt > 0 }
                BookCollectionSource.Library -> allBooks.filter(Book::inLibrary)
            }
        sourceBooks = books
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                books =
                    visibleBooks(
                        books = books,
                        filter = currentState.filter,
                        searchQuery = currentState.searchQuery,
                        sortOrder = currentState.sortOrder,
                    ),
                totalBookCount = books.size,
            )
        }
    }

    private fun initialUiState(): BookCollectionUiState =
        BookCollectionUiState(
            filter = savedFilter(),
            sortOrder = savedSortOrder(),
            isListView = source.supportsListView && settingsRepository.settings.value.libraryListView,
            isSearchActive = savedStateHandle[searchActiveKey] ?: false,
            searchQuery = savedStateHandle[searchQueryKey] ?: "",
        )

    private fun savedFilter(): BookFilter =
        BookFilter.entries.firstOrNull { it.name == savedStateHandle.get<String>(filterKey) }
            ?: BookFilter.All

    private fun savedSortOrder(): BookSortOrder =
        BookSortOrder.entries.firstOrNull { it.name == savedStateHandle.get<String>(sortOrderKey) }
            ?: BookSortOrder.LastOpened

    private fun visibleBooks(
        books: List<Book>,
        filter: BookFilter,
        searchQuery: String,
        sortOrder: BookSortOrder,
    ): List<Book> {
        val matchingBooks =
            if (searchQuery.isBlank()) {
                books
            } else {
                val normalizedQuery = searchQuery.trim()
                books.filter { book -> book.title.contains(normalizedQuery, ignoreCase = true) }
            }
        val filteredBooks =
            when (filter) {
                BookFilter.All -> matchingBooks
                BookFilter.Recent -> matchingBooks.filter { it.lastOpenedAt > 0 }
                BookFilter.Starred -> matchingBooks.filter(Book::starred)
            }
        return when (sortOrder) {
            BookSortOrder.LastOpened -> {
                filteredBooks.sortedByDescending { it.lastOpenedAt }
            }

            BookSortOrder.TitleAsc -> {
                filteredBooks.sortedBy { it.title.lowercase() }
            }

            BookSortOrder.TitleDesc -> {
                filteredBooks.sortedByDescending { it.title.lowercase() }
            }

            BookSortOrder.Progress -> {
                filteredBooks.sortedByDescending {
                    if (it.pageCount > 0) it.lastPage.toDouble() / it.pageCount else (it.epubProgress ?: 0.0)
                }
            }
        }
    }

    companion object {
        fun factory(
            bookRepository: BookRepository,
            settingsRepository: SettingsRepository,
            source: BookCollectionSource,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    BookCollectionViewModel(
                        bookRepository = bookRepository,
                        settingsRepository = settingsRepository,
                        source = source,
                        savedStateHandle = createSavedStateHandle(),
                    )
                }
            }
    }
}
