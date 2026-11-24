package io.github.roshansoni_io.kitab.navigation

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppNavigationBar(
    selectedTab: KitabTab,
    onSelectTab: (KitabTab) -> Unit,
) {
    ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        KitabTab.entries.forEach { tab ->
            ShortNavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconResource),
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label) },
                colors =
                    ShortNavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface,
                        selectedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}
