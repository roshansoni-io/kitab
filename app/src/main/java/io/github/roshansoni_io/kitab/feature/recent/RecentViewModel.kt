package io.github.roshansoni_io.kitab.feature.recent

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
import io.github.roshansoni_io.kitab.core.data.repository.BookRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class RecentUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val totalBookCount: Int = 0,
    val filter: BookFilter = BookFilter.All,
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val covers: Map<String, ImageBitmap> = emptyMap(),
    val pendingRemoval: Book? = null,
)

internal sealed interface RecentEffect {
    data class ShowSnackbar(
        val message: String,
    ) : RecentEffect
}

internal class RecentViewModel(
    private val bookRepository: BookRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialUiState())
    val uiState: StateFlow<RecentUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RecentEffect>(Channel.BUFFERED)
    val effects: Flow<RecentEffect> = _effects.receiveAsFlow()
    private val coverRequests = mutableSetOf<String>()
    private var sortedBooks = emptyList<Book>()

    init {
        viewModelScope.launch {
            bookRepository.books.collectLatest { allBooks ->
                publishBooks(allBooks)
            }
        }
    }

    fun onFilterChanged(filter: BookFilter) {
        savedStateHandle[KEY_FILTER] = filter.name
        _uiState.update { currentState ->
            currentState.copy(
                filter = filter,
                books =
                    visibleBooks(
                        allBooks = sortedBooks,
                        filter = filter,
                        searchQuery = currentState.searchQuery,
                    ),
            )
        }
    }

    fun onSearchVisibilityChanged(isVisible: Boolean) {
        savedStateHandle[KEY_SEARCH_ACTIVE] = isVisible
        _uiState.update { currentState ->
            val searchQuery = if (isVisible) currentState.searchQuery else ""
            savedStateHandle[KEY_SEARCH_QUERY] = searchQuery
            currentState.copy(
                isSearchActive = isVisible,
                searchQuery = searchQuery,
                books =
                    visibleBooks(
                        allBooks = sortedBooks,
                        filter = currentState.filter,
                        searchQuery = searchQuery,
                    ),
            )
        }
    }

    fun onSearchQueryChanged(searchQuery: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = searchQuery
        _uiState.update { currentState ->
            currentState.copy(
                searchQuery = searchQuery,
                books =
                    visibleBooks(
                        allBooks = sortedBooks,
                        filter = currentState.filter,
                        searchQuery = searchQuery,
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
                _effects.send(RecentEffect.ShowSnackbar("Could not add book to library"))
            } else {
                _effects.send(RecentEffect.ShowSnackbar("Added to library"))
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
            _effects.send(RecentEffect.ShowSnackbar("Removed from library"))
        }
    }

    fun onRemovalDismissed() {
        _uiState.update { it.copy(pendingRemoval = null) }
    }

    private suspend fun publishBooks(allBooks: List<Book>) {
        val recentBooks =
            withContext(Dispatchers.Default) {
                allBooks.sortedByDescending(Book::lastOpenedAt)
            }
        sortedBooks = recentBooks
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                books =
                    visibleBooks(
                        allBooks = recentBooks,
                        filter = currentState.filter,
                        searchQuery = currentState.searchQuery,
                    ),
                totalBookCount = allBooks.size,
            )
        }
    }

    private fun initialUiState(): RecentUiState =
        RecentUiState(
            filter = savedFilter(),
            isSearchActive = savedStateHandle[KEY_SEARCH_ACTIVE] ?: false,
            searchQuery = savedStateHandle[KEY_SEARCH_QUERY] ?: "",
        )

    private fun savedFilter(): BookFilter =
        BookFilter.entries.firstOrNull { it.name == savedStateHandle.get<String>(KEY_FILTER) }
            ?: BookFilter.All

    private fun visibleBooks(
        allBooks: List<Book>,
        filter: BookFilter,
        searchQuery: String,
    ): List<Book> {
        val matchingBooks =
            if (searchQuery.isBlank()) {
                allBooks
            } else {
                val normalizedQuery = searchQuery.trim()
                allBooks.filter { book -> book.title.contains(normalizedQuery, ignoreCase = true) }
            }
        return when (filter) {
            BookFilter.All,
            BookFilter.Pdf,
            -> matchingBooks

            BookFilter.Recent -> matchingBooks.filter { it.lastOpenedAt > 0 }

            BookFilter.Starred -> matchingBooks.filter(Book::starred)
        }
    }

    companion object {
        fun factory(bookRepository: BookRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    RecentViewModel(
                        bookRepository = bookRepository,
                        savedStateHandle = createSavedStateHandle(),
                    )
                }
            }

        private const val KEY_FILTER = "recent_filter"
        private const val KEY_SEARCH_ACTIVE = "recent_search_active"
        private const val KEY_SEARCH_QUERY = "recent_search_query"
    }
}
