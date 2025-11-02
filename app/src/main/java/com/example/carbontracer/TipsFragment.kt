package com.example.carbontracer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText

class TipsFragment : Fragment() {

    private lateinit var etElectricity: TextInputEditText
    private lateinit var etVehicleTravel: TextInputEditText
    private lateinit var btnCalculate: Button
    private lateinit var layoutResult: LinearLayout
    private lateinit var tvPredictionResult: TextView
    private lateinit var tvTips: TextView

    // Standard emission factors (example values, replace with more accurate ones if needed)
    private val electricityEmissionFactor = 0.82 // kg CO2e per kWh
    private val vehicleEmissionFactor = 0.21   // kg CO2e per km

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tips, container, false)

        // Initialize Views
        etElectricity = view.findViewById(R.id.et_electricity)
        etVehicleTravel = view.findViewById(R.id.et_vehicle_travel)
        btnCalculate = view.findViewById(R.id.btn_calculate)
        layoutResult = view.findViewById(R.id.layout_result)
        tvPredictionResult = view.findViewById(R.id.tv_prediction_result)
        tvTips = view.findViewById(R.id.tv_tips)

        btnCalculate.setOnClickListener {
            calculateCarbonFootprint()
        }

        return view
    }

    private fun calculateCarbonFootprint() {
        val electricity = etElectricity.text.toString().toFloatOrNull()
        val vehicleTravel = etVehicleTravel.text.toString().toFloatOrNull()

        if (electricity == null || vehicleTravel == null) {
            Toast.makeText(requireContext(), "Please enter valid numbers for all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Simple Calculation Placeholder ---
        // This formula calculates the footprint based on simple emission factors.
        // It is NOT using a machine learning model.
        val electricityFootprint = electricity * electricityEmissionFactor
        val travelFootprint = vehicleTravel * vehicleEmissionFactor

        // Calculate total annual footprint in tonnes
        val totalMonthlyFootprintKg = electricityFootprint + travelFootprint
        val totalAnnualFootprintTonnes = (totalMonthlyFootprintKg * 12) / 1000
        // --- End of Calculation ---

        tvPredictionResult.text = String.format("%.2f tonnes CO2e", totalAnnualFootprintTonnes)
        generateTips(totalAnnualFootprintTonnes)

        layoutResult.visibility = View.VISIBLE
    }

    private fun generateTips(footprint: Double) {
        val tips = StringBuilder()
        // Example tips based on the calculated footprint
        if (footprint > 2.0) {
            tips.append("• Your footprint is higher than average. Consider reducing your travel or electricity consumption.\n")
            tips.append("• Look for energy-efficient appliances for your next purchase.\n")
        } else if (footprint > 1.0) {
            tips.append("• You're on the right track! Small changes can make a big difference.\n")
            tips.append("• Try using public transport or carpooling when possible.\n")
        } else {
            tips.append("• Excellent! Your carbon footprint is low. Keep up the great work!\n")
        }
        tvTips.text = tips.toString()
    }
}
