package com.example.carbontracer.model

data class OcrResult(
    val text: String,
    val confidence: Double,
    val language: String
)