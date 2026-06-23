package com.astrid0049.myskin.network

import com.astrid0049.myskin.model.Skincare
import com.astrid0049.myskin.model.OpStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

private const val BASE_URL = "https://myskin-rest-api.vercel.app/"

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

interface SkincareApiService {
    @GET("api/skincare")
    suspend fun getSkincare(
        @Header("Authorization") token: String
    ): List<Skincare>

    @Multipart
    @POST("api/skincare")
    suspend fun postSkincare(
        @Header("Authorization") token: String,
        @Part("nama") nama: String,
        @Part("brand") brand: String,
        @Part image: MultipartBody.Part
    ): OpStatus

    @DELETE("api/skincare")
    suspend fun deleteSkincare(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): OpStatus
}

object SkincareApi {
    val service: SkincareApiService by lazy {
        retrofit.create(SkincareApiService::class.java)
    }

    fun getSkincareUrl(imageId: String): String {
        return "${BASE_URL}api/image?id=$imageId"
    }
}

enum class ApiStatus { LOADING, SUCCESS, FAILED }