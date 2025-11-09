package com.example.carbontracer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.model.Appliance
import com.example.carbontracer.ui.ApplianceAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ElectricityActivity : AppCompatActivity() {

    private lateinit var etElectricityUsage: TextInputEditText
    private lateinit var btnSaveElectricity: Button
    private lateinit var fabAddAppliance: FloatingActionButton
    private lateinit var rvAppliances: RecyclerView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val badgeManager = BadgeManager()
    private val appliances = mutableListOf<Appliance>()
    private lateinit var applianceAdapter: ApplianceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electricity)

        etElectricityUsage = findViewById(R.id.etElectricityUsage)
        btnSaveElectricity = findViewById(R.id.btnSaveElectricity)
        fabAddAppliance = findViewById(R.id.fabAddAppliance)
        rvAppliances = findViewById(R.id.rvAppliances)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()

        btnSaveElectricity.setOnClickListener {
            saveElectricityUsage()
        }

        fabAddAppliance.setOnClickListener {
            showAddApplianceDialog()
        }
    }

    private fun setupRecyclerView() {
        applianceAdapter = ApplianceAdapter(appliances)
        rvAppliances.adapter = applianceAdapter
        rvAppliances.layoutManager = LinearLayoutManager(this)
    }

    private fun showAddApplianceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Appliance")

        val view = layoutInflater.inflate(R.layout.dialog_add_appliance, null)
        val etApplianceName = view.findViewById<TextInputEditText>(R.id.etApplianceName)
        val etApplianceModel = view.findViewById<TextInputEditText>(R.id.etApplianceModel)

        builder.setView(view)
        builder.setPositiveButton("Add") { dialog, _ ->
            val name = etApplianceName.text.toString().trim()
            val model = etApplianceModel.text.toString().trim()

            if (name.isNotEmpty() && model.isNotEmpty()) {
                val appliance = Appliance(name = name, model = model, powerConsumption = 0.0)
                appliances.add(appliance)
                applianceAdapter.notifyItemInserted(appliances.size - 1)
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
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
