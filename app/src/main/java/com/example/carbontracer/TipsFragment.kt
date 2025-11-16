package com.example.carbontracer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TipsFragment : Fragment() {

    private lateinit var etYear: TextInputEditText
    private lateinit var spinnerMonth: Spinner
    private lateinit var etElectricity: TextInputEditText
    private lateinit var etLpg: TextInputEditText
    private lateinit var etPetrol: TextInputEditText
    private lateinit var etDiesel: TextInputEditText
    private lateinit var btnPredict: Button
    private lateinit var layoutPredictionResults: LinearLayout
    private lateinit var tvCurrentPrediction: TextView
    private lateinit var layoutFuturePredictions: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tips, container, false)

        etYear = view.findViewById(R.id.etYear)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        etElectricity = view.findViewById(R.id.etElectricity)
        etLpg = view.findViewById(R.id.etLpg)
        etPetrol = view.findViewById(R.id.etPetrol)
        etDiesel = view.findViewById(R.id.etDiesel)
        btnPredict = view.findViewById(R.id.btnPredict)
        layoutPredictionResults = view.findViewById(R.id.layout_prediction_results)
        tvCurrentPrediction = view.findViewById(R.id.tv_current_prediction)
        layoutFuturePredictions = view.findViewById(R.id.layout_future_predictions)

        // Setup Month Spinner
        val monthAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.months_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMonth.adapter = adapter
        }

        btnPredict.setOnClickListener {
            predictEmissions()
        }

        return view
    }

    private fun predictEmissions() {
        val year = etYear.text.toString().toIntOrNull()
        val month = spinnerMonth.selectedItemPosition + 1
        val electricity = etElectricity.text.toString().toDoubleOrNull() ?: 0.0
        val lpg = etLpg.text.toString().toDoubleOrNull() ?: 0.0
        val petrol = etPetrol.text.toString().toDoubleOrNull() ?: 0.0
        val diesel = etDiesel.text.toString().toDoubleOrNull() ?: 0.0

        if (year == null) {
            Toast.makeText(requireContext(), "Please enter a valid year.", Toast.LENGTH_SHORT).show()
            return
        }

        // ** Placeholder for ML Model Integration **
        // In a real scenario, you would pass these values to your ML model.
        // For now, we will use dummy data to demonstrate the UI.

        val currentPrediction = 150.75 + (diesel * 2.68) // Dummy value, adjusted for diesel
        val futurePredictions = listOf(145.50, 148.20, 152.90, 155.10, 158.60, 160.30) // Dummy values

        // Display the results
        tvCurrentPrediction.text = "Predicted Emission for ${spinnerMonth.selectedItem} $year: %.2f kg CO2".format(currentPrediction)

        layoutFuturePredictions.removeAllViews()
        for (i in 0 until futurePredictions.size) {
            val futureMonth = (month + i) % 12 + 1
            val futureYear = year + (month + i) / 12

            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, futureMonth - 1) }.time)

            val predictionTextView = TextView(requireContext()).apply {
                text = "$monthName $futureYear: %.2f kg CO2".format(futurePredictions[i])
                setPadding(0, 8, 0, 8)
            }
            layoutFuturePredictions.addView(predictionTextView)
        }

        layoutPredictionResults.visibility = View.VISIBLE
    }
}
