package com.example.carbontracer

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class EmissionForecaster(context: Context) {

    private val interpreter: Interpreter
    private val nSteps = 3  // same as in Python create_sequences(n_steps=3)

    // last_sequence_scaled = [0.030526307665977193, 0.0, 0.03514208078315839]
    // Converted to floats for Kotlin:
    private val baseLastSequence = floatArrayOf(
        0.030526308f,
        0.0f,
        0.03514208f
    )

    init {
        val assetManager = context.assets
        val modelData = assetManager.open("emission_lstm_forecast.tflite").readBytes()
        val bb = ByteBuffer.allocateDirect(modelData.size)
        bb.order(ByteOrder.nativeOrder())
        bb.put(modelData)
        bb.rewind()
        interpreter = Interpreter(bb)
    }

    /**
     * Forecast next 6 months using LSTM TF-Lite model.
     *
     * @param currentEmission current month emission (unscaled, kg CO2)
     * @param scalerMin emission_scaler.data_min_ from Python
     * @param scalerMax emission_scaler.data_max_ from Python
     */
    fun forecastNextSixMonths(
        currentEmission: Float,
        scalerMin: Float,
        scalerMax: Float
    ): List<Float> {
        val scale = scalerMax - scalerMin
        val currentScaled = if (scale == 0f) 0f else (currentEmission - scalerMin) / scale

        // Start from last sequence from training, replace last value with currentScaled
        val seq = baseLastSequence.clone()
        seq[seq.size - 1] = currentScaled

        val results = mutableListOf<Float>()
        var inputSeq = seq

        repeat(6) {
            // Build input shape [1, nSteps, 1]
            val inputArray = Array(1) {
                Array(nSteps) { i ->
                    floatArrayOf(inputSeq[i])
                }
            }

            val outputArray = Array(1) { FloatArray(1) }
            interpreter.run(inputArray, outputArray)

            val nextScaled = outputArray[0][0]
            val nextUnscaled = unscale(nextScaled, scalerMin, scalerMax)
            results.add(nextUnscaled)

            // Shift window: drop first, append new
            inputSeq = floatArrayOf(
                inputSeq[1],
                inputSeq[2],
                nextScaled
            )
        }

        return results
    }

    private fun unscale(scaled: Float, dataMin: Float, dataMax: Float): Float {
        return scaled * (dataMax - dataMin) + dataMin
    }
}
