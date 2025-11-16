
package com.example.carbontracer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Calendar

class PredictionActivity : AppCompatActivity() {

    private lateinit var tflite_current: Interpreter

    // UI Elements
    private lateinit var editElectricity: EditText
    private lateinit var editLpg: EditText
    private lateinit var editPetrol: EditText
    private lateinit var editDiesel: EditText
    private lateinit var editPng: EditText
    private lateinit var predictButton: Button

    private val fixed_vals = mapOf(
        "Household_Size" to 3.0f,
        "Urban" to 1.0f,
        "Has_Car" to 1.0f,
        "Car_CC" to 0.0f,
        "Car_Fuel_Type_enc" to 1.0f,
        "Has_Bike" to 1.0f,
        "Bike_CC" to 0.0f,
        "Bike_Monthly_km" to 0.0f,
        "Bike_Fuel_L" to 0.0f,
        "Car_Monthly_km" to 0.0f,
        "Car_Fuel_L" to 0.0f,
        "AC_Tonnage" to 0.0f,
        "AC_Hours_per_day" to 0.0f,
        "Electricity_kWh_other" to 362.0f,
        "Electricity_kWh_AC" to 0.0f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        try {
            tflite_current = Interpreter(loadModelFile("xgboost_model.tflite"))
        } catch (ex: Exception) {
            Toast.makeText(this, "Error loading model: ${ex.message}", Toast.LENGTH_LONG).show()
            finish() // Can't proceed if model fails
            return
        }

        editElectricity = findViewById(R.id.edit_electricity)
        editLpg = findViewById(R.id.edit_lpg)
        editPetrol = findViewById(R.id.edit_petrol)
        editDiesel = findViewById(R.id.edit_diesel)
        editPng = findViewById(R.id.edit_png)
        predictButton = findViewById(R.id.button_predict)

        predictButton.setOnClickListener {
            try {
                val inputArray = buildInputArray()
                val currentPrediction = runPrediction(inputArray)

                // --- THIS IS THE KEY CHANGE ---
                // Send the result back to the previous activity.
                val resultIntent = Intent()
                resultIntent.putExtra("predictionResult", currentPrediction)
                setResult(Activity.RESULT_OK, resultIntent)
                finish() // Close this activity

            } catch (e: Exception) {
                Toast.makeText(this, "Prediction Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        val fileDescriptor = assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getFloatFromEditText(editText: EditText): Float {
        return editText.text.toString().toFloatOrNull() ?: 0.0f
    }

    private fun buildInputArray(): FloatArray {
        val featureOrder = listOf(
            "Year", "Month", "Household_Size", "Urban", "Has_Car", "Car_CC",
            "Car_Monthly_km", "Car_Fuel_L", "Has_Bike", "Bike_CC", "Bike_Monthly_km",
            "Bike_Fuel_L", "AC_Tonnage", "AC_Hours_per_day", "Electricity_kWh_total",
            "Electricity_kWh_other", "Electricity_kWh_AC", "LPG_kg", "PNG_scm",
            "Wood_kg", "Kerosene_L", "Charcoal_kg", "Coal_kg", "Propane_kg",
            "Petrol_L", "Diesel_L", "Car_Fuel_Type_enc"
        )

        val userInputs = mapOf(
            "Electricity_kWh_total" to getFloatFromEditText(editElectricity),
            "LPG_kg" to getFloatFromEditText(editLpg),
            "Petrol_L" to getFloatFromEditText(editPetrol),
            "Diesel_L" to getFloatFromEditText(editDiesel),
            "PNG_scm" to getFloatFromEditText(editPng),
            "Year" to 2024.0f, // Hardcoded to match your test case
            "Month" to 5.0f      // Hardcoded to match your test case (May)
        )

        val inputArray = FloatArray(featureOrder.size)
        featureOrder.forEachIndexed { index, featureName ->
            val value = when {
                userInputs.containsKey(featureName) -> userInputs[featureName]!!
                fixed_vals.containsKey(featureName) -> fixed_vals[featureName]!!
                else -> 0.0f
            }
            inputArray[index] = value
        }
        return inputArray
    }

    private fun runPrediction(inputArray: FloatArray): Float {
        val inputBuffer = ByteBuffer.allocateDirect(inputArray.size * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputArray.forEach { inputBuffer.putFloat(it) }

        val outputBuffer = ByteBuffer.allocateDirect(1 * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        tflite_current.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        return outputBuffer.float
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tflite_current.isInitialized) {
            tflite_current.close()
        }
    }
}
