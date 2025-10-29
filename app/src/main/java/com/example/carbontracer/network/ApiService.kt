package com.example.carbontracer.network

import com.example.carbontracer.model.OcrResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    /**
     * This function defines how to call your /ocr/ endpoint
     */
    @Multipart
    @POST("ocr/")
    fun uploadOcrImage(
        @Part file: MultipartBody.Part
    ): Call<OcrResponse>

    /**
     * You would add your /parse_email/ endpoint here too
     */
    // @Multipart
    // @POST("parse_email/")
    // fun uploadEmailFile(...): Call<YourEmailResponseClass>
}