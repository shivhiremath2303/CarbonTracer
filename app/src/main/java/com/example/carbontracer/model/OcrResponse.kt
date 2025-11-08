package com.example.carbontracer.model

import com.google.gson.annotations.SerializedName

data class OcrResponse(
    @SerializedName("ParsedResults") val parsedResults: List<ParsedResult>?,
    @SerializedName("OCRExitCode") val ocrExitCode: Int?,
    @SerializedName("IsErroredOnProcessing") val isErroredOnProcessing: Boolean?,
    @SerializedName("ProcessingTimeInMilliseconds") val processingTimeInMilliseconds: String?,
    @SerializedName("SearchablePDFURL") val searchablePDFURL: String?
)

data class ParsedResult(
    @SerializedName("TextOverlay") val textOverlay: TextOverlay?,
    @SerializedName("FileParseExitCode") val fileParseExitCode: Int?,
    @SerializedName("ParsedText") val parsedText: String?,
    @SerializedName("ErrorMessage") val errorMessage: String?,
    @SerializedName("ErrorDetails") val errorDetails: String?
)

data class TextOverlay(
    @SerializedName("Lines") val lines: List<Line>?,
    @SerializedName("HasOverlay") val hasOverlay: Boolean?,
    @SerializedName("Message") val message: String?
)

data class Line(
    @SerializedName("LineText") val lineText: String?,
    @SerializedName("Words") val words: List<Word>?,
    @SerializedName("MaxHeight") val maxHeight: Double?,
    @SerializedName("MinTop") val minTop: Double?
)

data class Word(
    @SerializedName("WordText") val wordText: String?,
    @SerializedName("Left") val left: Double?,
    @SerializedName("Top") val top: Double?,
    @SerializedName("Height") val height: Double?,
    @SerializedName("Width") val width: Double?
)
