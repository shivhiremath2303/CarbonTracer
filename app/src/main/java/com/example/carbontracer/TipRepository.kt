package com.example.carbontracer

import com.example.carbontracer.model.Tip

object TipRepository {

    fun getAllTips(): List<Tip> {
        return listOf(
            // === 1. PREDICTION Model Tips ===
            Tip(
                id = "p1",
                title = "Prediction: You're a High Energy User",
                description = "Our model predicts your overall usage is high. See the suggestion below for where to start!",
                category = "Prediction",
                trigger = "high_user" // <-- Prediction model output
            ),
            Tip(
                id = "p2",
                title = "Prediction: Great Job!",
                description = "Our model predicts your energy usage is low and efficient. Keep up the good work!",
                category = "Prediction",
                trigger = "low_user" // <-- Prediction model output
            ),

            // === 2. SUGGESTION Model Tips ===
            Tip(
                id = "s1",
                title = "Suggestion: Focus on Your AC",
                description = "Our suggestion model found that your Air Conditioner is your biggest energy drain. Set it to 24°C to save.",
                category = "Suggestion",
                trigger = "suggest_ac" // <-- Suggestion model output
            ),
            Tip(
                id = "s2",
                title = "Suggestion: Check Your Fridge",
                description = "Our suggestion model found that your Refrigerator is your top problem. Clean its coils to improve efficiency.",
                category = "Suggestion",
                trigger = "suggest_fridge" // <-- Suggestion model output
            ),

            // === 3. General Tips ===
            Tip(
                id = "g1",
                title = "Switch to LED Bulbs",
                description = "LEDs use up to 85% less energy than incandescent bulbs.",
                category = "General",
                trigger = "general"
            )
        )
    }
}