# Reader feature

Reader is a navigation destination opened with a book URI; it is not a bottom-bar tab. `ReaderRoute` resolves the book from `BookRepository`, then `ReaderScreen` presents pages, controls, zoom, links, and text selection. Page changes are reported to the app shell for persistence.

Reader currently has no feature ViewModel. Transient page, zoom, link-confirmation, and selection interactions are held by the reader composables/state holders; durable book progress remains in `BookRepository`.

- [`document`](document/README.md) opens and renders PDFs through MuPDF.
- [`render`](render/README.md) maps page geometry and render regions.
- [`selection`](selection/README.md) models extracted text and selection state.
