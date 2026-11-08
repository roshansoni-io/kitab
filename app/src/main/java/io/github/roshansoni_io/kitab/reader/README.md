# Reader package

The reader is organized into a small document layer and a UI layer. The document layer opens PDFs and provides page data; the UI layer displays pages and handles navigation, zoom, links, and selection.

## Document layer

| File | Purpose |
| --- | --- |
| `MuPdfDocument.kt` | Low-level MuPDF wrapper and document-open errors. |
| `ReaderBook.kt` | Reader-facing book API, page rendering, and page data access. |
| `PageText.kt` | Extracted page characters and text layout data. |
| `LinkTarget.kt` | Link destination data returned for a page. |

## UI layer (`ui/`)

| File | Purpose |
| --- | --- |
| `ReaderScreen.kt` | Public screen entry point, opens/closes the book, and coordinates pages. |
| `ReaderPage.kt` | Renders one page; handles zoom, sharper zoom rendering, links, and text selection gestures. |
| `ReaderChrome.kt` | Shared screen scaffold, title/back bar, and page slider. |
| `ZoomState.kt` | Zoom/pan state and pointer gesture handling. |
| `PageGeometry.kt` | Page fitting and visible-region calculations for rendering. |
| `PageMapper.kt` | Maps between screen coordinates and PDF page coordinates; hit-tests links. |
| `TextSelection.kt` | Finds words and builds selections from extracted page text. |
| `SelectionOverlay.kt` | Draws selection highlights and the copy/close toolbar. |
| `LinkConfirmChip.kt` | Small confirmation UI shown before opening an external link. |

Keep related page behavior together in `ReaderPage.kt`; split it further only if a distinct responsibility grows enough to be easier to maintain on its own.
