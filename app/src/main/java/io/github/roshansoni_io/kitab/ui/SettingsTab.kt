package io.github.roshansoni_io.kitab.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.BuildConfig
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.data.SettingsRepository
import io.github.roshansoni_io.kitab.data.ThemeMode

@Composable
internal fun SettingsTabContent(
    settingsRepository: SettingsRepository,
    contentPadding: PaddingValues,
) {
    val settings by settingsRepository.settings.collectAsState()
    var isThemeDialogVisible by remember { mutableStateOf(false) }
    var isAboutDialogVisible by remember { mutableStateOf(false) }
    var areLicensesDialogVisible by remember { mutableStateOf(false) }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val layoutDirection = LocalLayoutDirection.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
                start = contentPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end = contentPadding.calculateEndPadding(layoutDirection) + 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            AppHeader()
        }

        item {
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    title = "Theme",
                    icon = Icons.Filled.Settings,
                    onClick = { isThemeDialogVisible = true },
                    trailing = {
                        Text(
                            text = settings.themeMode.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingsSwitchItem(
                    title = "Dynamic color",
                    supporting =
                        if (supportsDynamicColor) {
                            "Use colors from your wallpaper"
                        } else {
                            "Requires Android 12 or later"
                        },
                    icon = Icons.Filled.Star,
                    checked = settings.dynamicColor && supportsDynamicColor,
                    enabled = supportsDynamicColor,
                    onCheckedChange = settingsRepository::setDynamicColor,
                )
            }
        }

        item {
            SettingsSection(title = "About") {
                SettingsItem(
                    title = "About Kitab",
                    supporting = "An open-source PDF reader",
                    icon = Icons.Filled.Info,
                    onClick = { isAboutDialogVisible = true },
                )
                SettingsItem(
                    title = "Open-source licenses",
                    supporting = "MuPDF, AGPL-3.0",
                    icon = Icons.Filled.Lock,
                    onClick = { areLicensesDialogVisible = true },
                )
            }
        }
    }

    if (isThemeDialogVisible) {
        ThemeDialog(
            selected = settings.themeMode,
            onSelect = { mode -> settingsRepository.setThemeMode(mode) },
            onDismiss = { isThemeDialogVisible = false },
        )
    }

    if (isAboutDialogVisible) {
        InfoDialog(
            title = "About Kitab",
            message = ABOUT_TEXT,
            onDismiss = { isAboutDialogVisible = false },
        )
    }

    if (areLicensesDialogVisible) {
        InfoDialog(
            title = "Open-source licenses",
            message = LICENSES_TEXT,
            onDismiss = { areLicensesDialogVisible = false },
        )
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_kitab_mark),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = "Kitab",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A labelled group of settings inside a rounded, tone-filled container. */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    icon: ImageVector? = null,
    supporting: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val disabledAlpha = 0.38f
    val headlineColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
        }
    val supportingColor =
        if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = disabledAlpha)
        }
    ListItem(
        headlineContent = { Text(text = title, color = headlineColor) },
        supportingContent =
            if (supporting != null) {
                { Text(text = supporting, color = supportingColor) }
            } else {
                null
            },
        leadingContent =
            if (icon != null) {
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = supportingColor,
                    )
                }
            } else {
                null
            },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
            if (onClick != null) {
                Modifier.clickable(enabled = enabled, onClick = onClick)
            } else {
                Modifier
            },
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    supporting: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsItem(
        title = title,
        supporting = supporting,
        icon = icon,
        enabled = enabled,
        onClick =
            if (enabled) {
                { onCheckedChange(!checked) }
            } else {
                null
            },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
    )
}

@Composable
private fun ThemeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = mode == selected,
                                    role = Role.RadioButton,
                                    onClick = { onSelect(mode) },
                                ).padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == selected,
                            onClick = null,
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Done") }
        },
    )
}

@Composable
private fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "OK") }
        },
        title = { Text(text = title) },
        text = { Text(text = message) },
    )
}

private val ThemeMode.label: String
    get() =
        when (this) {
            ThemeMode.System -> "System default"
            ThemeMode.Light -> "Light"
            ThemeMode.Dark -> "Dark"
        }

private const val ABOUT_TEXT =
    "Kitab is an open-source book reader built with Jetpack Compose and MuPDF."

private const val LICENSES_TEXT =
    "PDF rendering is powered by MuPDF, Copyright (c) 2006-2026 Artifex Software, Inc., " +
        "licensed under the GNU Affero General Public License v3. " +
        "In accordance with the AGPL, the complete source code of this app is open source."
