package io.github.roshansoni_io.kitab.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.roshansoni_io.kitab.core.di.AppContainer

@Composable
internal fun LibraryRoute(
    container: AppContainer,
    onOpenBook: (String) -> Unit,
    onAddBook: () -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
) {
    val libraryViewModel: LibraryViewModel =
        viewModel(factory = LibraryViewModel.factory(container.bookRepository, container.settingsRepository))
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(libraryViewModel, onShowSnackbar) {
        libraryViewModel.effects.collect { effect ->
            when (effect) {
                is LibraryEffect.ShowSnackbar -> onShowSnackbar(effect.message)
            }
        }
    }

    LibraryScreen(
        uiState = uiState,
        onFilterChanged = libraryViewModel::onFilterChanged,
        onListViewChanged = libraryViewModel::onListViewChanged,
        onSearchVisibilityChanged = libraryViewModel::onSearchVisibilityChanged,
        onSearchQueryChanged = libraryViewModel::onSearchQueryChanged,
        onOpenBook = { book -> onOpenBook(book.uri) },
        onAddBook = onAddBook,
        onStarRequested = libraryViewModel::onStarRequested,
        onRemoveRequested = libraryViewModel::onRemoveRequested,
        onRemovalConfirmed = libraryViewModel::onRemovalConfirmed,
        onRemovalDismissed = libraryViewModel::onRemovalDismissed,
    )
}
