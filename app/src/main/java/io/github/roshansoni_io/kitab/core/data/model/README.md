# Data models

These types describe values shared across app features:

- `Book` stores a PDF's source URI, cached file path, title, reading progress, and library membership.
- `AppSettings` and `ThemeMode` represent the selected appearance and dynamic-color preference.

Keep these models independent of screen-specific presentation state. Add a field here only when it is part of the shared book or settings data contract.
