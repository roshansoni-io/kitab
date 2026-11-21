package io.github.roshansoni_io.kitab.feature.reader

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LinkConfirmChip(
    uri: String,
    onOpen: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = remember(uri) { runCatching { Uri.parse(uri).host }.getOrNull() }
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = host?.let { "Open $it?" } ?: "Open link?",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { onOpen(uri) }) {
                Text(text = "Open")
            }
            TextButton(onClick = onClose) {
                Text(text = "Close")
            }
        }
    }
}
