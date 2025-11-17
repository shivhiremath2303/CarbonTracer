package com.example.carbontracer

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmissionPredictor(context: Context) {

    private val interpreter: Interpreter

    init {
        val assetManager = context.assets
        val modelData = assetManager.open("emission_predictor.tflite").readBytes()
        val bb = ByteBuffer.allocateDirect(modelData.size)
        bb.order(ByteOrder.nativeOrder())
        bb.put(modelData)
        bb.rewind()
        interpreter = Interpreter(bb)
    }

    /**
     * Predicts current month CO2 emission (kg) given raw inputs.
     * Order must match Python feature_cols:
     * [Electricity_kWh_total, LPG_kg, Petrol_L, Diesel_L]
     */
    fun predict(
        electricityKwh: Double,
        lpgKg: Double,
        petrolLiters: Double,
        dieselLiters: Double
    ): Float {
        val input = floatArrayOf(
            electricityKwh.toFloat(),
            lpgKg.toFloat(),
            petrolLiters.toFloat(),
            dieselLiters.toFloat()
        )

        val inputBuffer = Array(1) { input }           // shape [1,4]
        val outputBuffer = Array(1) { FloatArray(1) }  // shape [1,1]

        interpreter.run(inputBuffer, outputBuffer)

        return outputBuffer[0][0]  // kg CO2
    }
}
