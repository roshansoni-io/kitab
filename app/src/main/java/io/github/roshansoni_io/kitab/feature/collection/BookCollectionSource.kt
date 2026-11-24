package io.github.roshansoni_io.kitab.feature.collection

internal enum class BookCollectionSource(
    val savedStateKeyPrefix: String,
    val supportsListView: Boolean,
) {
    Recent(
        savedStateKeyPrefix = "recent",
        supportsListView = true,
    ),
    Library(
        savedStateKeyPrefix = "library",
        supportsListView = true,
    ),
}
