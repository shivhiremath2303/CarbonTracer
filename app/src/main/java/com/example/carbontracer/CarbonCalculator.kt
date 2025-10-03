package com.example.carbontracer

object CarbonCalculator {

    // Emission factors (kg CO2e per unit)
    private const val ELECTRICITY_FACTOR = 0.4 // kg CO2e per kWh
    private const val CAR_FACTOR = 0.18 // kg CO2e per km
    private const val BUS_FACTOR = 0.08 // kg CO2e per km
    private const val TRAIN_FACTOR = 0.04 // kg CO2e per km

    fun calculateElectricityEmissions(kwh: Double): Double {
        return kwh * ELECTRICITY_FACTOR
    }

    fun calculateTransportEmissions(transportType: String, distance: Double): Double {
        return when (transportType) {
            "Car" -> distance * CAR_FACTOR
            "Bus" -> distance * BUS_FACTOR
            "Train" -> distance * TRAIN_FACTOR
            "Walk/Cycle" -> 0.0
            else -> 0.0
        }
    }
}
