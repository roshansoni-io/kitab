package io.github.roshansoni_io.kitab.feature.settings

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.BuildConfig
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.core.platform.openExternalLink

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    contentPadding: PaddingValues,
    isAboutDialogVisible: Boolean,
    onAboutClick: () -> Unit,
    onAboutDismiss: () -> Unit,
    areLicensesDialogVisible: Boolean,
    onLicensesClick: () -> Unit,
    onLicensesDismiss: () -> Unit,
) {
    val supportsDynamicColor = uiState.supportsDynamicColor
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    var isColorPickerVisible by remember { mutableStateOf(false) }

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
                    supporting = "Choose light, dark, or follow the system",
                )
                ThemeSelector(
                    selected = uiState.themeMode,
                    onSelect = { onEvent(SettingsEvent.SetThemeMode(it)) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
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
                    checked = uiState.dynamicColor && supportsDynamicColor,
                    enabled = supportsDynamicColor,
                    onCheckedChange = { onEvent(SettingsEvent.SetDynamicColor(it)) },
                )
                SettingsItem(
                    title = "Custom color",
                    supporting =
                        if (uiState.dynamicColor &&
                            supportsDynamicColor
                        ) {
                            "Using wallpaper colors"
                        } else {
                            "Choose a seed color for the Material 3 palette"
                        },
                    icon = Icons.Filled.Star,
                    interactionModifier = Modifier.clickable(role = Role.Button) { isColorPickerVisible = true },
                    trailing = {
                        Surface(
                            color = Color(uiState.customSeedColor),
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp),
                        ) {}
                    },
                )
            }
        }

        item {
            SettingsSection(title = "Reading") {
                SettingsSwitchItem(
                    title = "Keep screen awake",
                    supporting = "Prevent the screen from turning off while reading",
                    icon = Icons.Filled.Lock,
                    checked = uiState.keepScreenAwake,
                    onCheckedChange = { onEvent(SettingsEvent.SetKeepAwake(it)) },
                )
                EnumSelector("Page direction", ReaderPaging.entries, uiState.readerPaging, { it.label }) {
                    onEvent(SettingsEvent.SetPaging(it))
                }
                EnumSelector("When opening a book", ResumeBehavior.entries, uiState.resumeBehavior, { it.label }) {
                    onEvent(SettingsEvent.SetResumeBehavior(it))
                }
                SettingsSwitchItem(
                    title = "Show reader controls",
                    supporting = "Show the page controls when opening a book",
                    icon = Icons.Filled.Settings,
                    checked = uiState.readerControlsInitiallyVisible,
                    onCheckedChange = { onEvent(SettingsEvent.SetControlsInitiallyVisible(it)) },
                )
                EnumSelector("Page tone", ReaderPageTone.entries, uiState.readerPageTone, { it.label }) {
                    onEvent(SettingsEvent.SetPageTone(it))
                }
                EnumSelector("Selection color", SelectionColor.entries, uiState.selectionColor, { it.label }) {
                    onEvent(SettingsEvent.SetSelectionColor(it))
                }
            }
        }

        item {
            SettingsSection(title = "Support") {
                SettingsItem(
                    title = "Report a bug or request a feature",
                    supporting = "Share feedback or tell us what could be better",
                    icon = Icons.Filled.Info,
                    interactionModifier =
                        Modifier.clickable(role = Role.Button) {
                            openExternalLink(context, REPOSITORY_ISSUES_URL)
                        },
                )
                SettingsItem(
                    title = "Star Kitab on GitHub",
                    supporting = "Support the project by starring the repository",
                    icon = Icons.Filled.Star,
                    interactionModifier =
                        Modifier.clickable(role = Role.Button) {
                            openExternalLink(context, REPOSITORY_URL)
                        },
                )
            }
        }

        item {
            SettingsSection(title = "About") {
                SettingsItem(
                    title = "About Kitab",
                    supporting = "PDF reader · Version ${BuildConfig.VERSION_NAME}",
                    icon = Icons.Filled.Info,
                    interactionModifier =
                        Modifier.clickable(role = Role.Button) {
                            onAboutClick()
                        },
                )
                SettingsItem(
                    title = "Open-source licenses",
                    supporting = "MuPDF, AGPL-3.0",
                    icon = Icons.Filled.Lock,
                    interactionModifier =
                        Modifier.clickable(role = Role.Button) {
                            onLicensesClick()
                        },
                )
            }
        }
    }

    if (isAboutDialogVisible) {
        InfoDialog(
            title = "About Kitab",
            message = ABOUT_TEXT,
            onDismiss = onAboutDismiss,
        )
    }

    if (areLicensesDialogVisible) {
        InfoDialog(
            title = "Open-source licenses",
            message = LICENSES_TEXT,
            onDismiss = onLicensesDismiss,
        )
    }

    if (isColorPickerVisible) {
        ColorPickerDialog(
            seedColor = uiState.customSeedColor,
            onColorSelected = { onEvent(SettingsEvent.SetCustomSeedColor(it)) },
            onDismiss = { isColorPickerVisible = false },
        )
    }
}

