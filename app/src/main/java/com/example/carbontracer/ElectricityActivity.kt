package com.example.carbontracer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ElectricityActivity : AppCompatActivity() {

    private lateinit var etElectricityUsage: TextInputEditText
    private lateinit var btnSaveElectricity: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val badgeManager = BadgeManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electricity)

        etElectricityUsage = findViewById(R.id.etElectricityUsage)
        btnSaveElectricity = findViewById(R.id.btnSaveElectricity)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnSaveElectricity.setOnClickListener {
            saveElectricityUsage()
        }
    }

    private fun saveElectricityUsage() {
        val usage = etElectricityUsage.text.toString().trim()
        if (usage.isNotEmpty()) {
            val usageValue = usage.toDoubleOrNull()
            if (usageValue != null) {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val carbonEmissions = CarbonCalculator.calculateElectricityEmissions(usageValue)
                    val emission = hashMapOf(
                        "userId" to currentUser.uid,
                        "type" to "electricity",
                        "amount" to usageValue,
                        "carbon_emissions" to carbonEmissions,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("emissions")
                        .add(emission)
                        .addOnSuccessListener { 
                            badgeManager.checkAndAwardDataBadges()
                            Toast.makeText(this, "Electricity usage saved.", Toast.LENGTH_SHORT).show()
                            finish()
                         }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                etElectricityUsage.error = "Please enter a valid number."
            }
        } else {
            etElectricityUsage.error = "Please enter your electricity usage."
        }
    }
}