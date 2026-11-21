# Data models

These types describe values shared across app features:

- `Book` stores a PDF's source URI, cached file path, title, reading progress, library membership, and starred state.
- `BookFilter` defines the collection filters used by Recent and Library.
- `AppSettings` and `ThemeMode` represent the selected appearance and dynamic-color preference.

Keep these models independent of screen-specific presentation state. Add a field here only when it is part of the shared book or settings data contract.
