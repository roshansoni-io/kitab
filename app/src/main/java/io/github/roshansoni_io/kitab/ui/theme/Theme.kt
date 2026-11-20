package io.github.roshansoni_io.kitab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.roshansoni_io.kitab.data.ThemeMode

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF00629D),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCFE4FF),
        onPrimaryContainer = Color(0xFF001D35),
        secondary = Color(0xFF526069),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD5E4EF),
        onSecondaryContainer = Color(0xFF0F1D24),
        tertiary = Color(0xFF605A7D),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE6DEFF),
        onTertiaryContainer = Color(0xFF1C1636),
        background = Color(0xFFFCFCFF),
        onBackground = Color(0xFF191C1F),
        surface = Color(0xFFFCFCFF),
        onSurface = Color(0xFF191C1F),
        surfaceVariant = Color(0xFFDEE3EB),
        onSurfaceVariant = Color(0xFF42474D),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF9CCAFF),
        onPrimary = Color(0xFF003355),
        primaryContainer = Color(0xFF004A79),
        onPrimaryContainer = Color(0xFFCFE4FF),
        secondary = Color(0xFFB9C8D3),
        onSecondary = Color(0xFF24323A),
        secondaryContainer = Color(0xFF3B4850),
        onSecondaryContainer = Color(0xFFD5E4EF),
        tertiary = Color(0xFFCAC1EA),
        onTertiary = Color(0xFF32284C),
        tertiaryContainer = Color(0xFF493F64),
        onTertiaryContainer = Color(0xFFE6DEFF),
        background = Color(0xFF191C1F),
        onBackground = Color(0xFFE2E2E6),
        surface = Color(0xFF191C1F),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF42474D),
        onSurfaceVariant = Color(0xFFC2C7CE),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
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
    content: @Composable () -> Unit,
) {
    val darkTheme = isDarkTheme(themeMode)
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                DarkColors
            }

            else -> {
                LightColors
            }
        }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content,
    )
}
