# Reader text selection

This package models extracted page characters and their PDF-space bounds, selected text ranges, and interaction state. `PageText` and selection helpers translate between character positions and word/range selections; `TextSelectionState` tracks selection and handle dragging.

Rendering of the selection belongs in [`ui`](ui/README.md). Keep selection calculations based on the text and geometry supplied by the document layer.
