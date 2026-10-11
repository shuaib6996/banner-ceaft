package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class SaveResult {
    object Success : SaveResult()
    object PermissionDenied : SaveResult()
    class Failure(val message: String) : SaveResult()
}

class SaveService(private val context: Context) {

    suspend fun saveToGallery(
        imageBytes: ByteArray,
        albumName: String = "YT Banners"
    ): SaveResult = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext SaveResult.Failure("No image data available to save.")
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "YT_Banner_$timeStamp.png"

        return@withContext try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // MediaStore API for Android 10+ (API 29+) - Zero runtime permission needed for app's own pictures directory
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/$albumName"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(imageBytes)
                        out.flush()
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    SaveResult.Success
                } else {
                    SaveResult.Failure("Could not create MediaStore entry.")
                }
            } else {
                // Legacy Android 9 and lower
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, albumName)
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }

                val imageFile = File(appDir, fileName)
                FileOutputStream(imageFile).use { out ->
                    out.write(imageBytes)
                    out.flush()
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                SaveResult.Success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to gallery", e)
            SaveResult.Failure("Couldn't save your banner. Please check that this app has permission to access your photos, then try again.")
        }
    }

    companion object {
        private const val TAG = "SaveService"
    }
}
