package io.github.roshansoni_io.kitab.feature.recent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.roshansoni_io.kitab.core.data.model.Book
import io.github.roshansoni_io.kitab.core.data.model.BookFilter
import io.github.roshansoni_io.kitab.core.di.AppContainer

@Composable
internal fun RecentRoute(
    container: AppContainer,
    onOpenBook: (String) -> Unit,
    onAddBook: () -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
) {
    val recentViewModel: RecentViewModel =
        viewModel(factory = RecentViewModel.factory(container.bookRepository))
    val uiState by recentViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(recentViewModel, onShowSnackbar) {
        recentViewModel.effects.collect { effect ->
            when (effect) {
                is RecentEffect.ShowSnackbar -> onShowSnackbar(effect.message)
            }
        }
    }

    RecentScreen(
        uiState = uiState,
        onFilterChanged = recentViewModel::onFilterChanged,
        onSearchVisibilityChanged = recentViewModel::onSearchVisibilityChanged,
        onSearchQueryChanged = recentViewModel::onSearchQueryChanged,
        onOpenBook = { book -> onOpenBook(book.uri) },
        onAddBook = onAddBook,
        onStarRequested = recentViewModel::onStarRequested,
        onRemoveRequested = recentViewModel::onRemoveRequested,
        onRemovalConfirmed = recentViewModel::onRemovalConfirmed,
        onRemovalDismissed = recentViewModel::onRemovalDismissed,
    )
}
