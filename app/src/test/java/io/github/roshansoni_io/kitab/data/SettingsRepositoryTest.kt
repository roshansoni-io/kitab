package io.github.roshansoni_io.kitab.data

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class SettingsRepositoryTest {
    private val application = RuntimeEnvironment.getApplication() as Application

    @Test
    fun defaults_to_system_theme_and_dynamic_color() {
        val settings = SettingsRepository(application).settings.value

        assertEquals(ThemeMode.System, settings.themeMode)
        assertTrue(settings.dynamicColor)
    }

    @Test
    fun updates_are_persisted_and_survive_reload() {
        val repository = SettingsRepository(application)

        repository.setThemeMode(ThemeMode.Dark)
        repository.setDynamicColor(false)

        assertEquals(ThemeMode.Dark, repository.settings.value.themeMode)
        assertFalse(repository.settings.value.dynamicColor)

        val reloaded = SettingsRepository(application).settings.value
        assertEquals(ThemeMode.Dark, reloaded.themeMode)
        assertFalse(reloaded.dynamicColor)
    }
}
