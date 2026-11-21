package io.github.roshansoni_io.kitab.core.data.repository

import android.app.Application
import io.github.roshansoni_io.kitab.core.data.model.ThemeMode
import io.github.roshansoni_io.kitab.core.data.model.AppPalette
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
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
        assertTrue(settings.dynamicColor)
    }

    @Test
    fun updates_are_persisted_and_survive_reload() {
        val repository = SharedPreferencesSettingsRepository(application)

        repository.setThemeMode(ThemeMode.Dark)
        repository.setDynamicColor(false)
        repository.setAppPalette(AppPalette.Ocean)
        repository.setKeepScreenAwake(true)
        repository.setSelectionColor(SelectionColor.Rose)
        repository.setReaderPageTone(ReaderPageTone.Warm)
        repository.setMaximumZoom(8f)
        repository.setReaderPaging(ReaderPaging.Vertical)
        repository.setResumeBehavior(ResumeBehavior.Beginning)
        repository.setReaderControlsInitiallyVisible(false)
        repository.setLibraryListView(true)

        assertEquals(ThemeMode.Dark, repository.settings.value.themeMode)
        assertFalse(repository.settings.value.dynamicColor)

        val reloaded = SharedPreferencesSettingsRepository(application).settings.value
        assertEquals(ThemeMode.Dark, reloaded.themeMode)
        assertFalse(reloaded.dynamicColor)
        assertEquals(AppPalette.Ocean, reloaded.appPalette)
        assertTrue(reloaded.keepScreenAwake)
        assertEquals(SelectionColor.Rose, reloaded.selectionColor)
        assertEquals(ReaderPageTone.Warm, reloaded.readerPageTone)
        assertEquals(8f, reloaded.maximumZoom)
        assertEquals(ReaderPaging.Vertical, reloaded.readerPaging)
        assertEquals(ResumeBehavior.Beginning, reloaded.resumeBehavior)
        assertFalse(reloaded.readerControlsInitiallyVisible)
        assertTrue(reloaded.libraryListView)
    }
}
