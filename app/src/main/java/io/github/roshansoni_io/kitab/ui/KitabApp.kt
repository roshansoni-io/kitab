package io.github.roshansoni_io.kitab.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import io.github.roshansoni_io.kitab.reader.ui.ReaderScreen
import kotlinx.coroutines.launch

private sealed interface KitabScreen {
    data object Home : KitabScreen

    data class Reading(
        val book: Book,
    ) : KitabScreen
}

/** Navigates between the home screen and the reader; entry point from [MainActivity]. */
@Composable
fun KitabRoot(
    pendingExternalUri: Uri?,
    onExternalUriHandled: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { BookRepository(context) }
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf<KitabScreen>(KitabScreen.Home) }
    var isImportFailedDialogVisible by remember { mutableStateOf(false) }

    fun openBook(uri: Uri) {
        scope.launch {
            val book = repository.importBook(uri)
            if (book != null) {
                screen = KitabScreen.Reading(book)
            } else {
                isImportFailedDialogVisible = true
            }
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
                onOpenBook = { book -> screen = KitabScreen.Reading(book) },
            )
        }

        is KitabScreen.Reading -> {
            ReaderScreen(
                book = currentScreen.book,
                onBack = { screen = KitabScreen.Home },
                onOpenExternalLink = { uri -> openExternalLink(context, uri) },
            )
        }
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

/** Opens [uri] in an external browser app, telling the user when none is installed. */
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
