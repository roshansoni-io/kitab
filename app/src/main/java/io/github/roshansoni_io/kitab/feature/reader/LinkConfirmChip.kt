package io.github.roshansoni_io.kitab.feature.reader

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinkConfirmChip(
    uri: String,
    onOpen: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = remember(uri) { runCatching { Uri.parse(uri).host }.getOrNull() }
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        content = {
            Text(
                text = host?.let { "Open $it?" } ?: "Open link?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .padding(start = 12.dp, end = 4.dp)
                        .align(Alignment.CenterVertically),
            )
            TextButton(onClick = { onOpen(uri) }) {
                Text(text = "Open")
            }
            TextButton(onClick = onClose) {
                Text(text = "Close")
            }
        },
    )
}
