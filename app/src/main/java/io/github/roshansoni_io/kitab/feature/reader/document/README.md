# Reader documents

This package adapts MuPDF documents for Reader. `ReaderBook` owns the open document lifecycle and exposes page count, page size, page/region rendering, links, and extracted text. `MuPdfDocument` contains the native document operations; `DocumentOpenException` represents expected open failures.

`PageBitmapCache` caches rendered pages. Close each `ReaderBook` when its reader session ends, and keep MuPDF-specific types inside this package.
