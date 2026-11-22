package io.github.roshansoni_io.kitab.feature.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
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
    val dynamicColor: Boolean = false,
    val supportsDynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    val customSeedColor: Int = 0xFF6750A4.toInt(),
    val keepScreenAwake: Boolean = false,
    val selectionColor: SelectionColor = SelectionColor.Blue,
    val readerPageTone: ReaderPageTone = ReaderPageTone.Theme,
    val readerPaging: ReaderPaging = ReaderPaging.Horizontal,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.Resume,
    val readerControlsInitiallyVisible: Boolean = true,
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

    data class SetPaging(
        val value: ReaderPaging,
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
    private val initial = repository.settings.value

    val uiState: StateFlow<SettingsUiState> =
        repository.settings
            .map { settings ->
                SettingsUiState(
                    themeMode = settings.themeMode,
                    dynamicColor = settings.dynamicColor,
                    supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    customSeedColor = settings.customSeedColor,
                    keepScreenAwake = settings.keepScreenAwake,
                    selectionColor = settings.selectionColor,
                    readerPageTone = settings.readerPageTone,
                    readerPaging = settings.readerPaging,
                    resumeBehavior = settings.resumeBehavior,
                    readerControlsInitiallyVisible = settings.readerControlsInitiallyVisible,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    SettingsUiState(
                        themeMode = initial.themeMode,
                        dynamicColor = initial.dynamicColor,
                        supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        customSeedColor = initial.customSeedColor,
                        keepScreenAwake = initial.keepScreenAwake,
                        selectionColor = initial.selectionColor,
                        readerPageTone = initial.readerPageTone,
                        readerPaging = initial.readerPaging,
                        resumeBehavior = initial.resumeBehavior,
                        readerControlsInitiallyVisible = initial.readerControlsInitiallyVisible,
                    ),
            )

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetThemeMode -> repository.setThemeMode(event.mode)
            is SettingsEvent.SetDynamicColor -> repository.setDynamicColor(event.enabled)
            is SettingsEvent.SetCustomSeedColor -> repository.setCustomSeedColor(event.value)
            is SettingsEvent.SetKeepAwake -> repository.setKeepScreenAwake(event.value)
            is SettingsEvent.SetSelectionColor -> repository.setSelectionColor(event.value)
            is SettingsEvent.SetPageTone -> repository.setReaderPageTone(event.value)
            is SettingsEvent.SetPaging -> repository.setReaderPaging(event.value)
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
