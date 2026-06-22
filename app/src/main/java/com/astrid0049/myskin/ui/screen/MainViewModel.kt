package com.astrid0049.myskin.ui.screen

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrid0049.myskin.model.Skincare
import com.astrid0049.myskin.network.ApiStatus
import com.astrid0049.myskin.network.SkincareApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class MainViewModel : ViewModel() {
    var data = mutableStateOf(emptyList<Skincare>())
        private set

    var status = MutableStateFlow(ApiStatus.LOADING)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    fun retrieveData(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            status.value = ApiStatus.LOADING
            try {
                val activeToken = token.ifEmpty { "anonymous" }
                data.value = SkincareApi.service.getSkincare(activeToken)
                status.value = ApiStatus.SUCCESS
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fetch Failure: ${e.message}")
                status.value = ApiStatus.FAILED
            }
        }
    }

    fun saveData(token: String, nama: String, brand: String, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeToken = token.ifEmpty { "anonymous" }
                val result = SkincareApi.service.postSkincare(
                    activeToken,
                    nama.toRequestBody("text/plain".toMediaTypeOrNull()),
                    brand.toRequestBody("text/plain".toMediaTypeOrNull()),
                    bitmap.toMultipartBody()
                )

                if (result.status == "success") {
                    retrieveData(activeToken)
                } else {
                    throw Exception(result.message)
                }
            } catch (e: Exception) {
                errorMessage.value = "Error saving data: ${e.message}"
            }
        }
    }

    fun deleteData(token: String, id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeToken = token.ifEmpty { "anonymous" }
                val result = SkincareApi.service.deleteSkincare(activeToken, id)

                if (result.status == "success") {
                    retrieveData(activeToken)
                } else {
                    throw Exception(result.message)
                }
            } catch (e: Exception) {
                errorMessage.value = "Error deleting data: ${e.message}"
            }
        }
    }

    private fun Bitmap.toMultipartBody(): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        val requestBody = byteArray.toRequestBody("image/jpg".toMediaTypeOrNull(), 0, byteArray.size)
        return MultipartBody.Part.createFormData("image", "skincare.jpg", requestBody)
    }

    fun clearMessage() { errorMessage.value = null }
}