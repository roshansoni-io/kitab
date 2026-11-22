package io.github.roshansoni_io.kitab.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.materialkolor.dynamicColorScheme
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val KitabShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

@Composable
fun isDarkTheme(themeMode: ThemeMode): Boolean =
    when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

@Composable
fun KitabTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    customSeedColor: Int = 0xFF6750A4.toInt(),
    content: @Composable () -> Unit,
) {
    val darkTheme = isDarkTheme(themeMode)
    val context = LocalContext.current
    val fallbackColorScheme: ColorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    var colorScheme by remember { mutableStateOf(fallbackColorScheme) }

    LaunchedEffect(darkTheme, dynamicColor, customSeedColor, context) {
        colorScheme =
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                withContext(Dispatchers.Default) {
                    dynamicColorScheme(
                        seedColor = Color(customSeedColor),
                        isDark = darkTheme,
                        isAmoled = false,
                    )
                }
            }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = KitabShapes,
        typography = Typography,
        content = content,
    )
}
