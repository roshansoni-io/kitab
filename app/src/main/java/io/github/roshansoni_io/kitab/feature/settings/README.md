# Settings feature

The Settings destination lets users configure app appearance, reading behavior, selection color, maximum zoom, and persistent library layout, alongside app information. `SettingsViewModel` maps `SettingsRepository.settings` into UI state and writes changes back through repository methods.

`MainActivity` also observes the settings flow to apply the selected theme app-wide. Keep persistence out of the screen and avoid duplicating the settings source of truth locally.
