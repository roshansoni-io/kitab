package io.github.roshansoni_io.kitab.feature.settings

import io.github.roshansoni_io.kitab.MainDispatcherRule
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.fakes.FakeSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSettingsRepository(AppSettings(themeMode = ThemeMode.Dark, dynamicColor = false))

    @Test
    fun initialState_reflectsRepository() {
        val viewModel = SettingsViewModel(repository)

        assertEquals(ThemeMode.Dark, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.dynamicColor)
    }

    @Test
    fun themeModeEvent_updatesRepository() {
        val viewModel = SettingsViewModel(repository)

        viewModel.onEvent(SettingsEvent.SetThemeMode(ThemeMode.Light))

        assertEquals(ThemeMode.Light, repository.settings.value.themeMode)
    }

    @Test
    fun dynamicColorEvent_updatesRepository() {
        val viewModel = SettingsViewModel(repository)

        viewModel.onEvent(SettingsEvent.SetDynamicColor(true))

        assertEquals(true, repository.settings.value.dynamicColor)
    }
}
