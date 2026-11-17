package io.github.roshansoni_io.kitab.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

fun hasLibraryAccess(context: Context): Boolean =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            Environment.isExternalStorageManager()
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }

        else -> {
            true
        }
    }

fun launchLibraryAccessSettings(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
) {
    try {
        launcher.launch(
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ),
        )
    } catch (noHandler: ActivityNotFoundException) {
        runCatching {
            launcher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }
}
