package io.github.roshansoni_io.kitab.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val uri: String,
    val cachedPath: String,
    val title: String,
    val lastPage: Int = 0,
    val pageCount: Int = 0,
    val lastOpenedAt: Long = 0,
    val inLibrary: Boolean = false,
    val starred: Boolean = false,
)

enum class BookFilter {
    All,
    Recent,
    Starred,
}
