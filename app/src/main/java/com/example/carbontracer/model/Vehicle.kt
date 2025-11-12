package com.example.carbontracer.model

import com.google.firebase.firestore.DocumentId

data class Vehicle(
    @DocumentId
    val id: String = "", // Firestore document ID
    val userId: String = "",
    val nickname: String = "",
    val vehicleType: String = "", // e.g., "Car", "Motorcycle"
    val fuelType: String = "", // e.g., "Petrol", "Diesel", "Electric"
    val efficiency: Double = 0.0, // e.g., km/L or km/kWh
    val efficiencyUnit: String = "km/L" // e.g., "km/L", "km/kWh"
)