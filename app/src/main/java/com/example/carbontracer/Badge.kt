package com.example.carbontracer

data class Badge(
    val name: String,
    val description: String,
    val icon: Int, // Drawable resource ID
    var isEarned: Boolean = false
)
