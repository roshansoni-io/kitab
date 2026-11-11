package io.github.roshansoni_io.kitab.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.data.Book
import io.github.roshansoni_io.kitab.data.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: BookRepository,
    onOpenBook: (Book) -> Unit,
) {
    var recents by remember { mutableStateOf(emptyList<Book>()) }
    var isAboutDialogVisible by remember { mutableStateOf(false) }
    var importFailedDialogVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        recents = withContext(Dispatchers.IO) { repository.recentBooks() }
    }
    val refreshRecents: () -> Unit = {
        scope.launch { recents = withContext(Dispatchers.IO) { repository.recentBooks() } }
    }
    val documentPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { pickedUri: Uri? ->
            if (pickedUri != null) {
                scope.launch {
                    repository
                        .importBook(pickedUri)
                        ?.let { book ->
                            onOpenBook(book)
                            refreshRecents()
                        }
                        ?: run { importFailedDialogVisible = true }
                }
            }
        }

    HomeScaffold(
        onShowAbout = { isAboutDialogVisible = true },
        onOpenLibraryPicker = { documentPicker.launch(arrayOf("application/pdf")) },
    ) { contentPadding ->
        if (recents.isEmpty()) {
            HomeEmptyState(
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            RecentBooksList(
                books = recents,
                contentPadding = contentPadding,
                onOpenBook = onOpenBook,
                onRemoveBook = { removed ->
                    scope.launch {
                        withContext(Dispatchers.IO) { repository.removeRecent(removed.uri) }
                        recents = withContext(Dispatchers.IO) { repository.recentBooks() }
                    }
                },
            )
        }
    }

    if (isAboutDialogVisible) {
        AboutDialog(onDismiss = { isAboutDialogVisible = false })
    }

    if (importFailedDialogVisible) {
        ErrorDialog(
            message = "This book could not be imported.",
            onDismiss = { importFailedDialogVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScaffold(
    onShowAbout: () -> Unit,
    onOpenLibraryPicker: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Kitab") },
                actions = {
                    IconButton(onClick = onShowAbout) {
                        Icon(imageVector = Icons.Filled.Info, contentDescription = "About")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                text = { Text(text = "Open a book") },
                onClick = onOpenLibraryPicker,
            )
        },
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "No books yet", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Tap 'Open a book' or open a PDF from another app",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun RecentBooksList(
    books: List<Book>,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenBook: (Book) -> Unit,
    onRemoveBook: (Book) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = 96.dp,
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        items(items = books, key = { it.uri }) { book ->
            ListItem(
                headlineContent = { Text(text = book.title) },
                supportingContent = { Text(text = "PDF") },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBook(book) },
                trailingContent = {
                    IconButton(onClick = { onRemoveBook(book) }) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Remove")
                    }
                },
            )
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "OK") }
        },
        title = { Text(text = "About Kitab") },
        text = { Text(text = ABOUT_TEXT) },
    )
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "OK") }
        },
        title = { Text(text = "Something went wrong") },
        text = { Text(text = message) },
    )
}

private const val ABOUT_TEXT =
    "Kitab is an open-source book reader.\n\n" +
        "PDF rendering is powered by MuPDF, Copyright (c) 2006-2026 Artifex Software, Inc., " +
        "licensed under the GNU Affero General Public License v3. " +
        "In accordance with the AGPL, the complete source code of this app is open source."
