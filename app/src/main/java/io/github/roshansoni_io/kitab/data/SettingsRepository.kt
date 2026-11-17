package io.github.roshansoni_io.kitab.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setThemeMode(themeMode: ThemeMode) {
        update { it.copy(themeMode = themeMode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        update { it.copy(dynamicColor = enabled) }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        prefs
            .edit()
            .putString(KEY_THEME_MODE, updated.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, updated.dynamicColor)
            .apply()
        _settings.value = updated
    }

    private fun readSettings(): AppSettings =
        AppSettings(
            themeMode = storedThemeMode(),
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        )

    private fun storedThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.System
        return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.System
    }

    private companion object {
        const val PREFS_NAME = "kitab_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}
