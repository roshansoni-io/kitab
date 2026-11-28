package io.github.roshansoni_io.kitab.feature.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.roshansoni_io.kitab.BuildConfig
import io.github.roshansoni_io.kitab.R
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.core.data.model.ReaderTextSize
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.core.platform.openExternalLink

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    val appearanceBadgeShape = MaterialShapes.Cookie4Sided.toShape()
    val appearanceContainer = MaterialTheme.colorScheme.primaryContainer
    val appearanceContent = MaterialTheme.colorScheme.onPrimaryContainer

    val readingBadgeShape = MaterialShapes.Cookie6Sided.toShape()
    val readingContainer = MaterialTheme.colorScheme.secondaryContainer
    val readingContent = MaterialTheme.colorScheme.onSecondaryContainer

    val supportBadgeShape = MaterialShapes.Sunny.toShape()
    val supportContainer = MaterialTheme.colorScheme.tertiaryContainer
    val supportContent = MaterialTheme.colorScheme.onTertiaryContainer

    val aboutBadgeShape = MaterialShapes.Cookie12Sided.toShape()
    val aboutContainer = MaterialTheme.colorScheme.surfaceContainerHighest
    val aboutContent = MaterialTheme.colorScheme.onSurface

    val itemColors =
        ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )

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
            SettingsGroup(title = "Appearance") {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            ListItem(
                                supportingContent = { Text("Choose light, dark, or follow the system") },
                                leadingContent = {
                                    SettingsBadge(
                                        shape = appearanceBadgeShape,
                                        containerColor = appearanceContainer,
                                        contentColor = appearanceContent,
                                        iconRes = R.drawable.ic_palette,
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            ) {
                                Text("Theme")
                            }
                            EnumSelector(
                                title = null,
                                options = ThemeMode.entries,
                                selected = uiState.themeMode,
                                label = { it.label },
                                onSelect = { onEvent(SettingsEvent.SetThemeMode(it)) },
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                        }
                    }

                    SegmentedListItem(
                        checked = uiState.dynamicColor && supportsDynamicColor,
                        onCheckedChange = { onEvent(SettingsEvent.SetDynamicColor(it)) },
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                        colors = itemColors,
                        enabled = supportsDynamicColor,
                        leadingContent = {
                            SettingsBadge(
                                shape = appearanceBadgeShape,
                                containerColor = appearanceContainer,
                                contentColor = appearanceContent,
                                iconRes = R.drawable.ic_auto_awesome,
                            )
                        },
                        supportingContent = {
                            Text(
                                if (supportsDynamicColor) {
                                    "Use colors from your wallpaper"
                                } else {
                                    "Requires Android 12 or later"
                                },
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.dynamicColor && supportsDynamicColor,
                                onCheckedChange = null,
                                enabled = supportsDynamicColor,
                            )
                        },
                    ) {
                        Text("Dynamic color")
                    }

                    SegmentedListItem(
                        onClick = { isColorPickerVisible = true },
                        shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                        colors = itemColors,
                        leadingContent = {
                            SettingsBadge(
                                shape = appearanceBadgeShape,
                                containerColor = appearanceContainer,
                                contentColor = appearanceContent,
                                iconRes = R.drawable.ic_color_lens,
                            )
                        },
                        supportingContent = {
                            Text(
                                if (uiState.dynamicColor && supportsDynamicColor) {
                                    "Using wallpaper colors"
                                } else {
                                    "Choose a seed color for the Material 3 palette"
                                },
                            )
                        },
                        trailingContent = {
                            Surface(
                                color = Color(uiState.customSeedColor),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp),
                            ) {}
                        },
                    ) {
                        Text("Custom color")
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Reading") {
                SettingsSwitchRow(
                    title = "Keep screen awake",
                    supporting = "Prevent the screen from turning off while reading",
                    iconRes = R.drawable.ic_visibility,
                    badgeShape = readingBadgeShape,
                    badgeContainerColor = readingContainer,
                    badgeContentColor = readingContent,
                    checked = uiState.keepScreenAwake,
                    onCheckedChange = { onEvent(SettingsEvent.SetKeepAwake(it)) },
                )
                EnumSelector("Page direction", ReaderPaging.entries, uiState.readerPaging, { it.label }) {
                    onEvent(SettingsEvent.SetPaging(it))
                }
                EnumSelector("Text size", ReaderTextSize.entries, uiState.readerTextSize, { it.label }) {
                    onEvent(SettingsEvent.SetReaderTextSize(it))
                }
                EnumSelector("When opening a book", ResumeBehavior.entries, uiState.resumeBehavior, { it.label }) {
                    onEvent(SettingsEvent.SetResumeBehavior(it))
                }
                SettingsSwitchRow(
                    title = "Show reader controls",
                    supporting = "Show the page controls when opening a book",
                    iconRes = R.drawable.ic_tune,
                    badgeShape = readingBadgeShape,
                    badgeContainerColor = readingContainer,
                    badgeContentColor = readingContent,
                    checked = uiState.readerControlsInitiallyVisible,
                    onCheckedChange = { onEvent(SettingsEvent.SetControlsInitiallyVisible(it)) },
                )
                EnumSelector("Reader theme", ThemeMode.entries, uiState.readerTheme, { it.label }) {
                    onEvent(SettingsEvent.SetReaderTheme(it))
                }
                EnumSelector("Page tone", ReaderPageTone.entries, uiState.readerPageTone, { it.label }) {
                    onEvent(SettingsEvent.SetPageTone(it))
                }
                EnumSelector(
                    title = "Selection color",
                    options = SelectionColor.entries,
                    selected = uiState.selectionColor,
                    label = { it.label },
                    modifier = Modifier.padding(bottom = 12.dp),
                ) {
                    onEvent(SettingsEvent.SetSelectionColor(it))
                }
            }
        }

        item {
            SettingsGroup(title = "Support") {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    SegmentedListItem(
                        onClick = { openExternalLink(context, REPOSITORY_ISSUES_URL) },
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                        colors = itemColors,
                        leadingContent = {
                            SettingsBadge(
                                shape = supportBadgeShape,
                                containerColor = supportContainer,
                                contentColor = supportContent,
                                imageVector = Icons.Filled.Info,
                            )
                        },
                        supportingContent = { Text("Share feedback or tell us what could be better") },
                    ) {
                        Text("Report a bug or request a feature")
                    }
                    SegmentedListItem(
                        onClick = { openExternalLink(context, REPOSITORY_URL) },
                        shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                        colors = itemColors,
                        leadingContent = {
                            SettingsBadge(
                                shape = supportBadgeShape,
                                containerColor = supportContainer,
                                contentColor = supportContent,
                                iconRes = R.drawable.ic_star,
                            )
                        },
                        supportingContent = { Text("Support the project by starring the repository") },
                    ) {
                        Text("Star Kitab on GitHub")
                    }
                }
            }
        }

        item {
            SettingsGroup(title = "About") {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    SegmentedListItem(
                        onClick = onAboutClick,
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                        colors = itemColors,
                        leadingContent = {
                            SettingsBadge(
                                shape = aboutBadgeShape,
                                containerColor = aboutContainer,
                                contentColor = aboutContent,
                                iconRes = R.drawable.ic_kitab_logo,
                            )
                        },
                        supportingContent = { Text("PDF and EPUB reader · Version ${BuildConfig.VERSION_NAME}") },
                    ) {
                        Text("About Kitab")
                    }
                    SegmentedListItem(
                        onClick = onLicensesClick,
                        shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                        colors = itemColors,
                        leadingContent = {
                            SettingsBadge(
                                shape = aboutBadgeShape,
                                containerColor = aboutContainer,
                                contentColor = aboutContent,
                                iconRes = R.drawable.ic_description,
                            )
                        },
                        supportingContent = { Text("MuPDF, Readium") },
                    ) {
                        Text("Open-source licenses")
                    }
                }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> EnumSelector(
    title: String?,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = if (title != null) 12.dp else 0.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { index, option ->
                val buttonContent: @Composable RowScope.() -> Unit = {
                    Text(
                        text = label(option),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val buttonModifier = Modifier.weight(1f)
                val buttonPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                if (options.size == 1) {
                    ToggleButton(
                        checked = selected == option,
                        onCheckedChange = { checked -> if (checked) onSelect(option) },
                        contentPadding = buttonPadding,
                        modifier = buttonModifier,
                        content = buttonContent,
                    )
                } else {
                    ToggleButton(
                        checked = selected == option,
                        onCheckedChange = { checked -> if (checked) onSelect(option) },
                        shapes =
                            when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        contentPadding = buttonPadding,
                        modifier = buttonModifier,
                        content = buttonContent,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            shape = MaterialShapes.Cookie12Sided.toShape(),
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
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsGroup(title = title) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun SettingsBadge(
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    @DrawableRes iconRes: Int? = null,
    imageVector: ImageVector? = null,
) {
    Surface(
        shape = shape,
        color = containerColor,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            } else if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    supporting: String,
    @DrawableRes iconRes: Int,
    badgeShape: Shape,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        supportingContent = { Text(supporting) },
        leadingContent = {
            SettingsBadge(
                shape = badgeShape,
                containerColor = badgeContainerColor,
                contentColor = badgeContentColor,
                iconRes = iconRes,
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
            Modifier.toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        Text(title)
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
private val ReaderTextSize.label: String get() =
    when (this) {
        ReaderTextSize.Small -> "Small"
        ReaderTextSize.Medium -> "Medium"
        ReaderTextSize.Large -> "Large"
        ReaderTextSize.ExtraLarge -> "Extra large"
    }
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
    "Kitab is an open-source PDF and EPUB reader for Android. Import books into Kitab's private app " +
        "storage, browse your library and recent books, and read with adjustable text size, page " +
        "direction, page tone, text selection, and pinch-to-zoom. Appearance supports Android " +
        "wallpaper colors and custom Material 3 seed colors.\n\n" +
        "Version ${BuildConfig.VERSION_NAME}\n" +
        "Licensed under the GNU Affero General Public License, version 3.\n\n" +
        REPOSITORY_URL

private const val LICENSES_TEXT =
    "PDF rendering is powered by MuPDF, Copyright (c) 2006-2026 Artifex Software, Inc., " +
        "licensed under the GNU Affero General Public License v3. " +
        "EPUB rendering is powered by the Readium Kotlin Toolkit, Copyright (c) 2020-2026 " +
        "Readium Foundation, licensed under the BSD 3-Clause License. " +
        "In accordance with the AGPL, the complete source code of this app is open source."

private const val REPOSITORY_URL = "https://github.com/roshansoni-io/kitab"
private const val REPOSITORY_ISSUES_URL = "$REPOSITORY_URL/issues"
