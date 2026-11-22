package io.github.roshansoni_io.kitab.core.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

fun openExternalLink(
    context: Context,
    uri: String,
) {
    try {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    } catch (notFound: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link.", Toast.LENGTH_SHORT).show()
    }
}

fun sharePdfFile(
    context: Context,
    filePath: String,
    title: String,
) {
    val file = File(filePath)
    if (!file.isFile) {
        Toast.makeText(context, "The PDF file is no longer available.", Toast.LENGTH_SHORT).show()
        return
    }
    val fileUri =
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (invalidPath: IllegalArgumentException) {
            Toast.makeText(context, "Could not prepare this PDF to send.", Toast.LENGTH_SHORT).show()
            return
        }
    val sendIntent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            clipData = ClipData.newUri(context.contentResolver, title, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    try {
        context.startActivity(Intent.createChooser(sendIntent, "Send book"))
    } catch (notFound: ActivityNotFoundException) {
        Toast.makeText(context, "No app can send this file.", Toast.LENGTH_SHORT).show()
    } catch (permissionDenied: SecurityException) {
        Toast.makeText(context, "Kitab cannot access this PDF to send it.", Toast.LENGTH_SHORT).show()
    }
}
