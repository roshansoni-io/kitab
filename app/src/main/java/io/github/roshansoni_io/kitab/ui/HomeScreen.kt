package io.github.roshansoni_io.kitab.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import io.github.roshansoni_io.kitab.data.Book
import io.github.roshansoni_io.kitab.data.BookRepository
import io.github.roshansoni_io.kitab.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class HomeDestination(
    val label: String,
    val icon: ImageVector,
) {
    Recent("Recent", Icons.AutoMirrored.Filled.List),
    Home("Home", Icons.Filled.Home),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
fun HomeScreen(
    repository: BookRepository,
    settingsRepository: SettingsRepository,
    onOpenBook: (Book) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recents by remember { mutableStateOf(emptyList<Book>()) }
    var covers by remember { mutableStateOf(emptyMap<String, ImageBitmap>()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasAccess by remember { mutableStateOf(hasLibraryAccess(context)) }
    var selectedDestination by rememberSaveable { mutableStateOf(HomeDestination.Home) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var isImportFailedDialogVisible by remember { mutableStateOf(false) }
    var isAccessDialogVisible by remember { mutableStateOf(false) }
    var addAfterGrant by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<Book?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun refreshRecents() {
        scope.launch { recents = withContext(Dispatchers.IO) { repository.recentBooks() } }
    }

    val documentPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { pickedUri: Uri? ->
            if (pickedUri != null) {
                scope.launch {
                    val book = repository.addBook(pickedUri)
                    if (book != null) {
                        refreshRecents()
                        snackbarHostState.showSnackbar("Added to library")
                    } else {
                        isImportFailedDialogVisible = true
                    }
                }
            }
        }

    fun openLibraryPicker() {
        documentPicker.launch(arrayOf("application/pdf"))
    }

    val settingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            hasAccess = hasLibraryAccess(context)
            if (hasAccess && addAfterGrant) {
                addAfterGrant = false
                openLibraryPicker()
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasAccess = granted || hasLibraryAccess(context)
            if (hasAccess && addAfterGrant) {
                addAfterGrant = false
                openLibraryPicker()
            }
        }

    fun requestStorageAccess() {
        if (hasLibraryAccess(context)) {
            openLibraryPicker()
        } else {
            addAfterGrant = true
            isAccessDialogVisible = true
        }
    }

    LaunchedEffect(repository) {
        recents = withContext(Dispatchers.IO) { repository.recentBooks() }
        isLoading = false
    }

    LaunchedEffect(recents) {
        recents.forEach { book ->
            if (book.uri !in covers) {
                repository.loadCover(book)?.let { bitmap ->
                    covers = covers + (book.uri to bitmap.asImageBitmap())
                }
            }
        }
    }

    fun filtered(books: List<Book>): List<Book> =
        if (query.isBlank()) {
            books
        } else {
            books.filter { it.title.contains(query.trim(), ignoreCase = true) }
        }
    val libraryBooks = filtered(recents.filter(Book::inLibrary))
    val recentBooks = filtered(recents.sortedByDescending(Book::lastOpenedAt))
    val onSearchActiveChange: (Boolean) -> Unit = { active ->
        isSearchActive = active
        if (!active) {
            query = ""
        }
    }

    Scaffold(
        topBar = {
            when (selectedDestination) {
                HomeDestination.Home -> {
                    LibraryTopBar(
                        title = "Kitab",
                        isSearchActive = isSearchActive,
                        query = query,
                        onSearchActiveChange = onSearchActiveChange,
                        onQueryChange = { query = it },
                    )
                }

                HomeDestination.Recent -> {
                    LibraryTopBar(
                        title = "Recent",
                        isSearchActive = isSearchActive,
                        query = query,
                        onSearchActiveChange = onSearchActiveChange,
                        onQueryChange = { query = it },
                    )
                }

                HomeDestination.Settings -> {
                    SettingsTopBar()
                }
            }
        },
        bottomBar = {
            HomeNavigationBar(
                selectedDestination = selectedDestination,
                onSelectDestination = { destination ->
                    selectedDestination = destination
                    isSearchActive = false
                    query = ""
                },
            )
        },
        floatingActionButton = {
            if (selectedDestination != HomeDestination.Settings) {
                ExtendedFloatingActionButton(
                    icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                    text = { Text(text = "Add a book") },
                    onClick = ::requestStorageAccess,
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { contentPadding ->
        when (selectedDestination) {
            HomeDestination.Home -> {
                LibraryContent(
                    isLoading = isLoading,
                    books = libraryBooks,
                    covers = covers,
                    query = query,
                    contentPadding = contentPadding,
                    onOpenBook = onOpenBook,
                    onRequestRemove = { pendingRemoval = it },
                )
            }

            HomeDestination.Recent -> {
                LibraryContent(
                    isLoading = isLoading,
                    books = recentBooks,
                    covers = covers,
                    query = query,
                    contentPadding = contentPadding,
                    onOpenBook = onOpenBook,
                    onRequestRemove = { pendingRemoval = it },
                )
            }

            HomeDestination.Settings -> {
                SettingsTabContent(
                    settingsRepository = settingsRepository,
                    contentPadding = contentPadding,
                )
            }
        }
    }

    val bookPendingRemoval = pendingRemoval
    if (bookPendingRemoval != null) {
        RemoveDialog(
            book = bookPendingRemoval,
            onConfirm = { book ->
                pendingRemoval = null
                scope.launch {
                    repository.removeBook(book.uri)
                    refreshRecents()
                    snackbarHostState.showSnackbar("Removed from library")
                }
            },
            onDismiss = { pendingRemoval = null },
        )
    }

    if (isAccessDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                isAccessDialogVisible = false
                addAfterGrant = false
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAccessDialogVisible = false
                        addAfterGrant = false
                    },
                ) {
                    Text(text = "Not now")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isAccessDialogVisible = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            launchLibraryAccessSettings(context, settingsLauncher)
                        } else {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    },
                ) {
                    Text(text = "Allow access")
                }
            },
            title = { Text(text = "Storage access needed") },
            text = {
                Text(
                    text =
                        "Kitab saves books in /storage/emulated/0/Kitab. " +
                            "Allow access to all files so books can be added to your library.",
                )
            },
        )
    }

    if (isImportFailedDialogVisible) {
        AlertDialog(
            onDismissRequest = { isImportFailedDialogVisible = false },
            confirmButton = {
                TextButton(onClick = { isImportFailedDialogVisible = false }) { Text(text = "OK") }
            },
            title = { Text(text = "Something went wrong") },
            text = { Text(text = "This book could not be imported.") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar() {
    CenterAlignedTopAppBar(
        title = { Text(text = "Settings") },
    )
}

@Composable
private fun HomeNavigationBar(
    selectedDestination: HomeDestination,
    onSelectDestination: (HomeDestination) -> Unit,
) {
    NavigationBar {
        HomeDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onSelectDestination(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = { Text(text = destination.label) },
            )
        }
    }
}

@Composable
private fun RemoveDialog(
    book: Book,
    onConfirm: (Book) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(book) }) { Text(text = "Remove") }
        },
        title = { Text(text = "Remove from library?") },
        text = {
            Text(
                text =
                    "\"${book.title}\" will be removed from your library " +
                        "and its stored PDF will be deleted.",
            )
        },
    )
}