@Composable
private fun ColorPickerDialog(
    seedColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialHsv = remember(seedColor) { FloatArray(3).also { android.graphics.Color.colorToHSV(seedColor, it) } }
    val hueState = remember(seedColor) { SliderState(value = initialHsv[0], trackRange = 0f..360f) }
    val saturationState = remember(seedColor) { SliderState(value = initialHsv[1]) }
    val brightnessState = remember(seedColor) { SliderState(value = initialHsv[2]) }
    val selectedColor = android.graphics.Color.HSVToColor(floatArrayOf(hueState.value, saturationState.value, brightnessState.value))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom color") },
        text = {
            Column {
                Surface(
                    color = Color(selectedColor),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().size(72.dp),
                ) {}
                Text("Hue", style = MaterialTheme.typography.labelLarge)
                Slider(state = hueState, onValueChange = { hueState.value = it })
                Text("Saturation", style = MaterialTheme.typography.labelLarge)
                Slider(state = saturationState, onValueChange = { saturationState.value = it })
                Text("Brightness", style = MaterialTheme.typography.labelLarge)
                Slider(state = brightnessState, onValueChange = { brightnessState.value = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(selectedColor)
                    onDismiss()
                },
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun <T> EnumSelector(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label(option), maxLines = 1) },
                )
            }
        }
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
                    painter = painterResource(R.drawable.ic_kitab_logo),
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
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    icon: ImageVector? = null,
    supporting: String? = null,
    enabled: Boolean = true,
    interactionModifier: Modifier = Modifier,
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
        modifier = interactionModifier,
    ) {
        Text(text = title, color = headlineColor)
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    supporting: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsItem(
        title = title,
        supporting = supporting,
        icon = icon,
        enabled = enabled,
        interactionModifier =
            Modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
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
private fun ThemeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                label = { Text(text = mode.label) },
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        activeContentColor = MaterialTheme.colorScheme.onSurface,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
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
            ThemeMode.System -> "System"
            ThemeMode.Light -> "Light"
            ThemeMode.Dark -> "Dark"
        }

private val ReaderPaging.label: String get() = name
private val ResumeBehavior.label: String get() =
    when (this) {
        ResumeBehavior.Resume -> "Resume"
        ResumeBehavior.Beginning -> "Beginning"
    }
private val ReaderPageTone.label: String get() =
    when (this) {
        ReaderPageTone.Theme -> "Theme"
        ReaderPageTone.Paper -> "Paper"
        ReaderPageTone.Warm -> "Warm"
        ReaderPageTone.Dark -> "Dark"
    }
private val SelectionColor.label: String get() = name

private val ABOUT_TEXT =
    "Kitab is an open-source PDF reader for Android. Import PDFs into Kitab's private app storage, " +
        "browse your library and recent books, and read with adjustable page direction, page tone, " +
        "text selection, and pinch-to-zoom. Appearance supports Android wallpaper colors and custom " +
        "Material 3 seed colors.\n\n" +
        "Version ${BuildConfig.VERSION_NAME}\n" +
        "Licensed under the GNU Affero General Public License, version 3.\n\n" +
        REPOSITORY_URL

private const val LICENSES_TEXT =
    "PDF rendering is powered by MuPDF, Copyright (c) 2006-2026 Artifex Software, Inc., " +
        "licensed under the GNU Affero General Public License v3. " +
        "In accordance with the AGPL, the complete source code of this app is open source."

private const val REPOSITORY_URL = "https://github.com/roshansoni-io/kitab"
private const val REPOSITORY_ISSUES_URL = "$REPOSITORY_URL/issues"
