package io.github.roshansoni_io.kitab.core.di

import android.content.Context
import io.github.roshansoni_io.kitab.core.data.repository.BookRepository
import io.github.roshansoni_io.kitab.core.data.repository.LocalBookRepository
import io.github.roshansoni_io.kitab.core.data.repository.SettingsRepository
import io.github.roshansoni_io.kitab.core.data.repository.SharedPreferencesSettingsRepository
import io.github.roshansoni_io.kitab.core.platform.AndroidStorageAccess
import io.github.roshansoni_io.kitab.core.platform.StorageAccess
import io.github.roshansoni_io.kitab.feature.reader.document.MuPdfCoverRenderer

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val bookRepository: BookRepository = LocalBookRepository(appContext, MuPdfCoverRenderer())
    val settingsRepository: SettingsRepository = SharedPreferencesSettingsRepository(appContext)
    val storageAccess: StorageAccess = AndroidStorageAccess(appContext)
}
