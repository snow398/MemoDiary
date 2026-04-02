package com.memodiary.data.image

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageStorage {

    private const val IMAGE_DIR = "memo_images"

    /** Copy a content:// or file:// URI into the app's private files directory and return the path. */
    fun saveImageFromUri(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
            val fileName = "img_${System.currentTimeMillis()}_${(100..999).random()}.jpg"
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** Create an empty file in the cache for TakePicture to write into; returns the File. */
    fun createCameraFile(context: Context): File {
        val cacheDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(cacheDir, "PHOTO_${stamp}.jpg")
    }

    /** Move camera-result file from cache into permanent image dir and return the path. */
    fun saveCameraFile(context: Context, cacheFile: File): String? {
        return try {
            val dir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
            val dest = File(dir, cacheFile.name)
            cacheFile.copyTo(dest, overwrite = true)
            cacheFile.delete()
            dest.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /** Delete a stored image file by its absolute path. */
    fun deleteImage(path: String) {
        try { File(path).delete() } catch (_: Exception) {}
    }
}
