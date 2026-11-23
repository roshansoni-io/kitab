package io.github.roshansoni_io.kitab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Window
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.roshansoni_io.kitab.core.di.AppContainer
import io.github.roshansoni_io.kitab.core.ui.theme.KitabTheme
import io.github.roshansoni_io.kitab.core.ui.theme.isDarkTheme
import io.github.roshansoni_io.kitab.navigation.KitabRoot

class MainActivity : FragmentActivity() {
    private val pendingExternalBookUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingExternalBookUri.value = externalBookUri(intent)
        setContent {
            val container = remember { AppContainer(applicationContext) }
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
            val darkTheme = isDarkTheme(settings.themeMode)
            KitabTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                customSeedColor = settings.customSeedColor,
            ) {
                SystemBarsEffect(window = window, darkTheme = darkTheme)
                KitabRoot(
                    container = container,
                    pendingExternalUri = pendingExternalBookUri.value,
                    onExternalUriHandled = { pendingExternalBookUri.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingExternalBookUri.value = externalBookUri(intent)
    }

    private fun externalBookUri(intent: Intent): Uri? = intent.takeIf { it.action == Intent.ACTION_VIEW }?.data
}

/** Keeps the system bar icons legible when the in-app theme differs from the system theme. */
@Composable
private fun SystemBarsEffect(
    window: Window,
    darkTheme: Boolean,
) {
    SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
