package io.github.roshansoni_io.kitab.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.roshansoni_io.kitab.core.data.repository.BookRepository
import io.github.roshansoni_io.kitab.core.di.AppContainer
import io.github.roshansoni_io.kitab.core.platform.StorageAccess
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
    StorageAccessRequired,
    ImportFailed,
    OpenBookFailed,
}

internal sealed interface KitabEvent {
    data object AddBookRequested : KitabEvent

    data class DocumentPicked(
        val uri: String?,
    ) : KitabEvent

    data class ExternalUriReceived(
        val uri: String,
    ) : KitabEvent

    data class OpenBookRequested(
        val uri: String,
    ) : KitabEvent

    data object RequestStorageAccess : KitabEvent

    data class StorageAccessResult(
        val isGranted: Boolean,
    ) : KitabEvent

    data object DismissDialog : KitabEvent

    data class ProgressChanged(
        val bookUri: String,
        val page: Int,
        val pageCount: Int,
    ) : KitabEvent

    data class OpenExternalLink(
        val url: String,
    ) : KitabEvent
}

internal sealed interface KitabEffect {
    data object OpenDocumentPicker : KitabEffect

    data object OpenStorageAccessSettings : KitabEffect

    data object RequestLegacyPermission : KitabEffect

    data class NavigateToReader(
        val bookUri: String,
    ) : KitabEffect

    data class OpenExternalLink(
        val url: String,
    ) : KitabEffect

    data class ShowSnackbar(
        val message: String,
    ) : KitabEffect
}

internal class KitabViewModel(
    private val bookRepository: BookRepository,
    private val storageAccess: StorageAccess,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KitabUiState())
    val uiState: StateFlow<KitabUiState> = _uiState.asStateFlow()

    private val _effects = Channel<KitabEffect>(Channel.BUFFERED)
    val effects: Flow<KitabEffect> = _effects.receiveAsFlow()

    private var pendingExternalUri: String?
        get() = savedStateHandle[KEY_PENDING_EXTERNAL_URI]
        set(uri) {
            savedStateHandle[KEY_PENDING_EXTERNAL_URI] = uri
        }

    private var isDocumentPickerPending: Boolean
        get() = savedStateHandle[KEY_DOCUMENT_PICKER_PENDING] ?: false
        set(isPending) {
            savedStateHandle[KEY_DOCUMENT_PICKER_PENDING] = isPending
        }

    fun onEvent(event: KitabEvent) {
        when (event) {
            KitabEvent.AddBookRequested -> requestBookImport()
            is KitabEvent.DocumentPicked -> importBook(event.uri)
            is KitabEvent.ExternalUriReceived -> openExternalBook(event.uri)
            is KitabEvent.OpenBookRequested -> openBook(event.uri)
            KitabEvent.RequestStorageAccess -> requestStorageAccess()
            is KitabEvent.StorageAccessResult -> onStorageAccessResult(event.isGranted)
            KitabEvent.DismissDialog -> dismissDialog()
            is KitabEvent.ProgressChanged -> updateProgress(event.bookUri, event.page, event.pageCount)
            is KitabEvent.OpenExternalLink -> _effects.trySend(KitabEffect.OpenExternalLink(event.url))
        }
    }

    private fun requestBookImport() {
        if (storageAccess.hasAccess()) {
            _effects.trySend(KitabEffect.OpenDocumentPicker)
            return
        }
        isDocumentPickerPending = true
        pendingExternalUri = null
        _uiState.update { it.copy(dialog = KitabDialog.StorageAccessRequired) }
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

    private fun openExternalBook(uri: String) {
        if (!storageAccess.hasAccess()) {
            pendingExternalUri = uri
            isDocumentPickerPending = false
            _uiState.update { it.copy(dialog = KitabDialog.StorageAccessRequired) }
            return
        }
        openBook(uri)
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

    private fun onStorageAccessResult(isGranted: Boolean) {
        val externalUri = pendingExternalUri
        val shouldOpenDocumentPicker = isDocumentPickerPending
        pendingExternalUri = null
        isDocumentPickerPending = false

        val hasAccess = isGranted || storageAccess.hasAccess()
        _uiState.update { it.copy(dialog = null) }
        if (!hasAccess) return

        when {
            externalUri != null -> openExternalBook(externalUri)
            shouldOpenDocumentPicker -> _effects.trySend(KitabEffect.OpenDocumentPicker)
        }
    }

    private fun requestStorageAccess() {
        if (storageAccess.usesLegacyPermission) {
            _effects.trySend(KitabEffect.RequestLegacyPermission)
        } else {
            _effects.trySend(KitabEffect.OpenStorageAccessSettings)
        }
    }

    private fun dismissDialog() {
        pendingExternalUri = null
        isDocumentPickerPending = false
        _uiState.update { it.copy(dialog = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    KitabViewModel(
                        bookRepository = container.bookRepository,
                        storageAccess = container.storageAccess,
                        savedStateHandle = createSavedStateHandle(),
                    )
                }
            }

        private const val KEY_PENDING_EXTERNAL_URI = "pending_external_uri"
        private const val KEY_DOCUMENT_PICKER_PENDING = "document_picker_pending"
    }
}
