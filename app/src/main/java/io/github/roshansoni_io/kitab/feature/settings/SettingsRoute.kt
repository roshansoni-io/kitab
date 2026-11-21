package io.github.roshansoni_io.kitab.feature.settings

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.roshansoni_io.kitab.core.di.AppContainer

@Composable
internal fun SettingsRoute(container: AppContainer) {
    val settingsViewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModel.factory(container.settingsRepository))
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { SettingsTopBar() },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        SettingsScreen(
            uiState = uiState,
            onEvent = settingsViewModel::onEvent,
            contentPadding = contentPadding,
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
