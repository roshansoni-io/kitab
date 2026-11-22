package io.github.roshansoni_io.kitab.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.roshansoni_io.kitab.core.data.repository.BookRepository
import io.github.roshansoni_io.kitab.core.di.AppContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class KitabUiState(
    val dialog: KitabDialog? = null,
)

internal enum class KitabDialog {
    ImportFailed,
    OpenBookFailed,
}

internal sealed interface KitabEvent {
    data class DocumentPicked(
        val uri: String?,
    ) : KitabEvent

    data class ExternalUriReceived(
        val uri: String,
    ) : KitabEvent

    data class OpenBookRequested(
        val uri: String,
    ) : KitabEvent

    data object DismissDialog : KitabEvent

    data class ProgressChanged(
        val bookUri: String,
        val page: Int,
        val pageCount: Int,
    ) : KitabEvent
}

internal sealed interface KitabEffect {
    data class NavigateToReader(
        val bookUri: String,
    ) : KitabEffect

    data class ShowSnackbar(
        val message: String,
    ) : KitabEffect
}

internal class KitabViewModel(
    private val bookRepository: BookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KitabUiState())
    val uiState: StateFlow<KitabUiState> = _uiState.asStateFlow()

    private val _effects = Channel<KitabEffect>(Channel.BUFFERED)
    val effects: Flow<KitabEffect> = _effects.receiveAsFlow()

    fun onEvent(event: KitabEvent) {
        when (event) {
            is KitabEvent.DocumentPicked -> importBook(event.uri)
            is KitabEvent.ExternalUriReceived -> openBook(event.uri)
            is KitabEvent.OpenBookRequested -> openBook(event.uri)
            KitabEvent.DismissDialog -> _uiState.update { it.copy(dialog = null) }
            is KitabEvent.ProgressChanged -> updateProgress(event.bookUri, event.page, event.pageCount)
        }
    }

    private fun importBook(uri: String?) {
        if (uri == null) return
        viewModelScope.launch {
            val book = bookRepository.addBook(uri)
            if (book == null) {
                _uiState.update { it.copy(dialog = KitabDialog.ImportFailed) }
            } else {
                _effects.send(KitabEffect.ShowSnackbar("Added to library"))
            }
        }
    }

    private fun openBook(uri: String) {
        viewModelScope.launch {
            val book = bookRepository.openBook(uri)
            if (book == null) {
                _uiState.update { it.copy(dialog = KitabDialog.OpenBookFailed) }
            } else {
                _effects.send(KitabEffect.NavigateToReader(book.uri))
            }
        }
    }

    private fun updateProgress(
        bookUri: String,
        page: Int,
        pageCount: Int,
    ) {
        viewModelScope.launch {
            bookRepository.updateProgress(bookUri, page, pageCount)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    KitabViewModel(bookRepository = container.bookRepository)
                }
            }
    }
}
