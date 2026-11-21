package io.github.roshansoni_io.kitab.navigation

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.roshansoni_io.kitab.core.di.AppContainer
import io.github.roshansoni_io.kitab.core.platform.launchLibraryAccessSettings
import io.github.roshansoni_io.kitab.core.platform.openExternalLink
import io.github.roshansoni_io.kitab.core.ui.components.ImportFailedDialog
import io.github.roshansoni_io.kitab.core.ui.components.StorageAccessDialog
import io.github.roshansoni_io.kitab.feature.library.LibraryRoute
import io.github.roshansoni_io.kitab.feature.reader.ReaderRoute
import io.github.roshansoni_io.kitab.feature.recent.RecentRoute
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

    val settingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onEvent(
                KitabEvent.StorageAccessResult(isGranted = container.storageAccess.hasAccess()),
            )
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.onEvent(KitabEvent.StorageAccessResult(isGranted))
        }
    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            viewModel.onEvent(KitabEvent.DocumentPicked(uri?.toString()))
        }

    LaunchedEffect(viewModel, navController, showSnackbar) {
        viewModel.effects.collect { effect ->
            when (effect) {
                KitabEffect.OpenDocumentPicker -> documentPicker.launch(arrayOf("application/pdf"))
                KitabEffect.OpenStorageAccessSettings -> {
                    launchLibraryAccessSettings(context, settingsLauncher)
                }
                KitabEffect.RequestLegacyPermission -> {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                is KitabEffect.NavigateToReader -> {
                    navController.navigate(KitabDestinations.reader(effect.bookUri)) {
                        launchSingleTop = true
                    }
                }
                is KitabEffect.OpenExternalLink -> openExternalLink(context, effect.url)
                is KitabEffect.ShowSnackbar -> showSnackbar(effect.message)
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
                RecentRoute(
                    container = container,
                    onOpenBook = { uri -> viewModel.onEvent(KitabEvent.OpenBookRequested(uri)) },
                    onAddBook = { viewModel.onEvent(KitabEvent.AddBookRequested) },
                    onShowSnackbar = showSnackbar,
                )
            }
            composable(KitabTab.Library.route) {
                LibraryRoute(
                    container = container,
                    onOpenBook = { uri -> viewModel.onEvent(KitabEvent.OpenBookRequested(uri)) },
                    onAddBook = { viewModel.onEvent(KitabEvent.AddBookRequested) },
                    onShowSnackbar = showSnackbar,
                )
            }
            composable(KitabTab.Settings.route) {
                SettingsRoute(container = container)
            }
            composable(
                route = KitabDestinations.READER_ROUTE,
                arguments =
                    listOf(
                        navArgument(KitabDestinations.BOOK_URI_ARGUMENT) {
                            type = NavType.StringType
                        },
                    ),
            ) { backStackEntry ->
                val bookUri = backStackEntry.arguments?.getString(KitabDestinations.BOOK_URI_ARGUMENT)
                if (bookUri == null) {
                    Text(text = "Book unavailable", modifier = Modifier.padding(24.dp))
                } else {
                    ReaderRoute(
                        bookUri = bookUri,
                        bookRepository = container.bookRepository,
                        settingsRepository = container.settingsRepository,
                        onProgressChanged = { uri, page, pageCount ->
                            viewModel.onEvent(KitabEvent.ProgressChanged(uri, page, pageCount))
                        },
                        onBack = { navController.popBackStack() },
                        onOpenExternalLink = { url -> viewModel.onEvent(KitabEvent.OpenExternalLink(url)) },
                    )
                }
            }
        }
    }

    when (uiState.dialog) {
        KitabDialog.StorageAccessRequired -> {
            StorageAccessDialog(
                message = "Allow access to add or open PDF books stored on your device.",
                onConfirm = { viewModel.onEvent(KitabEvent.RequestStorageAccess) },
                onDismiss = { viewModel.onEvent(KitabEvent.DismissDialog) },
            )
        }
        KitabDialog.ImportFailed -> {
            ImportFailedDialog(
                message = "This PDF could not be added to your library.",
                onDismiss = { viewModel.onEvent(KitabEvent.DismissDialog) },
            )
        }
        KitabDialog.OpenBookFailed -> {
            ImportFailedDialog(
                message = "This book could not be opened.",
                onDismiss = { viewModel.onEvent(KitabEvent.DismissDialog) },
            )
        }
        null -> Unit
    }
}
