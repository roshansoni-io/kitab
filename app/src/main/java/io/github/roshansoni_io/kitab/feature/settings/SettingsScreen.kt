package io.github.roshansoni_io.kitab.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/** Owns transient screen interactions and delegates rendering to [SettingsContent]. */
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    var isAboutDialogVisible by rememberSaveable { mutableStateOf(false) }
    var areLicensesDialogVisible by rememberSaveable { mutableStateOf(false) }

    SettingsContent(
        uiState = uiState,
        onEvent = onEvent,
        contentPadding = contentPadding,
        isAboutDialogVisible = isAboutDialogVisible,
        onAboutClick = { isAboutDialogVisible = true },
        onAboutDismiss = { isAboutDialogVisible = false },
        areLicensesDialogVisible = areLicensesDialogVisible,
        onLicensesClick = { areLicensesDialogVisible = true },
        onLicensesDismiss = { areLicensesDialogVisible = false },
    )
}
