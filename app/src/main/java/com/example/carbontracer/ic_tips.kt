package com.example.carbontracer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.carbontracer.databinding.ActivityIcTipsBinding
import com.example.carbontracer.ml.KerasModel
import com.example.carbontracer.ml.LstmModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar

data class MonthlyEmission(val year: Int, val month: Int, val totalCO2: Double)

class ic_tips : AppCompatActivity() {

    private lateinit var binding: ActivityIcTipsBinding

    // CORRECT Scaling factors from the Python script
    private val DATA_MIN = floatArrayOf(
        2023.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -5.0f, 0.0f
    )
    private val DATA_MAX = floatArrayOf(
        2023.0f, 12.0f, 1303.0f, 14.2f, 121.22f, 128.56f, 34.09f, 5.0f, 1.0f, 1.0f, 2000.0f, 2.0f, 1.0f, 350.0f, 461.0f, 16.36f, 1157.0f, 128.56f, 2.0f, 12.0f, 763.0f, 792.0f
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIcTipsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonPredict.setOnClickListener {
            predictAndSuggest()
        }
    }

    private fun predictAndSuggest() {
        val year = binding.editTextYear.text.toString().toIntOrNull()
        val month = binding.editTextMonth.text.toString().toIntOrNull()

        if (year == null || month == null) {
            Toast.makeText(this, "Please enter a valid year and month", Toast.LENGTH_SHORT).show()
            return
        }

        val electricity = binding.editTextElectricity.text.toString().toFloatOrNull() ?: 0.0f
        val lpg = binding.editTextLpg.text.toString().toFloatOrNull() ?: 0.0f
        val petrol = binding.editTextPetrol.text.toString().toFloatOrNull() ?: 0.0f
        val diesel = binding.editTextDiesel.text.toString().toFloatOrNull() ?: 0.0f
        val png = binding.editTextPng.text.toString().toFloatOrNull() ?: 0.0f

        val currentPrediction = predictCurrentEmission(year, month, electricity, lpg, petrol, diesel, png)

        val (emissionSeries, min, max) = readAndProcessPredictionData()
        if (emissionSeries.isEmpty()) {
            Toast.makeText(this, "Could not read prediction data. Make sure prediction_data.csv is in your assets folder.", Toast.LENGTH_LONG).show()
            return
        }
        val scaler = MinMaxScaler(min, max)
        val scaledEmissionSeries = emissionSeries.map { scaler.transform(it) }.toMutableList()

        val nSteps = 3
        if (scaledEmissionSeries.size < nSteps) {
            Toast.makeText(this, "Not enough data to make a prediction.", Toast.LENGTH_LONG).show()
            return
        }
        val lastKnownEmissions = scaledEmissionSeries.takeLast(nSteps).toMutableList()

        val currentPredScaled = scaler.transform(currentPrediction.toDouble())
        lastKnownEmissions.removeAt(0)
        lastKnownEmissions.add(currentPredScaled)

        val futurePredictions = predictFutureEmissions(lastKnownEmissions, scaler, nSteps)

        displayPredictions(currentPrediction, futurePredictions, month, year)

        val suggestions = generateSuggestions(electricity, lpg, petrol, diesel, png)
        displaySuggestions(suggestions)
    }

    private fun scaleFeatures(features: FloatArray): FloatArray {
        val scaledFeatures = FloatArray(features.size)
        for (i in features.indices) {
            val min = DATA_MIN[i]
            val max = DATA_MAX[i]
            val range = max - min
            if (range != 0.0f) {
                scaledFeatures[i] = (features[i] - min) / range
            } else {
                scaledFeatures[i] = 0.0f
            }
        }
        return scaledFeatures
    }

    private fun predictCurrentEmission(
        year: Int,
        month: Int,
        electricity: Float,
        lpg: Float,
        petrol: Float,
        diesel: Float,
        png: Float
    ): Float {
        try {
            val model = KerasModel.newInstance(this)

            val rawInputFeatures = floatArrayOf(
                year.toFloat(),
                month.toFloat(),
                electricity,
                lpg,
                petrol,
                diesel,
                png,
                3.0f, // Household_Size
                1.0f, // Urban
                1.0f, // Has_Car
                1200f, // Car_CC
                0.0f, // Car_Fuel_Type_enc
                1.0f, // Has_Bike
                150f, // Bike_CC
                300f, // Bike_Monthly_km
                10f, // Bike_Fuel_L
                500f, // Car_Monthly_km
                20f, // Car_Fuel_L
                1.5f, // AC_Tonnage
                4f, // AC_Hours_per_day
                50f, // Electricity_kWh_other
                100f // Electricity_kWh_AC
            )

            val scaledInputFeatures = scaleFeatures(rawInputFeatures)

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, scaledInputFeatures.size), DataType.FLOAT32)
            val byteBuffer = ByteBuffer.allocateDirect(scaledInputFeatures.size * 4)
            byteBuffer.order(ByteOrder.nativeOrder())
            for (value in scaledInputFeatures) {
                byteBuffer.putFloat(value)
            }
            inputFeature0.loadBuffer(byteBuffer)

            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val prediction = outputFeature0.getFloatValue(0)

