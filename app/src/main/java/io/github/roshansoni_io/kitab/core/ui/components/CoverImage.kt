package io.github.roshansoni_io.kitab.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale

@Composable
internal fun CoverImage(
    cover: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RectangleShape,
        color = Color.Transparent,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
