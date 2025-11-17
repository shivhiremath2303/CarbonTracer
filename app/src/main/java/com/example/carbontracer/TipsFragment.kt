package com.example.carbontracer

import android.os.Bundle
import android.util.Log // Import this
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
// --- TENSORFLOW IMPORTS ---
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Calendar
import java.lang.Exception

class TipsFragment : Fragment() {

    // --- UI Variables ---
    private lateinit var spinnerYear: Spinner
    private lateinit var spinnerMonth: Spinner
    private lateinit var editElectricity: EditText
    private lateinit var editLpg: EditText
    private lateinit var editPetrol: EditText
    private lateinit var editDiesel: EditText
    private lateinit var predictButton: Button
    private lateinit var currentPredictionText: TextView
    private lateinit var layoutFuturePredictions: LinearLayout
    private lateinit var suggestionsText: TextView

    // --- TENSORFLOW VARIABLES ---
    private var tflite: Interpreter? = null
    private val MODEL_FILE_NAME = "lstm_model .tflite" // Must be in app/src/main/assets
    private val N_STEPS = 3 // This MUST match the n_steps in your Python code

    // --- Emission Factors (to simulate XGBoost) ---
    private val ELECTRICITY_FACTOR = 0.4 // kg CO2 per kWh
    private val LPG_FACTOR = 3.0       // kg CO2 per kg
    private val PETROL_FACTOR = 2.3    // kg CO2 per liter
    private val DIESEL_FACTOR = 2.7    // kg CO2 per liter

    // --- ADD YOUR SCALER VALUES FROM PYTHON HERE ---
    // !!! --------------------------------------------------
    // !!! REPLACE THESE with the numbers from your Python script !!!
    // !!! --------------------------------------------------
    private val MODEL_MIN_VAL = 50.0f // The "Scaler Min" value
    private val MODEL_MAX_VAL = 700.0f // The "Scaler Max" value
    // --- END ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tips, container, false)

        // --- 1. Find All UI Elements ---
        spinnerYear = view.findViewById(R.id.spinner_year)
        spinnerMonth = view.findViewById(R.id.spinner_month)
        editElectricity = view.findViewById(R.id.edit_electricity)
        editLpg = view.findViewById(R.id.edit_lpg)
        editPetrol = view.findViewById(R.id.edit_petrol)
        editDiesel = view.findViewById(R.id.edit_diesel)
        predictButton = view.findViewById(R.id.button_predict)
        currentPredictionText = view.findViewById(R.id.text_current_prediction)
        layoutFuturePredictions = view.findViewById(R.id.layout_future_predictions)
        suggestionsText = view.findViewById(R.id.text_suggestions)

        // --- 2. Load the LSTM Model ---
        try {
            tflite = Interpreter(loadModelFile(MODEL_FILE_NAME))
            Log.d("TipsFragment", "TFLite model loaded successfully.")
        } catch (e: Exception) {
            currentPredictionText.text = "Error: Model file not found. Check assets folder."
            Log.e("TipsFragment", "Error loading TFLite model: ${e.message}", e)
        }

