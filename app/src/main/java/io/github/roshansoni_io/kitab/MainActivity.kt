package io.github.roshansoni_io.kitab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.roshansoni_io.kitab.book.Book
import io.github.roshansoni_io.kitab.book.BookRepository
import io.github.roshansoni_io.kitab.home.HomeScreen
import io.github.roshansoni_io.kitab.reader.ui.ReaderScreen
import io.github.roshansoni_io.kitab.ui.theme.KitabTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingExternalBookUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingExternalBookUri.value = externalBookUri(intent)
        setContent {
            KitabTheme {
                KitabRoot(
                    pendingExternalUri = pendingExternalBookUri.value,
                    onExternalUriHandled = { pendingExternalBookUri.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingExternalBookUri.value = externalBookUri(intent)
    }

    private fun externalBookUri(intent: Intent): Uri? = intent.takeIf { it.action == Intent.ACTION_VIEW }?.data
}

private sealed interface KitabScreen {
    data object Home : KitabScreen

    data class Reading(
        val book: Book,
    ) : KitabScreen
}

private fun openExternalLink(
    context: android.content.Context,
    uri: String,
) {
    try {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    } catch (notFound: android.content.ActivityNotFoundException) {
        android.widget.Toast
            .makeText(context, "No app can open this link.", android.widget.Toast.LENGTH_SHORT)
            .show()
    }
}

@Composable
private fun KitabRoot(
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
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { isImportFailedDialogVisible = false },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { isImportFailedDialogVisible = false },
                ) {
                    androidx.compose.material3.Text(text = "OK")
                }
            },
            title = { androidx.compose.material3.Text(text = "Something went wrong") },
            text = { androidx.compose.material3.Text(text = "This book could not be opened.") },
        )
    }
}
