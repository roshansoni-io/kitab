package io.github.roshansoni_io.kitab.navigation

import android.net.Uri
import androidx.annotation.DrawableRes
import io.github.roshansoni_io.kitab.R

internal enum class KitabTab(
    val route: String,
    val label: String,
    @DrawableRes val iconResource: Int,
) {
    Recent("recent", "Recent", R.drawable.ic_clock),
    Library("library", "Library", R.drawable.ic_home),
    Settings("settings", "Settings", R.drawable.ic_settings),
}

internal object KitabDestinations {
    const val READER_ROUTE = "reader/{bookUri}"
    const val BOOK_URI_ARGUMENT = "bookUri"

    fun reader(bookUri: String): String = "reader/${Uri.encode(bookUri)}"

    fun tabForRoute(route: String?): KitabTab? = KitabTab.entries.firstOrNull { it.route == route }
}
