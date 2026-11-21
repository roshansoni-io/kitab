# Core

`core` contains app-wide data models and repository contracts, local persistence, Android platform adapters, dependency wiring, and shared UI. Feature code should use these shared contracts rather than reaching into another feature.

## Packages

- [`data`](data/README.md) defines shared models and repository APIs/adapters.
- [`di`](di/README.md) constructs the application-scoped repositories and platform services.
- [`platform`](platform/README.md) contains Android-specific storage and external-link operations.
- [`ui`](ui/README.md) contains reusable components and app theme code.

`BookRepository.books` and `SettingsRepository.settings` are `StateFlow` sources of truth. `PdfCoverRenderer` keeps PDF rendering behind a repository contract. The composition root currently supplies the MuPDF adapter from `feature/reader/document`; keep that cross-package wiring confined to `di`.
