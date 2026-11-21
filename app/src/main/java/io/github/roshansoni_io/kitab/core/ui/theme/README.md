# App theme

`KitabTheme` applies the app's Material 3 Expressive theme and typography. Theme mode, dynamic color, and the named app palette come from `SettingsRepository`; `MainActivity` observes them and supplies them to the theme.

`isDarkTheme` resolves the configured theme mode for system-bar appearance. Keep theme definitions here and keep persisted preference updates in the settings feature/repository.
