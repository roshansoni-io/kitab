package io.github.roshansoni_io.kitab.feature.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.core.data.model.ReaderTextSize
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val customSeedColor: Int = 0xFF6750A4.toInt(),
    val keepScreenAwake: Boolean = false,
    val selectionColor: SelectionColor = SelectionColor.Blue,
    val readerPageTone: ReaderPageTone = ReaderPageTone.Theme,
    val readerTheme: ThemeMode = ThemeMode.System,
    val readerPaging: ReaderPaging = ReaderPaging.Horizontal,
    val readerTextSize: ReaderTextSize = ReaderTextSize.Medium,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.Resume,
    val readerControlsInitiallyVisible: Boolean = true,
)

private fun AppSettings.toUiState(): SettingsUiState =
    SettingsUiState(
        themeMode = themeMode,
        dynamicColor = dynamicColor,
        supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
        customSeedColor = customSeedColor,
        keepScreenAwake = keepScreenAwake,
        selectionColor = selectionColor,
        readerPageTone = readerPageTone,
        readerTheme = readerTheme,
        readerPaging = readerPaging,
        readerTextSize = readerTextSize,
        resumeBehavior = resumeBehavior,
        readerControlsInitiallyVisible = readerControlsInitiallyVisible,
    )

internal sealed interface SettingsEvent {
    data class SetThemeMode(
        val mode: ThemeMode,
    ) : SettingsEvent

    data class SetDynamicColor(
        val enabled: Boolean,
    ) : SettingsEvent

    data class SetCustomSeedColor(
        val value: Int,
    ) : SettingsEvent

    data class SetKeepAwake(
        val value: Boolean,
    ) : SettingsEvent

    data class SetSelectionColor(
        val value: SelectionColor,
    ) : SettingsEvent

    data class SetPageTone(
        val value: ReaderPageTone,
    ) : SettingsEvent

    data class SetReaderTheme(
        val mode: ThemeMode,
    ) : SettingsEvent

    data class SetPaging(
        val value: ReaderPaging,
    ) : SettingsEvent

    data class SetReaderTextSize(
        val value: ReaderTextSize,
    ) : SettingsEvent

    data class SetResumeBehavior(
        val value: ResumeBehavior,
    ) : SettingsEvent

    data class SetControlsInitiallyVisible(
        val value: Boolean,
    ) : SettingsEvent
}

internal class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        repository.settings
            .map(AppSettings::toUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = repository.settings.value.toUiState(),
            )

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetThemeMode -> repository.setThemeMode(event.mode)
            is SettingsEvent.SetDynamicColor -> repository.setDynamicColor(event.enabled)
            is SettingsEvent.SetCustomSeedColor -> repository.setCustomSeedColor(event.value)
            is SettingsEvent.SetKeepAwake -> repository.setKeepScreenAwake(event.value)
            is SettingsEvent.SetSelectionColor -> repository.setSelectionColor(event.value)
            is SettingsEvent.SetPageTone -> repository.setReaderPageTone(event.value)
            is SettingsEvent.SetReaderTheme -> repository.setReaderTheme(event.mode)
            is SettingsEvent.SetPaging -> repository.setReaderPaging(event.value)
            is SettingsEvent.SetReaderTextSize -> repository.setReaderTextSize(event.value)
            is SettingsEvent.SetResumeBehavior -> repository.setResumeBehavior(event.value)
            is SettingsEvent.SetControlsInitiallyVisible -> repository.setReaderControlsInitiallyVisible(event.value)
        }
    }

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { SettingsViewModel(repository) }
            }
    }
}
