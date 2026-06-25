package com.astrid0049.myskin.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ImageStorage {
    fun saveToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val directory = File(context.filesDir, "skincare_images")
            if (!directory.exists()) directory.mkdirs()
            
            val file = File(directory, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error saving image: ${e.message}")
            null
        }
    }

    fun loadFromInternalStorage(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error loading image: ${e.message}")
            null
        }
    }

    fun deleteFile(path: String) {
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e("ImageStorage", "Error deleting image: ${e.message}")
        }
    }
}
