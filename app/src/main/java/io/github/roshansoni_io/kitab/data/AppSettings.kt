package io.github.roshansoni_io.kitab.data

enum class ThemeMode {
    System,
    Light,
    Dark,
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
)
