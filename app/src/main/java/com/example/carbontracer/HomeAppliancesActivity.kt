package com.example.carbontracer


import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.adapter.ApplianceAdapter // You will need to create this
import com.example.carbontracer.model.Appliance // You will need to create this
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeAppliancesActivity : AppCompatActivity() {

    private lateinit var rvAppliances: RecyclerView
    private lateinit var fabAddAppliance: FloatingActionButton
    private lateinit var tvNoAppliances: TextView
    private lateinit var applianceAdapter: ApplianceAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val applianceList = mutableListOf<Appliance>()

    // Your database of default wattages
    private val applianceWattageMap = mapOf(
        "Refrigerator" to 200,
        "LED TV" to 80,
        "Air Conditioner" to 1500,
        "Ceiling Fan" to 75,
        "Laptop" to 60,
        "Light Bulb (LED)" to 10,
        "Washing Machine" to 500,
        "Microwave Oven" to 1000,
        "Water Heater (Geyser)" to 2000
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_appliances)

        // Setup Toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbarHomeAppliances)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow

        // Init Views
        rvAppliances = findViewById(R.id.rvAppliances)
        fabAddAppliance = findViewById(R.id.fabAddAppliance)
        tvNoAppliances = findViewById(R.id.tvNoAppliances)

        // Setup RecyclerView
        setupRecyclerView()

        // Load data from Firestore
        loadAppliances()

        // Set Listeners
        fabAddAppliance.setOnClickListener {
            showAddApplianceDialog()
        }
    }

    private fun setupRecyclerView() {
        applianceAdapter = ApplianceAdapter(applianceList)
        rvAppliances.layoutManager = LinearLayoutManager(this)
        rvAppliances.adapter = applianceAdapter
    }

    private fun loadAppliances() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("user_appliances")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading appliances", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    applianceList.clear()
                    applianceList.addAll(snapshots.toObjects(Appliance::class.java))
                    applianceAdapter.updateList(applianceList)

                    tvNoAppliances.visibility = if (applianceList.isEmpty()) TextView.VISIBLE else TextView.GONE
                }
            }
    }

    private fun showAddApplianceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_appliance, null)

        val spinnerApplianceName = dialogView.findViewById<Spinner>(R.id.spinnerApplianceName)
        val etApplianceCount = dialogView.findViewById<EditText>(R.id.etApplianceCount)
        val etDailyHours = dialogView.findViewById<EditText>(R.id.etDailyHours)

        // Setup appliance name spinner
        val applianceNames = applianceWattageMap.keys.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, applianceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerApplianceName.adapter = adapter

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Appliance")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { dialog, _ ->
                val name = spinnerApplianceName.selectedItem.toString()
                val count = etApplianceCount.text.toString().toIntOrNull() ?: 1
                val hours = etDailyHours.text.toString().toDoubleOrNull() ?: 0.0

                if (hours > 0) {
                    saveApplianceToFirestore(name, count, hours)
                } else {
                    Toast.makeText(this, "Please enter valid hours", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun saveApplianceToFirestore(name: String, count: Int, hours: Double) {
        val userId = auth.currentUser?.uid ?: return

        // Get the wattage from your map
        val wattage = applianceWattageMap[name] ?: 0

        val appliance = Appliance(
            userId = userId,
            applianceName = name,
            applianceCount = count,
            wattageUsed = wattage,
            dailyHoursUsed = hours
        )

        db.collection("user_appliances")
            .add(appliance)
            .addOnSuccessListener {
                Toast.makeText(this, "Appliance added", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}