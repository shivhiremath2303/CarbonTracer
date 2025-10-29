package com.example.carbontracer.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 1. THIS IS YOUR SERVER'S ADDRESS
    // This 'magic' IP (10.0.2.2) is how the Android Emulator
    // connects to your computer's 'localhost'.
    // The port '8000' must match the port you use in your uvicorn command.
    private const val BASE_URL = "http://172.30.109.61:8000/"

    // 2. THIS CREATES THE RETROFIT 'ENGINE'
    val instance: ApiService by lazy {
        // 'by lazy' means this code will only run ONCE,
        // the very first time 'instance' is called.

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL) // Sets the base address for all API calls
            .addConverterFactory(GsonConverterFactory.create()) // Tells Retrofit to use Gson to read/write JSON
            .build() // Creates the final Retrofit object

        // 3. THIS CONNECTS YOUR INTERFACE TO RETROFIT
        // This tells Retrofit to read your 'ApiService' interface
        // and automatically build the networking code for it.
        retrofit.create(ApiService::class.java)
    }
}