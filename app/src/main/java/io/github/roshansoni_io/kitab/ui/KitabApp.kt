package io.github.roshansoni_io.kitab.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.roshansoni_io.kitab.data.Book
import io.github.roshansoni_io.kitab.data.BookRepository
import io.github.roshansoni_io.kitab.data.SettingsRepository
import io.github.roshansoni_io.kitab.reader.ui.ReaderScreen
import kotlinx.coroutines.launch

private sealed interface KitabScreen {
    data object Home : KitabScreen

    data class Reading(
        val book: Book,
    ) : KitabScreen
}

@Composable
fun KitabRoot(
    pendingExternalUri: Uri?,
    onExternalUriHandled: () -> Unit,
    settingsRepository: SettingsRepository,
) {
    val context = LocalContext.current
    val repository = remember { BookRepository(context) }
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf<KitabScreen>(KitabScreen.Home) }
    var isImportFailedDialogVisible by remember { mutableStateOf(false) }
    var isAccessDialogVisible by remember { mutableStateOf(false) }
    var pendingAccessUri by remember { mutableStateOf<Uri?>(null) }

    fun openBook(uri: Uri) {
        if (!hasLibraryAccess(context)) {
            pendingAccessUri = uri
            isAccessDialogVisible = true
            return
        }
        scope.launch {
            val book = repository.openBook(uri)
            if (book != null) {
                screen = KitabScreen.Reading(book)
            } else {
                isImportFailedDialogVisible = true
            }
        }
    }

    val settingsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {
            val uri = pendingAccessUri
            pendingAccessUri = null
            if (uri != null && hasLibraryAccess(context)) {
                openBook(uri)
            }
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            val uri = pendingAccessUri
            pendingAccessUri = null
            if (uri != null && (granted || hasLibraryAccess(context))) {
                openBook(uri)
            }
        }

    LaunchedEffect(pendingExternalUri) {
        pendingExternalUri?.let { uri ->
            openBook(uri)
            onExternalUriHandled()
        }
    }

    when (val currentScreen = screen) {
        KitabScreen.Home -> {
            HomeScreen(
                repository = repository,
                settingsRepository = settingsRepository,
                onOpenBook = { book -> screen = KitabScreen.Reading(book) },
            )
        }

        is KitabScreen.Reading -> {
            ReaderScreen(
                book = currentScreen.book,
                resumePage = currentScreen.book.lastPage,
                onProgress = { page, pageCount ->
                    scope.launch {
                        repository.updateProgress(currentScreen.book.uri, page, pageCount)
                    }
                },
                onBack = { screen = KitabScreen.Home },
                onOpenExternalLink = { uri -> openExternalLink(context, uri) },
            )
        }
    }

    if (isAccessDialogVisible) {
        AlertDialog(
            onDismissRequest = {
                isAccessDialogVisible = false
                pendingAccessUri = null
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isAccessDialogVisible = false
                        pendingAccessUri = null
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
                            "Allow access to all files to open this PDF.",
                )
            },
        )
    }

    if (isImportFailedDialogVisible) {
        AlertDialog(
            onDismissRequest = { isImportFailedDialogVisible = false },
            confirmButton = {
                TextButton(
                    onClick = { isImportFailedDialogVisible = false },
                ) {
                    Text(text = "OK")
                }
            },
            title = { Text(text = "Something went wrong") },
            text = { Text(text = "This book could not be opened.") },
        )
    }
}

private fun openExternalLink(
    context: Context,
    uri: String,
) {
    try {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    } catch (notFound: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link.", Toast.LENGTH_SHORT).show()
    }
}
