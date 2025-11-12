package com.example.carbontracer.model


import com.google.firebase.firestore.DocumentId

data class Appliance(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val applianceName: String = "",
    val applianceCount: Int = 1,
    val wattageUsed: Int = 0, // From your internal map
    val dailyHoursUsed: Double = 0.0 // From user
)