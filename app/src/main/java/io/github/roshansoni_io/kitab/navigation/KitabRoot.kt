package io.github.roshansoni_io.kitab.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.roshansoni_io.kitab.core.di.AppContainer
import io.github.roshansoni_io.kitab.core.platform.openExternalLink
import io.github.roshansoni_io.kitab.core.ui.components.ImportFailedDialog
import io.github.roshansoni_io.kitab.feature.collection.BookCollectionRoute
import io.github.roshansoni_io.kitab.feature.collection.BookCollectionSource
import io.github.roshansoni_io.kitab.feature.reader.ReaderRoute
import io.github.roshansoni_io.kitab.feature.settings.SettingsRoute

@Composable
fun KitabRoot(
    container: AppContainer,
    pendingExternalUri: Uri?,
    onExternalUriHandled: () -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: KitabViewModel = viewModel(factory = KitabViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val showSnackbar: suspend (String) -> Unit =
        remember(snackbarHostState) {
            { message -> snackbarHostState.showSnackbar(message) }
        }

    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            viewModel.onEvent(KitabEvent.DocumentPicked(uri?.toString()))
        }

    LaunchedEffect(viewModel, navController, showSnackbar) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is KitabEffect.NavigateToReader -> {
                    navController.navigate(ReaderDestination(effect.bookUri)) {
                        launchSingleTop = true
                    }
                }

                is KitabEffect.ShowSnackbar -> {
                    showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(pendingExternalUri) {
        pendingExternalUri?.let { uri ->
            viewModel.onEvent(KitabEvent.ExternalUriReceived(uri.toString()))
            onExternalUriHandled()
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedTab = KitabDestinations.tabForRoute(currentBackStackEntry?.destination?.route)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            selectedTab?.let { currentTab ->
                AppNavigationBar(selectedTab = currentTab) { selectedDestination ->
                    navController.navigate(selectedDestination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = KitabTab.Recent.route,
            modifier = Modifier.padding(contentPadding),
        ) {
            composable(KitabTab.Recent.route) {
                BookCollectionRoute(
                    container = container,
                    source = BookCollectionSource.Recent,
                    onOpenBook = { uri -> viewModel.onEvent(KitabEvent.OpenBookRequested(uri)) },
                    onAddBook = { documentPicker.launch(BOOK_MIME_TYPES) },
                    onShowSnackbar = showSnackbar,
                )
            }
            composable(KitabTab.Library.route) {
                BookCollectionRoute(
                    container = container,
                    source = BookCollectionSource.Library,
                    onOpenBook = { uri -> viewModel.onEvent(KitabEvent.OpenBookRequested(uri)) },
                    onAddBook = { documentPicker.launch(BOOK_MIME_TYPES) },
                    onShowSnackbar = showSnackbar,
                )
            }
            composable(KitabTab.Settings.route) {
                SettingsRoute(container = container)
            }
            composable<ReaderDestination> { backStackEntry ->
                val destination = backStackEntry.toRoute<ReaderDestination>()
                ReaderRoute(
                    bookUri = destination.bookUri,
                    bookRepository = container.bookRepository,
                    settingsRepository = container.settingsRepository,
                    onProgressChanged = { uri, page, pageCount ->
                        viewModel.onEvent(KitabEvent.ProgressChanged(uri, page, pageCount))
                    },
                    onEpubProgressChanged = { uri, locatorJson, progression ->
                        viewModel.onEvent(KitabEvent.EpubProgressChanged(uri, locatorJson, progression))
                    },
                    onBack = { navController.popBackStack() },
                    onOpenExternalLink = { url -> openExternalLink(context, url) },
                )
            }
        }
    }

    when (uiState.dialog) {
        KitabDialog.ImportFailed -> {
            ImportFailedDialog(
                message = "This book could not be added to your library.",
                onDismiss = { viewModel.onEvent(KitabEvent.DismissDialog) },
            )
        }

        KitabDialog.OpenBookFailed -> {
            ImportFailedDialog(
                message = "This book could not be opened.",
                onDismiss = { viewModel.onEvent(KitabEvent.DismissDialog) },
            )
        }

        null -> {
            Unit
        }
    }
}

private val BOOK_MIME_TYPES = arrayOf("application/pdf", "application/epub+zip")
