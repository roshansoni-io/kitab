# Android platform adapters

This package isolates Android-specific adapters and operations:

- `BookCoverRenderer` creates and loads size-limited first-page thumbnails with Android `PdfRenderer`; rendering and file access run from the repository's IO dispatcher.
- `openExternalLink` hands a URI to another installed app and reports when no handler is available.

Keep activity-result launchers and user-facing flow decisions in the UI/navigation layer. These helpers perform platform actions; they do not own screen state.
