package io.github.roshansoni_io.kitab.core.data.repository

import android.content.Context
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.core.data.model.ReaderTextSize
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
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

    override fun setCustomSeedColor(color: Int) = update { it.copy(customSeedColor = color, dynamicColor = false) }

    override fun setKeepScreenAwake(enabled: Boolean) = update { it.copy(keepScreenAwake = enabled) }

    override fun setSelectionColor(color: SelectionColor) = update { it.copy(selectionColor = color) }

    override fun setReaderPageTone(tone: ReaderPageTone) = update { it.copy(readerPageTone = tone) }

    override fun setReaderTheme(themeMode: ThemeMode) = update { it.copy(readerTheme = themeMode) }

    override fun setReaderPaging(paging: ReaderPaging) = update { it.copy(readerPaging = paging) }

    override fun setReaderTextSize(size: ReaderTextSize) = update { it.copy(readerTextSize = size) }

    override fun setResumeBehavior(behavior: ResumeBehavior) = update { it.copy(resumeBehavior = behavior) }

    override fun setReaderControlsInitiallyVisible(visible: Boolean) = update { it.copy(readerControlsInitiallyVisible = visible) }

    override fun setLibraryListView(listView: Boolean) = update { it.copy(libraryListView = listView) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        prefs
            .edit()
            .putString(KEY_THEME_MODE, updated.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, updated.dynamicColor)
            .putInt(KEY_CUSTOM_SEED_COLOR, updated.customSeedColor)
            .putBoolean(KEY_KEEP_AWAKE, updated.keepScreenAwake)
            .putString(KEY_SELECTION_COLOR, updated.selectionColor.name)
            .putString(KEY_PAGE_TONE, updated.readerPageTone.name)
            .putString(KEY_READER_THEME, updated.readerTheme.name)
            .putString(KEY_PAGING, updated.readerPaging.name)
            .putString(KEY_TEXT_SIZE, updated.readerTextSize.name)
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
            customSeedColor = storedCustomSeedColor(),
            keepScreenAwake = prefs.getBoolean(KEY_KEEP_AWAKE, false),
            selectionColor = enumPreference(KEY_SELECTION_COLOR, SelectionColor.Blue),
            readerPageTone = enumPreference(KEY_PAGE_TONE, ReaderPageTone.Theme),
            readerTheme = enumPreference(KEY_READER_THEME, ThemeMode.System),
            readerPaging = enumPreference(KEY_PAGING, ReaderPaging.Horizontal),
            readerTextSize = enumPreference(KEY_TEXT_SIZE, ReaderTextSize.Medium),
            resumeBehavior = enumPreference(KEY_RESUME, ResumeBehavior.Resume),
            readerControlsInitiallyVisible = prefs.getBoolean(KEY_CONTROLS_VISIBLE, true),
            libraryListView = prefs.getBoolean(KEY_LIBRARY_LIST, false),
        )

    private inline fun <reified T : Enum<T>> enumPreference(
        key: String,
        default: T,
    ): T = prefs.getString(key, null)?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default

    private fun storedThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.System
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.System
    }

    private fun storedCustomSeedColor(): Int {
        if (prefs.contains(KEY_CUSTOM_SEED_COLOR)) return prefs.getInt(KEY_CUSTOM_SEED_COLOR, DEFAULT_SEED_COLOR)

        val seedColor =
            when (prefs.getString(KEY_LEGACY_APP_PALETTE, null)) {
                "Amber" -> 0xFF8A5200.toInt()
                "Ocean" -> 0xFF005A9E.toInt()
                "Forest" -> 0xFF286333.toInt()
                "Rose" -> 0xFF9C3F59.toInt()
                else -> DEFAULT_SEED_COLOR
            }

        prefs
            .edit()
            .putInt(KEY_CUSTOM_SEED_COLOR, seedColor)
            .remove(KEY_LEGACY_APP_PALETTE)
            .apply()
        return seedColor
    }

    private companion object {
        const val PREFS_NAME = "kitab_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_CUSTOM_SEED_COLOR = "custom_seed_color"
        const val KEY_LEGACY_APP_PALETTE = "app_palette"
        const val DEFAULT_SEED_COLOR = 0xFF6750A4.toInt()
        const val KEY_KEEP_AWAKE = "keep_screen_awake"
        const val KEY_SELECTION_COLOR = "selection_color"
        const val KEY_PAGE_TONE = "reader_page_tone"
        const val KEY_READER_THEME = "reader_theme"
        const val KEY_PAGING = "reader_paging"
        const val KEY_TEXT_SIZE = "reader_text_size"
        const val KEY_RESUME = "resume_behavior"
        const val KEY_CONTROLS_VISIBLE = "reader_controls_visible"
        const val KEY_LIBRARY_LIST = "library_list_view"
    }
}
