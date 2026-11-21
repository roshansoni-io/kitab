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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    init {
        viewModelScope.launch {
            bookRepository.books.collectLatest { allBooks ->
                publishBooks(allBooks)
                loadCovers(allBooks)
            }
        }
    }

    fun onFilterChanged(filter: BookFilter) {
        savedStateHandle[KEY_FILTER] = filter.name
        _uiState.update { currentState ->
            currentState.copy(
                filter = filter,
                books = visibleBooks(
                    allBooks = bookRepository.books.value,
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
                books = visibleBooks(
                    allBooks = bookRepository.books.value,
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
                books = visibleBooks(
                    allBooks = bookRepository.books.value,
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

    private fun publishBooks(allBooks: List<Book>) {
        _uiState.update { currentState ->
            currentState.copy(
                isLoading = false,
                books = visibleBooks(
                    allBooks = allBooks,
                    filter = currentState.filter,
                    searchQuery = currentState.searchQuery,
                ),
                totalBookCount = allBooks.size,
            )
        }
    }

    private suspend fun loadCovers(allBooks: List<Book>) {
        val knownUris = allBooks.mapTo(mutableSetOf(), Book::uri)
        _uiState.update { currentState ->
            currentState.copy(covers = currentState.covers.filterKeys(knownUris::contains))
        }
        val missingBooks = allBooks.filter { it.uri !in _uiState.value.covers }
        val loadedCovers =
            missingBooks.mapNotNull { book ->
                bookRepository.loadCover(book)?.let { bitmap -> book.uri to bitmap.asImageBitmap() }
            }.toMap()
        if (loadedCovers.isNotEmpty()) {
            _uiState.update { currentState -> currentState.copy(covers = currentState.covers + loadedCovers) }
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
        val recentBooks = allBooks.sortedByDescending(Book::lastOpenedAt)
        val matchingBooks =
            if (searchQuery.isBlank()) {
                recentBooks
            } else {
                val normalizedQuery = searchQuery.trim()
                recentBooks.filter { book -> book.title.contains(normalizedQuery, ignoreCase = true) }
            }
        return when (filter) {
            BookFilter.All,
            BookFilter.Pdf -> matchingBooks
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
