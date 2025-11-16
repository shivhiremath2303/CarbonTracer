package com.example.carbontracer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
// --- TENSORFLOW IMPORTS ---
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Calendar // For getting month names

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
    private lateinit var forecastText: TextView

    // --- TENSORFLOW VARIABLES ---
    private var tflite: Interpreter? = null
    private val MODEL_FILE_NAME = "lstm_model.tflite" // This MUST match your file in assets
    private val N_STEPS = 3 // This MUST match the n_steps in your Python code

    // --- Emission Factors (to simulate XGBoost) ---
    // These are simplified factors to calculate current CO2
    private val ELECTRICITY_FACTOR = 0.4 // kg CO2 per kWh (Example, use your own)
    private val LPG_FACTOR = 3.0       // kg CO2 per kg
    private val PETROL_FACTOR = 2.3    // kg CO2 per liter
    private val DIESEL_FACTOR = 2.7    // kg CO2 per liter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tips, container, false)
        userId = auth.currentUser?.uid

        // --- 1. Find all UI elements FIRST ---
        // This fixes the crash by initializing your variables
        spinnerYear = view.findViewById(R.id.spinner_year)
        spinnerMonth = view.findViewById(R.id.spinner_month)
        editElectricity = view.findViewById(R.id.edit_electricity)
        editLpg = view.findViewById(R.id.edit_lpg)
        editPetrol = view.findViewById(R.id.edit_petrol)
        editDiesel = view.findViewById(R.id.edit_diesel)
        predictButton = view.findViewById(R.id.button_predict)
        currentPredictionText = view.findViewById(R.id.text_current_prediction)
        forecastText = view.findViewById(R.id.text_forecast_results)

        // --- 2. Load the LSTM Model ---
        try {
            tflite = Interpreter(loadModelFile(MODEL_FILE_NAME))
        } catch (e: Exception) {
            // Now this line is safe and will not crash
            forecastText.text = "Error: LSTM model not loaded. Check assets folder."
        }

        // --- 3. Set Button Click Listener ---
        predictButton.setOnClickListener {
            runFullPrediction()
        }

        return view
    }

    // Helper function to load the model from assets
    private fun loadModelFile(modelName: String): ByteBuffer {
        val assetFileDescriptor = context?.assets?.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor!!.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun runFullPrediction() {
        // --- 1. Get all values from the form ---
        val year = spinnerYear.selectedItem.toString().toInt()
        val monthString = spinnerMonth.selectedItem.toString()
        val monthInt = spinnerMonth.selectedItemPosition // 0 = Jan, 1 = Feb...

        val electricity = editElectricity.text.toString().toFloatOrNull() ?: 0f
        val lpg = editLpg.text.toString().toFloatOrNull() ?: 0f
        val petrol = editPetrol.text.toString().toFloatOrNull() ?: 0f
        val diesel = editDiesel.text.toString().toFloatOrNull() ?: 0f

        // --- 2. Run "XGBoost" Simulation (The "Now-cast") ---
        // This calculates the current month's CO2 based on your factors
        val currentMonthCo2 = (electricity * ELECTRICITY_FACTOR) +
                (lpg * LPG_FACTOR) +
                (petrol * PETROL_FACTOR) +
                (diesel * DIESEL_FACTOR)

        currentPredictionText.text = "Predicted Emission for $monthString $year: %.2f kg CO2".format(currentMonthCo2)

        // --- 3. Run "LSTM" Prediction (The "Forecast") ---
        if (tflite == null) {
            forecastText.text = "Error: LSTM model not loaded. Check file name in code."
            return
        }

        runLstmPrediction(currentMonthCo2.toFloat(), year, monthInt)
    }

    /**
     * This function runs your .tflite LSTM model
     */
    private fun runLstmPrediction(currentMonthCo2: Float, startYear: Int, startMonth: Int) {

        // --- 1. PRE-PROCESSING (Builds the input) ---
        // Your model needs 3 inputs (n_steps=3). We'll use 2 placeholder "past" values
        // and your new "current" value to start the trend.
        val pastMonth1 = (currentMonthCo2 * 0.95f) // Fake past data
        val pastMonth2 = (currentMonthCo2 * 1.02f) // Fake past data

        // Create the input buffer: shape [1, 3, 1]
        val inputBuffer = ByteBuffer.allocateDirect(N_STEPS * 1 * 4).order(ByteOrder.nativeOrder())
        inputBuffer.putFloat(pastMonth1)
        inputBuffer.putFloat(pastMonth2)
        inputBuffer.putFloat(currentMonthCo2)

        // --- 2. RUN INFERENCE LOOP (Just like your Python code) ---
        val predictions = mutableListOf<Float>()
        val outputBuffer = ByteBuffer.allocateDirect(1 * 4).order(ByteOrder.nativeOrder()) // Model outputs 1 value

        try {
            for (i in 1..6) { // Run prediction 6 times (for 6 months)
                outputBuffer.rewind() // Reset output buffer
                tflite?.run(inputBuffer, outputBuffer)

                outputBuffer.rewind() // Read the output
                val nextPred = outputBuffer.float
                predictions.add(nextPred)

                // Update inputBuffer for next loop (shift data left)
                val tempBuffer = ByteBuffer.allocateDirect(N_STEPS * 4).order(ByteOrder.nativeOrder())
                tempBuffer.putFloat(inputBuffer.getFloat(4)) // shift
                tempBuffer.putFloat(inputBuffer.getFloat(8)) // shift
                tempBuffer.putFloat(nextPred)                // add new
                inputBuffer.clear()
                inputBuffer.put(tempBuffer.array())
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error running prediction: ${e.message}", Toast.LENGTH_SHORT).show()
            return
        }

        // --- 3. POST-PROCESSING (Display the forecast) ---
        val calendar = Calendar.getInstance()
        val monthNames = resources.getStringArray(R.array.months)
        val forecastResult = StringBuilder()

        for (i in 0 until predictions.size) {
            // Calculate the correct future month and year
            calendar.set(startYear, startMonth, 1)
            calendar.add(Calendar.MONTH, i + 1)
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)

            forecastResult.append(
                "%s %d: %.2f kg CO2\n".format(
                    monthNames[month],
                    year,
                    predictions[i]
                )
            )
        }

        forecastText.text = forecastResult.toString()
    }
}