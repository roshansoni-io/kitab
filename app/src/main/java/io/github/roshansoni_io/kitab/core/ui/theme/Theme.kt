package io.github.roshansoni_io.kitab.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.core.data.model.AppPalette

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF8A5200),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDDB5),
        onPrimaryContainer = Color(0xFF2C1600),
        secondary = Color(0xFF745B3C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDDB5),
        onSecondaryContainer = Color(0xFF2A1700),
        tertiary = Color(0xFF6C5D2F),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF5E1A7),
        onTertiaryContainer = Color(0xFF231B00),
        background = Color(0xFFFFF8F1),
        onBackground = Color(0xFF211A13),
        surface = Color(0xFFFFF8F1),
        onSurface = Color(0xFF211A13),
        surfaceVariant = Color(0xFFF2E0CD),
        onSurfaceVariant = Color(0xFF514437),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFF2E3),
        surfaceContainer = Color(0xFFF9EBDC),
        surfaceContainerHigh = Color(0xFFF3E5D6),
        surfaceContainerHighest = Color(0xFFEDDFD0),
        outline = Color(0xFF837462),
        outlineVariant = Color(0xFFD5C3B0),
        inverseSurface = Color(0xFF373029),
        inverseOnSurface = Color(0xFFFCEEE2),
        inversePrimary = Color(0xFFFFB951),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFFFC14D),
        onPrimary = Color(0xFF3E2A00),
        primaryContainer = Color(0xFF5C3D00),
        onPrimaryContainer = Color(0xFFFFDDB5),
        secondary = Color(0xFFE9C79A),
        onSecondary = Color(0xFF3F2D12),
        secondaryContainer = Color(0xFF3A2C1B),
        onSecondaryContainer = Color(0xFFF5DFC0),
        tertiary = Color(0xFFF3D9A6),
        onTertiary = Color(0xFF3C2F12),
        tertiaryContainer = Color(0xFF54461F),
        onTertiaryContainer = Color(0xFFF8E3AE),
        background = Color(0xFF150F08),
        onBackground = Color(0xFFF3E7D7),
        surface = Color(0xFF1A130B),
        onSurface = Color(0xFFF3E7D7),
        surfaceVariant = Color(0xFF3A2E20),
        onSurfaceVariant = Color(0xFFCDB89E),
        surfaceContainerLowest = Color(0xFF0F0A05),
        surfaceContainerLow = Color(0xFF1E160D),
        surfaceContainer = Color(0xFF241B11),
        surfaceContainerHigh = Color(0xFF2C2216),
        surfaceContainerHighest = Color(0xFF35291B),
        outline = Color(0xFF6E5B44),
        outlineVariant = Color(0xFF3B2E20),
        inverseSurface = Color(0xFFF3E7D7),
        inverseOnSurface = Color(0xFF35291B),
        inversePrimary = Color(0xFF8A5200),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

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
    appPalette: AppPalette = AppPalette.Amber,
    content: @Composable () -> Unit,
) {
    val darkTheme = isDarkTheme(themeMode)
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> paletteColors(DarkColors, appPalette, darkTheme = true)

            else -> paletteColors(LightColors, appPalette, darkTheme = false)
        }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = KitabShapes,
        typography = Typography,
        content = content,
    )
}

private fun paletteColors(base: androidx.compose.material3.ColorScheme, palette: AppPalette, darkTheme: Boolean) =
    when (palette) {
        AppPalette.Amber -> base
        AppPalette.Ocean -> base.copy(
            primary = if (darkTheme) Color(0xFF9CCBFF) else Color(0xFF005A9E),
            onPrimary = if (darkTheme) Color(0xFF003257) else Color.White,
            primaryContainer = if (darkTheme) Color(0xFF004778) else Color(0xFFD2E4FF),
            onPrimaryContainer = if (darkTheme) Color(0xFFD2E4FF) else Color(0xFF001C37),
            secondary = if (darkTheme) Color(0xFFB7C8E2) else Color(0xFF526175),
            tertiary = if (darkTheme) Color(0xFFD7BDE4) else Color(0xFF6C5678),
        )
        AppPalette.Forest -> base.copy(
            primary = if (darkTheme) Color(0xFF9FD4A7) else Color(0xFF286333),
            onPrimary = if (darkTheme) Color(0xFF073914) else Color.White,
            primaryContainer = if (darkTheme) Color(0xFF174B23) else Color(0xFFD0E8D0),
            onPrimaryContainer = if (darkTheme) Color(0xFFB7F0BE) else Color(0xFF06210B),
            secondary = if (darkTheme) Color(0xFFB9CCB7) else Color(0xFF536451),
            tertiary = if (darkTheme) Color(0xFFA5D0C8) else Color(0xFF3E665E),
        )
        AppPalette.Rose -> base.copy(
            primary = if (darkTheme) Color(0xFFFFB1C1) else Color(0xFF9C3F59),
            onPrimary = if (darkTheme) Color(0xFF650D2A) else Color.White,
            primaryContainer = if (darkTheme) Color(0xFF812640) else Color(0xFFFFD9E0),
            onPrimaryContainer = if (darkTheme) Color(0xFFFFD9E0) else Color(0xFF3E0016),
            secondary = if (darkTheme) Color(0xFFE2BDC5) else Color(0xFF75565D),
            tertiary = if (darkTheme) Color(0xFFE2C2A0) else Color(0xFF765A38),
        )
    }
