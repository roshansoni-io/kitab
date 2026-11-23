package io.github.roshansoni_io.kitab.feature.reader.epub

import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
internal fun AppSettings.toEpubPreferences(isDarkTheme: Boolean): EpubPreferences =
    EpubPreferences(
        fontSize = readerTextSize.scale.toDouble(),
        publisherStyles = true,
        scroll = readerPaging == ReaderPaging.Vertical,
        theme =
            when (readerPageTone) {
                ReaderPageTone.Theme -> if (isDarkTheme) Theme.DARK else Theme.LIGHT
                ReaderPageTone.Paper -> Theme.LIGHT
                ReaderPageTone.Warm -> Theme.SEPIA
                ReaderPageTone.Dark -> Theme.DARK
            },
    )
