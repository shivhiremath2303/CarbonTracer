package com.example.carbontracer.network

import com.example.carbontracer.model.OcrResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("parse/image")
    fun uploadOcrImage(
        @Header("apikey") apiKey: String,
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody? = null,
        @Part("isOverlayRequired") isOverlayRequired: RequestBody? = null
    ): Call<OcrResponse>
}