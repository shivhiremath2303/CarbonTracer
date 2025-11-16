package com.example.carbontracer.model

import com.google.firebase.firestore.DocumentId

data class Appliance(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val applianceName: String = "",
    val applianceCount: Int = 1,
    // Electricity
    val wattageUsed: Int = 0,
    val dailyHoursUsed: Double = 0.0,
    // Other Fuels (monthly consumption)
    val dieselLiters: Double = 0.0
)