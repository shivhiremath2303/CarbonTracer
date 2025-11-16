package com.example.carbontracer.model

data class Tip(
    val id: String,
    val title: String,
    val description: String,

    // "Electricity", "Transport", "General"
    val category: String,

    // This is the "ML" part. It links the tip to a data point.
    // e.g., "Air Conditioner", "Refrigerator", "general"
    val trigger: String
)