        // --- 3. Setup Month Spinner ---
        val monthAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.months_array, // This must exist in res/values/array.xml
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMonth.adapter = adapter
        }

        // --- 4. Set Button Click Listener ---
        predictButton.setOnClickListener {
            predictEmissions()
        }

        // --- 5. Auto-fill from Scanner ---
        checkSharedPrefsForScannedValue()

        return view
    }

    // Helper function to load the model from assets
    private fun loadModelFile(modelName: String): ByteBuffer {
        val assetFileDescriptor = requireContext().assets.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Checks SharedPreferences for a value from the OCR scanner
    private fun checkSharedPrefsForScannedValue() {
        val prefs = requireActivity().getSharedPreferences("AppPrefs", AppCompatActivity.MODE_PRIVATE)
        val scannedValue = prefs.getString("scannedKwh", null)

        if (scannedValue != null) {
            editElectricity.setText(scannedValue)
            prefs.edit().remove("scannedKwh").apply()
        }
    }

    /**
     * This is the main prediction function.
     */
    private fun predictEmissions() {
        val year = spinnerYear.selectedItem.toString().toIntOrNull()
        val monthString = spinnerMonth.selectedItem.toString()
        val monthInt = spinnerMonth.selectedItemPosition // 0 = Jan

        val inputs = mapOf(
            "Electricity" to (editElectricity.text.toString().toDoubleOrNull() ?: 0.0),
            "LPG" to (editLpg.text.toString().toDoubleOrNull() ?: 0.0),
            "Petrol" to (editPetrol.text.toString().toDoubleOrNull() ?: 0.0),
            "Diesel" to (editDiesel.text.toString().toDoubleOrNull() ?: 0.0)
        )

        if (year == null) {
            Toast.makeText(requireContext(), "Please enter a valid year.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- 1. "XGBoost" Simulation ---
        val currentMonthCo2 = (inputs["Electricity"]!! * ELECTRICITY_FACTOR) +
                (inputs["LPG"]!! * LPG_FACTOR) +
                (inputs["Petrol"]!! * PETROL_FACTOR) +
                (inputs["Diesel"]!! * DIESEL_FACTOR)

        currentPredictionText.text = "Predicted Emission for $monthString $year: %.2f kg CO2".format(currentMonthCo2)

        // --- 2. "LSTM" Prediction ---
        // Check for 0 case
        if (currentMonthCo2 == 0.0) {
            layoutFuturePredictions.removeAllViews() // Clear old results
            val zeroTextView = TextView(requireContext()).apply {
                text = "6-month forecast is 0.00 kg CO2"
                setPadding(0, 8, 0, 8)
                textSize = 16f
            }
            layoutFuturePredictions.addView(zeroTextView)
        } else if (tflite == null) {
            // Check for model not loaded
            layoutFuturePredictions.removeAllViews()
            val errorTextView = TextView(requireContext()).apply {
                text = "Error: LSTM model not loaded."
                setPadding(0, 8, 0, 8)
                textSize = 16f
            }
            layoutFuturePredictions.addView(errorTextView)
        } else {
            // Run the real prediction
            runLstmPrediction(currentMonthCo2.toFloat(), year, monthInt)
        }

        // --- 3. "Suggestion" Model ---
        val suggestionList = generateSuggestions(inputs)
        suggestionsText.text = suggestionList.joinToString(separator = "\n") { "- $it" }
    }

    /**
     * This is the Kotlin version of your Python 'generate_always_on_suggestions'
     */
    private fun generateSuggestions(inputs: Map<String, Double>): List<String> {
        val suggestions = mutableListOf<String>()

        val electricity = inputs["Electricity"] ?: 0.0
        val lpg = inputs["LPG"] ?: 0.0
        val petrol = inputs["Petrol"] ?: 0.0
        val diesel = inputs["Diesel"] ?: 0.0

        suggestions.add("Consider adopting energy-saving habits to reduce emissions.")

        if (electricity > 600) {
            suggestions.add("High electricity use detected. Consider efficient appliances.")
        } else if (electricity > 200) {
            suggestions.add("Your electricity use is moderate. Try unplugging devices when not in use.")
        }

        if (lpg > 10) {
            suggestions.add("Optimize LPG usage by using efficient cooking methods.")
        }

        if (petrol > 20) {
            suggestions.add("High petrol use detected. Consider carpooling or public transport.")
        }

        if (diesel > 20) {
            suggestions.add("High diesel use detected. Consider efficient vehicles or alternatives.")
        }

        if (suggestions.size == 1) { // Only the default message was added
            suggestions.add("Your consumption looks balanced. Keep up the good work!")
        }

        return suggestions
    }


    // --- SCALER HELPER FUNCTIONS ---

    /**
     * Scales a raw CO2 value (e.g., 150.0) to a 0-1 value
     */
    private fun scale(value: Float): Float {
        return (value - MODEL_MIN_VAL) / (MODEL_MAX_VAL - MODEL_MIN_VAL)
    }

    /**
     * Un-scales a 0-1 prediction back to a real CO2 value
     */
    private fun inverseScale(scaledValue: Float): Float {
        return (scaledValue * (MODEL_MAX_VAL - MODEL_MIN_VAL)) + MODEL_MIN_VAL
    }

    /**
     * This function runs your .tflite LSTM model
     * (This is the fully REPLACED and FIXED version)
     */
    private fun runLstmPrediction(currentMonthCo2: Float, startYear: Int, startMonth: Int) {

        // --- 1. PRE-PROCESSING (Builds the input) ---
        //
        // --- THIS IS THE FIX ---
        // We feed the model a "flat" history of your current prediction.
        // This stops the model from seeing a "jump" and exploding.
        val scaledCurrent = scale(currentMonthCo2) // e.g., 100kg -> 0.11

        val inputBuffer = ByteBuffer.allocateDirect(N_STEPS * 1 * 4).order(ByteOrder.nativeOrder())
        inputBuffer.putFloat(scaledCurrent) // Input 1
        inputBuffer.putFloat(scaledCurrent) // Input 2
        inputBuffer.putFloat(scaledCurrent) // Input 3 (This is the stable history)
        // --- END OF FIX ---

        // --- 2. RUN INFERENCE LOOP ---
        val predictions = mutableListOf<Float>()
        val outputBuffer = ByteBuffer.allocateDirect(1 * 4).order(ByteOrder.nativeOrder())

        try {
            for (i in 1..6) {
                outputBuffer.rewind()
                tflite?.run(inputBuffer, outputBuffer)

                outputBuffer.rewind()
                val nextScaledPred = outputBuffer.float // This is a 0-1 value

                // Un-scale the prediction back to real kg
                val realPred = inverseScale(nextScaledPred)
                predictions.add(realPred) // Add the real kg value to our list

                // Update inputBuffer for next loop
                val tempBuffer = ByteBuffer.allocateDirect(N_STEPS * 4).order(ByteOrder.nativeOrder())
                tempBuffer.putFloat(inputBuffer.getFloat(4)) // shift
                tempBuffer.putFloat(inputBuffer.getFloat(8)) // shift
                tempBuffer.putFloat(nextScaledPred)          // add new *scaled* prediction
                inputBuffer.clear()
                inputBuffer.put(tempBuffer.array())
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error running prediction: ${e.message}", Toast.LENGTH_SHORT).show()
            layoutFuturePredictions.removeAllViews()
            val errorTextView = TextView(requireContext()).apply {
                text = "Error running prediction."
                setPadding(0, 8, 0, 8)
                textSize = 16f
            }
            layoutFuturePredictions.addView(errorTextView)
            return
        }

        // --- 3. POST-PROCESSING (Display the forecast) ---
        layoutFuturePredictions.removeAllViews()
        val calendar = Calendar.getInstance()
        val monthNames = resources.getStringArray(R.array.months_array)

        for (i in 0 until predictions.size) {
            calendar.set(startYear, startMonth, 1)
            calendar.add(Calendar.MONTH, i + 1)
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)

            val predictionTextView = TextView(requireContext()).apply {
                // 'predictions[i]' is now the un-scaled, correct kg value
                text = "${monthNames[month]} $year: %.2f kg CO2".format(predictions[i])
                setPadding(0, 8, 0, 8)
                textSize = 16f
            }
            layoutFuturePredictions.addView(predictionTextView)
        }
    }
}