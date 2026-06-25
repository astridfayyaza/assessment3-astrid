package com.astrid0049.myskin.network

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.astrid0049.myskin.database.SkincareDatabase
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = SkincareDatabase.getDatabase(applicationContext)
        val dao = database.skincareDao()
        val userDataStore = UserDataStore(applicationContext)
        
        return try {
            val token = userDataStore.tokenFlow.first()
            val authHeader = if (token == "anonymous" || token.isEmpty()) "anonymous" else "Bearer $token"
            val unsyncedItems = dao.getUnsyncedSkincare()

            if (unsyncedItems.isEmpty()) return Result.success()

            var allSuccess = true

            for (item in unsyncedItems) {
                try {
                    val localPath = item.localImagePath ?: continue
                    val localFile = File(localPath)
                    if (localFile.exists()) {
                        val bitmap = ImageStorage.loadFromInternalStorage(localPath)
                        if (bitmap != null) {
                            val stream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                            val byteArray = stream.toByteArray()
                            val imageBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
                            val imagePart = MultipartBody.Part.createFormData("image", "skincare.jpg", imageBody)

                            val response = SkincareApi.service.postSkincare(
                                token = authHeader,
                                nama = item.nama,
                                brand = item.brand,
                                image = imagePart
                            )

                            if (response.status.equals("success", ignoreCase = true)) {
                                dao.deleteById(item.id)
                                ImageStorage.deleteFile(localPath)
                            } else {
                                allSuccess = false
                            }
                        }
                    } else {
                        dao.deleteById(item.id)
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to sync item ${item.id}: ${e.message}")
                    allSuccess = false
                }
            }

            if (allSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error in sync worker: ${e.message}")
            Result.retry()
        }
    }
}
