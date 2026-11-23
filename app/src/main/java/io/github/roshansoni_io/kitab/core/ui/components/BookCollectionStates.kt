package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.BookFilter

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoadingState(
    message: String,
    contentPadding: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LoadingIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
internal fun EmptyLibraryState(contentPadding: PaddingValues) {
    EmptyState(
        title = "No books yet",
        message = "Tap Add Book or open a book from another app to start your collection.",
        contentPadding = contentPadding,
    )
}

@Composable
internal fun EmptyFilterState(
    filter: BookFilter,
    contentPadding: PaddingValues,
) {
    val (title, message) =
        when (filter) {
            BookFilter.All -> "No books yet" to "Tap Add Book to start your collection."
            BookFilter.Recent -> "Nothing opened yet" to "Books you open will show up here."
            BookFilter.Starred -> "No starred books" to "Tap the star on a book to save it here."
        }
    EmptyState(title = title, message = message, contentPadding = contentPadding)
}

@Composable
internal fun SearchEmptyState(
    query: String,
    contentPadding: PaddingValues,
) {
    EmptyState(
        title = "No results",
        message = "No books match \"${query.trim()}\"",
        contentPadding = contentPadding,
    )
}

@Composable
private fun EmptyState(
    title: String,
    message: String,
    contentPadding: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_kitab_logo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
