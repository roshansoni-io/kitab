package io.github.roshansoni_io.kitab.core.data.repository

import android.app.Application
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import io.github.roshansoni_io.kitab.core.data.model.ReaderTextSize
import io.github.roshansoni_io.kitab.core.data.model.ResumeBehavior
import io.github.roshansoni_io.kitab.core.data.model.SelectionColor
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
class SharedPreferencesSettingsRepositoryTest {
    private val application = RuntimeEnvironment.getApplication() as Application

    @Test
    fun defaults_to_system_theme_and_dynamic_color() {
        val settings = SharedPreferencesSettingsRepository(application).settings.value

        assertEquals(ThemeMode.System, settings.themeMode)
        assertEquals(ThemeMode.System, settings.readerTheme)
        assertTrue(settings.dynamicColor)
        assertEquals(0xFF6750A4.toInt(), settings.customSeedColor)
        assertEquals(ReaderTextSize.Medium, settings.readerTextSize)
    }

    @Test
    fun updates_are_persisted_and_survive_reload() {
        val repository = SharedPreferencesSettingsRepository(application)

        repository.setThemeMode(ThemeMode.Dark)
        repository.setDynamicColor(false)
        repository.setCustomSeedColor(0xFF008577.toInt())
        repository.setKeepScreenAwake(true)
        repository.setSelectionColor(SelectionColor.Rose)
        repository.setReaderPageTone(ReaderPageTone.Warm)
        repository.setReaderTheme(ThemeMode.Light)
        repository.setReaderPaging(ReaderPaging.Vertical)
        repository.setReaderTextSize(ReaderTextSize.Large)
        repository.setResumeBehavior(ResumeBehavior.Beginning)
        repository.setReaderControlsInitiallyVisible(false)
        repository.setLibraryListView(true)

        assertEquals(ThemeMode.Dark, repository.settings.value.themeMode)
        assertFalse(repository.settings.value.dynamicColor)

        val reloaded = SharedPreferencesSettingsRepository(application).settings.value
        assertEquals(ThemeMode.Dark, reloaded.themeMode)
        assertFalse(reloaded.dynamicColor)
        assertEquals(0xFF008577.toInt(), reloaded.customSeedColor)
        assertTrue(reloaded.keepScreenAwake)
        assertEquals(SelectionColor.Rose, reloaded.selectionColor)
        assertEquals(ReaderPageTone.Warm, reloaded.readerPageTone)
        assertEquals(ThemeMode.Light, reloaded.readerTheme)
        assertEquals(ReaderPaging.Vertical, reloaded.readerPaging)
        assertEquals(ReaderTextSize.Large, reloaded.readerTextSize)
        assertEquals(ResumeBehavior.Beginning, reloaded.resumeBehavior)
        assertFalse(reloaded.readerControlsInitiallyVisible)
        assertTrue(reloaded.libraryListView)
    }
}
