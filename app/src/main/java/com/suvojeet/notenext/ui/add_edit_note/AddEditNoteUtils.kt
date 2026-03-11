package com.suvojeet.notenext.ui.add_edit_note

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun createImageFile(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"

    // Use external pictures dir first, fall back to cache/pictures if unavailable
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        ?: File(context.cacheDir, "pictures")

    // Ensure the directory actually exists on disk before creating the temp file
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }

    val image = File.createTempFile(imageFileName, ".jpg", storageDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", image)
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error if no browser found
    }
}
