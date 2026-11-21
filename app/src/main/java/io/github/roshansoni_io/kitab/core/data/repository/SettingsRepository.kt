package io.github.roshansoni_io.kitab.core.data.repository

import io.github.roshansoni_io.kitab.core.data.model.AppPalette
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<AppSettings>

    fun setThemeMode(themeMode: ThemeMode)

    fun setDynamicColor(enabled: Boolean)

    fun setAppPalette(palette: AppPalette)

    fun setKeepScreenAwake(enabled: Boolean)

    fun setSelectionColor(color: SelectionColor)

    fun setReaderPageTone(tone: ReaderPageTone)

    fun setMaximumZoom(scale: Float)

    fun setReaderPaging(paging: ReaderPaging)

    fun setResumeBehavior(behavior: ResumeBehavior)

    fun setReaderControlsInitiallyVisible(visible: Boolean)

    fun setLibraryListView(listView: Boolean)
}
