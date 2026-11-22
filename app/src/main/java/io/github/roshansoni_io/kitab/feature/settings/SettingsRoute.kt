package io.github.roshansoni_io.kitab.feature.settings

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.roshansoni_io.kitab.core.di.AppContainer

@Composable
internal fun SettingsRoute(container: AppContainer) {
    val settingsViewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.factory(container.settingsRepository))
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var isAboutDialogVisible by rememberSaveable { mutableStateOf(false) }
    var areLicensesDialogVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { SettingsTopBar() },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        SettingsContent(
            uiState = uiState,
            onEvent = settingsViewModel::onEvent,
            contentPadding = contentPadding,
            isAboutDialogVisible = isAboutDialogVisible,
            onAboutClick = { isAboutDialogVisible = true },
            onAboutDismiss = { isAboutDialogVisible = false },
            areLicensesDialogVisible = areLicensesDialogVisible,
            onLicensesClick = { areLicensesDialogVisible = true },
            onLicensesDismiss = { areLicensesDialogVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar() {
    CenterAlignedTopAppBar(
        title = { Text(text = "Settings") },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}
