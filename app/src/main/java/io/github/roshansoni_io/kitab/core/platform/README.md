# Android platform adapters

This package isolates Android operations that are called by the app shell:

- `StorageAccess` and `AndroidStorageAccess` check the platform-specific access state.
- `launchLibraryAccessSettings` opens the relevant system settings screen.
- `openExternalLink` hands a URI to another installed app and reports when no handler is available.

Keep activity-result launchers and user-facing flow decisions in the UI/navigation layer. These helpers perform platform actions; they do not own screen state.
