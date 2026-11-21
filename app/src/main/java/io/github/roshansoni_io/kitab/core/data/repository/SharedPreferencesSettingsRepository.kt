package io.github.roshansoni_io.kitab.core.data.repository

import android.content.Context
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.core.data.model.AppPalette
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedPreferencesSettingsRepository(
    context: Context,
) : SettingsRepository {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override fun setThemeMode(themeMode: ThemeMode) {
        update { it.copy(themeMode = themeMode) }
    }

    override fun setDynamicColor(enabled: Boolean) {
        update { it.copy(dynamicColor = enabled) }
    }

    override fun setAppPalette(palette: AppPalette) = update { it.copy(appPalette = palette, dynamicColor = false) }
    override fun setKeepScreenAwake(enabled: Boolean) = update { it.copy(keepScreenAwake = enabled) }
    override fun setSelectionColor(color: SelectionColor) = update { it.copy(selectionColor = color) }
    override fun setReaderPageTone(tone: ReaderPageTone) = update { it.copy(readerPageTone = tone) }
    override fun setMaximumZoom(scale: Float) = update { it.copy(maximumZoom = scale.coerceIn(MIN_ZOOM, MAX_ZOOM)) }
    override fun setReaderPaging(paging: ReaderPaging) = update { it.copy(readerPaging = paging) }
    override fun setResumeBehavior(behavior: ResumeBehavior) = update { it.copy(resumeBehavior = behavior) }
    override fun setReaderControlsInitiallyVisible(visible: Boolean) = update { it.copy(readerControlsInitiallyVisible = visible) }
    override fun setLibraryListView(listView: Boolean) = update { it.copy(libraryListView = listView) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        prefs
            .edit()
            .putString(KEY_THEME_MODE, updated.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, updated.dynamicColor)
            .putString(KEY_APP_PALETTE, updated.appPalette.name)
            .putBoolean(KEY_KEEP_AWAKE, updated.keepScreenAwake)
            .putString(KEY_SELECTION_COLOR, updated.selectionColor.name)
            .putString(KEY_PAGE_TONE, updated.readerPageTone.name)
            .putFloat(KEY_MAX_ZOOM, updated.maximumZoom)
            .putString(KEY_PAGING, updated.readerPaging.name)
            .putString(KEY_RESUME, updated.resumeBehavior.name)
            .putBoolean(KEY_CONTROLS_VISIBLE, updated.readerControlsInitiallyVisible)
            .putBoolean(KEY_LIBRARY_LIST, updated.libraryListView)
            .apply()
        _settings.value = updated
    }

    private fun readSettings(): AppSettings =
        AppSettings(
            themeMode = storedThemeMode(),
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            appPalette = enumPreference(KEY_APP_PALETTE, AppPalette.Amber),
            keepScreenAwake = prefs.getBoolean(KEY_KEEP_AWAKE, false),
            selectionColor = enumPreference(KEY_SELECTION_COLOR, SelectionColor.Blue),
            readerPageTone = enumPreference(KEY_PAGE_TONE, ReaderPageTone.Theme),
            maximumZoom = prefs.getFloat(KEY_MAX_ZOOM, 5f).coerceIn(MIN_ZOOM, MAX_ZOOM),
            readerPaging = enumPreference(KEY_PAGING, ReaderPaging.Horizontal),
            resumeBehavior = enumPreference(KEY_RESUME, ResumeBehavior.Resume),
            readerControlsInitiallyVisible = prefs.getBoolean(KEY_CONTROLS_VISIBLE, true),
            libraryListView = prefs.getBoolean(KEY_LIBRARY_LIST, false),
        )

    private inline fun <reified T : Enum<T>> enumPreference(key: String, default: T): T =
        prefs.getString(key, null)?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default

    private fun storedThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.System
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.System
    }

    private companion object {
        const val PREFS_NAME = "kitab_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_APP_PALETTE = "app_palette"
        const val KEY_KEEP_AWAKE = "keep_screen_awake"
        const val KEY_SELECTION_COLOR = "selection_color"
        const val KEY_PAGE_TONE = "reader_page_tone"
        const val KEY_MAX_ZOOM = "maximum_zoom"
        const val KEY_PAGING = "reader_paging"
        const val KEY_RESUME = "resume_behavior"
        const val KEY_CONTROLS_VISIBLE = "reader_controls_visible"
        const val KEY_LIBRARY_LIST = "library_list_view"
        const val MIN_ZOOM = 2f
        const val MAX_ZOOM = 10f
    }
}
