package io.github.roshansoni_io.kitab.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.roshansoni_io.kitab.core.di.AppContainer

@Composable
internal fun BookCollectionRoute(
    container: AppContainer,
    source: BookCollectionSource,
    onOpenBook: (String) -> Unit,
    onAddBook: () -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
) {
    val collectionViewModel: BookCollectionViewModel =
        viewModel(
            factory =
                BookCollectionViewModel.factory(
                    bookRepository = container.bookRepository,
                    settingsRepository = container.settingsRepository,
                    source = source,
                ),
        )
    val uiState by collectionViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(collectionViewModel, onShowSnackbar) {
        collectionViewModel.effects.collect { effect ->
            when (effect) {
                is BookCollectionEffect.ShowSnackbar -> onShowSnackbar(effect.message)
            }
        }
    }

    BookCollectionScreen(
        uiState = uiState,
        source = source,
        onFilterChanged = collectionViewModel::onFilterChanged,
        onListViewChanged = collectionViewModel::onListViewChanged,
        onSearchVisibilityChanged = collectionViewModel::onSearchVisibilityChanged,
        onSearchQueryChanged = collectionViewModel::onSearchQueryChanged,
        onOpenBook = { book -> onOpenBook(book.uri) },
        onCoverRequested = collectionViewModel::onCoverRequested,
        onAddBook = onAddBook,
        onStarRequested = collectionViewModel::onStarRequested,
        onAddToLibrary = collectionViewModel::onAddToLibrary,
        onRemoveRequested = collectionViewModel::onRemoveRequested,
        onRemovalConfirmed = collectionViewModel::onRemovalConfirmed,
        onRemovalDismissed = collectionViewModel::onRemovalDismissed,
    )
}
