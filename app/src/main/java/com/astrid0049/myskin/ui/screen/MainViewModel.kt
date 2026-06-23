package com.astrid0049.myskin.ui.screen

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrid0049.myskin.database.SkincareDao
import com.astrid0049.myskin.model.Skincare
import com.astrid0049.myskin.model.User
import com.astrid0049.myskin.network.ApiStatus
import com.astrid0049.myskin.network.SkincareApi
import com.astrid0049.myskin.network.UserDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class MainViewModel : ViewModel() {
    var data = mutableStateOf(emptyList<Skincare>())
        private set

    var status = MutableStateFlow(ApiStatus.LOADING)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    private lateinit var userDataStore: UserDataStore

    var currentUser = mutableStateOf<User?>(null)

    var isLoggedIn = mutableStateOf(false)

    private var currentToken: String = "anonymous"

    fun initAuth(dataStore: UserDataStore, dao: SkincareDao) {
        this.userDataStore = dataStore
        viewModelScope.launch {
            userDataStore.isLoggedInFlow.collect { loggedIn ->
                isLoggedIn.value = loggedIn
            }
        }
        viewModelScope.launch {
            userDataStore.userFlow.collect { user ->
                currentUser.value = if (isLoggedIn.value) user else null
            }
        }
        viewModelScope.launch {
            userDataStore.tokenFlow.collect { token ->
                currentToken = token
                retrieveData(currentToken, dao)
            }
        }
    }

    fun simulateLogin() {
        viewModelScope.launch {
            val mockUser = User(
                name = "Astrid Dev",
                email = "astrid@myskin.com",
                photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb"
            )
            userDataStore.loginUser(mockUser, "bearer_token_abc123")
        }
    }

    fun simulateLogout() {
        viewModelScope.launch {
            userDataStore.logoutUser()
        }
    }

    fun refreshData(dao: SkincareDao) {
        retrieveData(currentToken, dao)
    }

    fun postNewData(nama: String, brand: String, bitmap: Bitmap, dao: SkincareDao) {
        saveData(currentToken, nama, brand, bitmap, dao)
    }

    fun executeDelete(id: String, dao: SkincareDao) {
        deleteData(currentToken, id, dao)
    }

    fun retrieveData(token: String, dao: SkincareDao) {
        viewModelScope.launch {
            status.value = ApiStatus.LOADING
            try {
                val activeToken = token.ifEmpty { "anonymous" }
                val networkData = withContext(Dispatchers.IO) {
                    SkincareApi.service.getSkincare(activeToken)
                }

                Log.d("MainViewModel", "Fetch Success: received ${networkData.size} items")
                data.value = networkData
                status.value = ApiStatus.SUCCESS

                // Update cache
                withContext(Dispatchers.IO) {
                    try {
                        dao.clearAll()
                        dao.insertAll(networkData)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to update Room cache: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fetch Failure: ${e.message}. Attempting fallback.")
                try {
                    val fallbackData = withContext(Dispatchers.IO) {
                        dao.getAllSkincare()
                    }
                    data.value = fallbackData
                    if (fallbackData.isNotEmpty()) {
                        status.value = ApiStatus.SUCCESS
                    } else {
                        status.value = ApiStatus.FAILED
                    }
                } catch (fallbackError: Exception) {
                    Log.e("MainViewModel", "Fallback Failure: ${fallbackError.message}")
                    status.value = ApiStatus.FAILED
                }
            }
        }
    }

    fun saveData(token: String, nama: String, brand: String, bitmap: Bitmap, dao: SkincareDao) {
        viewModelScope.launch {
            try {
                status.value = ApiStatus.LOADING
                val activeToken = token.ifEmpty { "anonymous" }

                val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                }

                val maxSize = 600
                val ratio = safeBitmap.width.toFloat() / safeBitmap.height.toFloat()
                val (finalWidth, finalHeight) = if (safeBitmap.width > safeBitmap.height) {
                    maxSize to (maxSize / ratio).toInt()
                } else {
                    (maxSize * ratio).toInt() to maxSize
                }

                val scaledBitmap = Bitmap.createScaledBitmap(safeBitmap, finalWidth, finalHeight, true)
                val stream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                val byteArray = stream.toByteArray()

                val imageBody = byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val imagePart = MultipartBody.Part.createFormData("image", "skincare.jpg", imageBody)

                val result = withContext(Dispatchers.IO) {
                    SkincareApi.service.postSkincare(
                        token = activeToken,
                        nama = nama,
                        brand = brand,
                        image = imagePart
                    )
                }

                if (result.status.equals("success", ignoreCase = true)) {
                    errorMessage.value = "Skincare saved successfully!"
                    retrieveData(activeToken, dao)
                } else {
                    status.value = ApiStatus.SUCCESS
                    errorMessage.value = result.message ?: "Server error"
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Save Failure: ${e.message}")
                status.value = ApiStatus.FAILED
                errorMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteData(token: String, id: String, dao: SkincareDao) {
        viewModelScope.launch {
            try {
                val activeToken = token.ifEmpty { "anonymous" }
                val result = withContext(Dispatchers.IO) {
                    SkincareApi.service.deleteSkincare(activeToken, id)
                }

                if (result.status.equals("success", ignoreCase = true)) {
                    errorMessage.value = "Item deleted"
                    retrieveData(activeToken, dao)
                } else {
                    errorMessage.value = result.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                errorMessage.value = "Error deleting data: ${e.message}"
            }
        }
    }

    fun clearMessage() { errorMessage.value = null }
}