package io.github.roshansoni_io.kitab.core.data.model

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class SelectionColor { Blue, Amber, Green, Rose }

enum class ReaderPageTone { Theme, Paper, Warm, Dark }

enum class ReaderPaging { Horizontal, Vertical }

enum class ResumeBehavior { Resume, Beginning }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val customSeedColor: Int = 0xFF6750A4.toInt(),
    val keepScreenAwake: Boolean = false,
    val selectionColor: SelectionColor = SelectionColor.Blue,
    val readerPageTone: ReaderPageTone = ReaderPageTone.Theme,
    val readerPaging: ReaderPaging = ReaderPaging.Horizontal,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.Resume,
    val readerControlsInitiallyVisible: Boolean = true,
    val libraryListView: Boolean = false,
)
