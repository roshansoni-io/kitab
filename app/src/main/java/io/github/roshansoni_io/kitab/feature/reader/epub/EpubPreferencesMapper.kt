package io.github.roshansoni_io.kitab.feature.reader.epub

import androidx.compose.ui.graphics.Color
import io.github.roshansoni_io.kitab.core.data.model.AppSettings
import io.github.roshansoni_io.kitab.core.data.model.ReaderPageTone
import io.github.roshansoni_io.kitab.core.data.model.ReaderPaging
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.Spread
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalReadiumApi::class)
internal fun AppSettings.toEpubPreferences(isDarkTheme: Boolean): EpubPreferences =
    EpubPreferences(
        columnCount = ColumnCount.ONE,
        fontSize = readerTextSize.scale.toDouble(),
        pageMargins = 0.5,
        publisherStyles = true,
        scroll = readerPaging == ReaderPaging.Vertical,
        spread = Spread.NEVER,
        theme =
            when (readerPageTone) {
                ReaderPageTone.Theme -> if (isDarkTheme) Theme.DARK else Theme.LIGHT
                ReaderPageTone.Paper -> Theme.LIGHT
                ReaderPageTone.Warm -> Theme.SEPIA
                ReaderPageTone.Dark -> Theme.DARK
            },
    )

internal fun ReaderPageTone.epubBackgroundColor(isDarkTheme: Boolean): Color =
    when (this) {
        ReaderPageTone.Theme -> if (isDarkTheme) Color(0xFF000000) else Color(0xFFFFFFFF)
        ReaderPageTone.Paper -> Color(0xFFFFFFFF)
        ReaderPageTone.Warm -> Color(0xFFFAF4E8)
        ReaderPageTone.Dark -> Color(0xFF000000)
    }
