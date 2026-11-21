# Core data

This package owns the app-wide book/settings data types and the repository contracts and local implementations that expose them. Feature ViewModels depend on repository interfaces; Android persistence details stay in the implementations.

- [`model`](model/README.md) contains `Book`, `BookFilter`, and appearance settings types.
- [`repository`](repository/README.md) exposes observable state and operations for books and settings.

The current implementation persists book metadata and settings with `SharedPreferences` and copies PDF files into app/library storage. This is local persistence, not a database-backed or cloud-synced data layer.
