package io.github.roshansoni_io.kitab.fakes

import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.repository.SettingsRepository
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.core.data.model.ReaderTextSize
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    private val _settings = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun setThemeMode(themeMode: ThemeMode) {
        _settings.update { it.copy(themeMode = themeMode) }
    }

    override fun setDynamicColor(enabled: Boolean) {
        _settings.update { it.copy(dynamicColor = enabled) }
    }

    override fun setCustomSeedColor(color: Int) = _settings.update { it.copy(customSeedColor = color, dynamicColor = false) }

    override fun setKeepScreenAwake(enabled: Boolean) = _settings.update { it.copy(keepScreenAwake = enabled) }
    override fun setSelectionColor(color: SelectionColor) = _settings.update { it.copy(selectionColor = color) }
    override fun setReaderPageTone(tone: ReaderPageTone) = _settings.update { it.copy(readerPageTone = tone) }
    override fun setReaderPaging(paging: ReaderPaging) = _settings.update { it.copy(readerPaging = paging) }
    override fun setReaderTextSize(size: ReaderTextSize) = _settings.update { it.copy(readerTextSize = size) }
    override fun setResumeBehavior(behavior: ResumeBehavior) = _settings.update { it.copy(resumeBehavior = behavior) }
    override fun setReaderControlsInitiallyVisible(visible: Boolean) = _settings.update { it.copy(readerControlsInitiallyVisible = visible) }
    override fun setLibraryListView(listView: Boolean) = _settings.update { it.copy(libraryListView = listView) }
}
