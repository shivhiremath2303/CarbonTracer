package com.example.carbontracer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TransportActivity : AppCompatActivity() {

    private lateinit var spinnerTransportType: Spinner
    private lateinit var etDistance: TextInputEditText
    private lateinit var btnSaveTransport: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val badgeManager = BadgeManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transport)

        spinnerTransportType = findViewById(R.id.spinnerTransportType)
        etDistance = findViewById(R.id.etDistance)
        btnSaveTransport = findViewById(R.id.btnSaveTransport)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val transportTypes = arrayOf("Car", "Bus", "Train", "Walk/Cycle")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, transportTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTransportType.adapter = adapter

        btnSaveTransport.setOnClickListener {
            saveTransportUsage()
        }
    }

    private fun saveTransportUsage() {
        val transportType = spinnerTransportType.selectedItem.toString()
        val distance = etDistance.text.toString().trim()
        if (distance.isNotEmpty()) {
            val distanceValue = distance.toDoubleOrNull()
            if (distanceValue != null) {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val carbonEmissions = CarbonCalculator.calculateTransportEmissions(transportType, distanceValue)
                    val emission = hashMapOf(
                        "userId" to currentUser.uid,
                        "type" to "transport",
                        "transport_type" to transportType,
                        "distance" to distanceValue,
                        "carbon_emissions" to carbonEmissions,
                        "timestamp" to System.currentTimeMillis()
                    )

                    db.collection("emissions")
                        .add(emission)
                        .addOnSuccessListener { 
                            badgeManager.checkAndAwardDataBadges()
                            Toast.makeText(this, "Transport usage saved.", Toast.LENGTH_SHORT).show()
                            finish()
                         }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                etDistance.error = "Please enter a valid number."
            }
        } else {
            etDistance.error = "Please enter the distance."
        }
    }
}