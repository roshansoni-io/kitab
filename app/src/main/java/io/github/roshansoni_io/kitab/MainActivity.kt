package io.github.roshansoni_io.kitab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.github.roshansoni_io.kitab.ui.KitabRoot
import io.github.roshansoni_io.kitab.ui.theme.KitabTheme

class MainActivity : ComponentActivity() {
    private val pendingExternalBookUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingExternalBookUri.value = externalBookUri(intent)
        setContent {
            KitabTheme {
                KitabRoot(
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
