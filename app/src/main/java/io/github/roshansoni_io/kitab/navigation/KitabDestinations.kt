package io.github.roshansoni_io.kitab.navigation

import androidx.annotation.DrawableRes
import io.github.roshansoni_io.kitab.R
import kotlinx.serialization.Serializable

internal enum class KitabTab(
    val route: String,
    val label: String,
    @param:DrawableRes val iconResource: Int,
) {
    Recent("recent", "Recent", R.drawable.ic_clock),
    Library("library", "Library", R.drawable.ic_home),
    Settings("settings", "Settings", R.drawable.ic_settings),
}

@Serializable
internal data class ReaderDestination(
    val bookUri: String,
)

internal object KitabDestinations {
    fun tabForRoute(route: String?): KitabTab? = KitabTab.entries.firstOrNull { it.route == route }
}