            model.close()

            return prediction

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error making current prediction. Make sure KerasModel.tflite is in your ml folder.", Toast.LENGTH_LONG).show()
            return 0.0f
        }
    }

    private fun readAndProcessPredictionData(): Triple<List<Double>, Double, Double> {
        try {
            val inputStream = assets.open("prediction_data.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Skip header

            val emissions = mutableListOf<MonthlyEmission>()
            reader.forEachLine {
                val tokens = it.split(",")
                if (tokens.size > 2) {
                    try {
                        val emission = MonthlyEmission(
                            year = tokens[0].toInt(),
                            month = tokens[1].toInt(),
                            totalCO2 = tokens[2].toDouble()
                        )
                        emissions.add(emission)
                    } catch (e: NumberFormatException) {
                        // Ignore malformed lines
                    }
                }
            }

            val monthlyEmissions = emissions.groupBy { it.year to it.month }
                .mapValues { (_, values) -> values.map { it.totalCO2 }.average() }
                .entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))
                .map { it.value }

            val min = monthlyEmissions.minOrNull() ?: 0.0
            val max = monthlyEmissions.maxOrNull() ?: 0.0

            return Triple(monthlyEmissions, min, max)
        } catch (e: Exception) {
            e.printStackTrace()
            return Triple(emptyList(), 0.0, 0.0)
        }
    }

    private fun predictFutureEmissions(lastKnownEmissions: List<Double>, scaler: MinMaxScaler, nSteps: Int): List<Float> {
        try {
            val model = LstmModel.newInstance(this)
            val futurePredictions = mutableListOf<Float>()
            val inputBuffer = lastKnownEmissions.toMutableList()

            for (i in 0 until 6) {
                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, nSteps, 1), DataType.FLOAT32)
                val byteBuffer = ByteBuffer.allocateDirect(nSteps * 4)
                byteBuffer.order(ByteOrder.nativeOrder())
                for (value in inputBuffer) {
                    byteBuffer.putFloat(value.toFloat())
                }
                byteBuffer.flip()
                inputFeature0.loadBuffer(byteBuffer)

                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer

                val prediction = outputFeature0.getFloatValue(0)
                futurePredictions.add(scaler.inverseTransform(prediction.toDouble()).toFloat())

                inputBuffer.removeAt(0)
                inputBuffer.add(prediction.toDouble())
            }

            model.close()
            return futurePredictions
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error making future predictions. Make sure LstmModel.tflite is in your ml folder.", Toast.LENGTH_LONG).show()
            return emptyList()
        }
    }

    private fun displayPredictions(currentPrediction: Float, futurePredictions: List<Float>, startMonth: Int, startYear: Int) {
        binding.cardPredictions.visibility = View.VISIBLE

        val calendar = Calendar.getInstance()
        calendar.set(startYear, startMonth - 1, 1)
        val currentMonthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, resources.configuration.locales[0])
        binding.textViewCurrentPrediction.text = "Predicted Emission for $currentMonthName $startYear: ${String.format("%.2f", currentPrediction)} kg CO2"

        val futurePredictionsText = StringBuilder("Predicted Emissions for next 6 months:\n")
        futurePredictions.forEachIndexed { index, prediction ->
            calendar.add(Calendar.MONTH, 1)
            val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, resources.configuration.locales[0])
            val year = calendar.get(Calendar.YEAR)
            futurePredictionsText.append("- $monthName $year: ${String.format("%.2f", prediction)} kg CO2\n")
        }
        binding.textViewFuturePredictions.text = futurePredictionsText.toString()
    }

    private fun generateSuggestions(electricity: Float, lpg: Float, petrol: Float, diesel: Float, png: Float): List<String> {
        val suggestions = mutableListOf<String>()
        suggestions.add("Consider adopting energy-saving habits and optimizing appliance usage to reduce emissions.")

        if (electricity > 600) {
            suggestions.add("High electricity use detected; consider efficient appliances and reduced usage.")
        }
        if (lpg > 10) {
            suggestions.add("Optimize LPG usage by efficient cooking or reducing cylinders.")
        }
        if (petrol > 20) {
            suggestions.add("Reduce petrol use; consider efficient vehicles or carpooling.")
        }
        if (diesel > 20) {
            suggestions.add("Reduce diesel use; consider alternatives or low emission vehicles.")
        }
        if (png > 20) {
            suggestions.add("Optimize PNG vehicle usage.")
        }

        return suggestions
    }

    private fun displaySuggestions(suggestions: List<String>) {
        binding.cardSuggestions.visibility = View.VISIBLE
        val suggestionsText = StringBuilder()
        suggestions.forEach {
            suggestionsText.append("- $it\n")
        }
        binding.textViewSuggestions.text = suggestionsText.toString()
    }

    class MinMaxScaler(private val min: Double, private val max: Double) {
        fun transform(value: Double): Double {
            if (max - min == 0.0) return 0.0
            return (value - min) / (max - min)
        }

        fun inverseTransform(value: Double): Double {
            return value * (max - min) + min
        }
    }
}