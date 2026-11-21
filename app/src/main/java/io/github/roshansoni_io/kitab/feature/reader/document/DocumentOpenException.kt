package io.github.roshansoni_io.kitab.feature.reader.document

import java.io.File

sealed class DocumentOpenException(
    message: String,
) : Exception(message) {
    class PasswordRequired : DocumentOpenException("This document is password protected")

    class NotReadable(
        path: String,
    ) : DocumentOpenException("Cannot read document at $path")

    class Failed(
        document: File,
        cause: Throwable,
    ) : DocumentOpenException("Cannot open document ${document.name} (${cause.message})")
}
