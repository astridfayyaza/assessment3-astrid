package com.astrid0049.myskin.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrid0049.myskin.database.SkincareDao
import com.astrid0049.myskin.model.Skincare
import com.astrid0049.myskin.model.User
import com.astrid0049.myskin.network.ApiStatus
import com.astrid0049.myskin.network.SkincareApi
import com.astrid0049.myskin.network.UserDataStore
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID

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

    private fun getAuthHeader(token: String): String {
        return if (token == "anonymous" || token.isEmpty()) "anonymous" else "Bearer $token"
    }

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

    fun loginGoogle(context: Context) {
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("1094089185796-n0nmod730ro2k1ja24csr167e5f3c0hi.apps.googleusercontent.com")
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    val user = User(
                        name = credential.displayName ?: "User",
                        email = credential.id,
                        photoUrl = credential.profilePictureUri?.toString() ?: ""
                    )
                    userDataStore.loginUser(user, credential.idToken)
                    Log.d("MainViewModel", "Google Login Success: ${user.email}")
                }
            } catch (e: GetCredentialException) {
                Log.e("MainViewModel", "Credential Error: ${e.message}")
                errorMessage.value = "Login failed: ${e.message}"
            } catch (e: Exception) {
                Log.e("MainViewModel", "Login Error: ${e.message}")
                errorMessage.value = "An unexpected error occurred"
            }
        }
    }

    fun logout() {
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

    fun updateExistingData(id: String, nama: String, brand: String, bitmap: Bitmap?, dao: SkincareDao) {
        putData(currentToken, id, nama, brand, bitmap, dao)
    }

    fun executeDelete(id: String, dao: SkincareDao) {
        deleteData(currentToken, id, dao)
    }

    fun retrieveData(token: String, dao: SkincareDao) {
        viewModelScope.launch {
            status.value = ApiStatus.LOADING
            try {
                val authHeader = getAuthHeader(token)
                val networkData = withContext(Dispatchers.IO) {
                    SkincareApi.service.getSkincare(authHeader)
                }

                Log.d("MainViewModel", "Fetch Success: received ${networkData.size} items")
                data.value = networkData
                status.value = ApiStatus.SUCCESS

                withContext(Dispatchers.IO) {
                    try {
                        dao.clearAll()
                        dao.insertAll(networkData)
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to update Room cache: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Fetch Failure: ${e.message}")
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
                    status.value = ApiStatus.FAILED
                }
            }
        }
    }

    fun saveData(token: String, nama: String, brand: String, bitmap: Bitmap, dao: SkincareDao) {
        viewModelScope.launch {
            try {
                status.value = ApiStatus.SUCCESS
                val authHeader = getAuthHeader(token)

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
                        token = authHeader,
                        nama = nama,
                        brand = brand,
                        image = imagePart
                    )
                }

                if (result.status.equals("success", ignoreCase = true)) {
                    errorMessage.value = "Skincare saved successfully!"
                } else {
                    errorMessage.value = result.message ?: "Server error"
                }
                retrieveData(token, dao)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Save Result: ${e.message}")
                retrieveData(token, dao)
            }
        }
    }

    fun deleteData(token: String, id: String, dao: SkincareDao) {
        viewModelScope.launch {
            try {
                val authHeader = getAuthHeader(token)
                val result = withContext(Dispatchers.IO) {
                    SkincareApi.service.deleteSkincare(authHeader, id)
                }

                if (result.status.equals("success", ignoreCase = true)) {
                    errorMessage.value = "Item deleted"
                } else {
                    errorMessage.value = result.message ?: "Unknown error"
                }
                retrieveData(token, dao)
            } catch (e: Exception) {
                errorMessage.value = "Item deleted"
                retrieveData(token, dao)
            }
        }
    }

    fun putData(token: String, id: String, nama: String, brand: String, bitmap: Bitmap?, dao: SkincareDao) {
        viewModelScope.launch {
            try {
                status.value = ApiStatus.SUCCESS
                val authHeader = getAuthHeader(token)

                var imagePart: MultipartBody.Part? = null
                
                bitmap?.let {
                    val safeBitmap = if (it.config == Bitmap.Config.HARDWARE) {
                        it.copy(Bitmap.Config.ARGB_8888, false)
                    } else {
                        it
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
                    imagePart = MultipartBody.Part.createFormData("image", "skincare.jpg", imageBody)
                }

                val result = withContext(Dispatchers.IO) {
                    SkincareApi.service.putSkincare(
                        token = authHeader,
                        id = id,
                        nama = nama,
                        brand = brand,
                        image = imagePart
                    )
                }

                if (result.status.equals("success", ignoreCase = true)) {
                    errorMessage.value = "Skincare updated successfully!"
                } else {
                    errorMessage.value = result.message ?: "Server error"
                }
                retrieveData(token, dao)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Update Result: ${e.message}")
                retrieveData(token, dao)
            }
        }
    }

    fun clearMessage() { errorMessage.value = null }
}