package com.example.carbontracer.model

data class OcrResponse(
    val filename: String,
    val ocr_result: OcrResult // Notice this refers to the first class you made
)