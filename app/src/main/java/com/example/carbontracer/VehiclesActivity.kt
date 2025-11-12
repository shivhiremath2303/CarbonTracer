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
import com.example.carbontracer.adapter.VehicleAdapter // You will need this adapter
import com.example.carbontracer.model.Vehicle // You will need this data model
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VehicleInfoActivity : AppCompatActivity() {

    private lateinit var rvVehicles: RecyclerView
    private lateinit var fabAddVehicle: FloatingActionButton
    private lateinit var tvNoVehicles: TextView
    private lateinit var vehicleAdapter: VehicleAdapter // This is a separate file

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val vehicleList = mutableListOf<Vehicle>() // This is a separate file

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This relies on activity_vehicle_info.xml
        setContentView(R.layout.activity_vehicle_info)

        // Setup Toolbar
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbarVehicleInfo)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow

        // Init Views
        rvVehicles = findViewById(R.id.rvVehicles)
        fabAddVehicle = findViewById(R.id.fabAddVehicle)
        tvNoVehicles = findViewById(R.id.tvNoVehicles)

        // Setup RecyclerView
        setupRecyclerView()

        // Load data from Firestore
        loadVehicles()

        // Set Listeners
        fabAddVehicle.setOnClickListener {
            showAddVehicleDialog()
        }
    }

    private fun setupRecyclerView() {
        // Requires VehicleAdapter.kt
        vehicleAdapter = VehicleAdapter(vehicleList)
        rvVehicles.layoutManager = LinearLayoutManager(this)
        rvVehicles.adapter = vehicleAdapter
    }

    private fun loadVehicles() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("user_vehicles")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading vehicles", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    vehicleList.clear()
                    // Requires Vehicle.kt data class
                    vehicleList.addAll(snapshots.toObjects(Vehicle::class.java))
                    vehicleAdapter.updateList(vehicleList)

                    // Show/hide the "No vehicles" message
                    tvNoVehicles.visibility = if (vehicleList.isEmpty()) TextView.VISIBLE else TextView.GONE
                }
            }
    }

    private fun showAddVehicleDialog() {
        // This relies on dialog_add_vehicle.xml
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_vehicle, null)

        // Find views in the dialog layout
        val etNickname = dialogView.findViewById<EditText>(R.id.etVehicleNickname)
        val spinnerVehicleType = dialogView.findViewById<Spinner>(R.id.spinnerVehicleType)
        val spinnerFuelType = dialogView.findViewById<Spinner>(R.id.spinnerFuelType)
        val etEfficiency = dialogView.findViewById<EditText>(R.id.etVehicleEfficiency)
        val spinnerEfficiencyUnit = dialogView.findViewById<Spinner>(R.id.spinnerEfficiencyUnit)

        // These rely on arrays in strings.xml
        setupSpinner(spinnerVehicleType, R.array.vehicle_types)
        setupSpinner(spinnerFuelType, R.array.fuel_types)
        setupSpinner(spinnerEfficiencyUnit, R.array.efficiency_units)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Vehicle")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { dialog, _ ->
                // Get data from dialog
                val nickname = etNickname.text.toString()
                val vehicleType = spinnerVehicleType.selectedItem.toString()
                val fuelType = spinnerFuelType.selectedItem.toString()
                val efficiency = etEfficiency.text.toString().toDoubleOrNull() ?: 0.0
                val efficiencyUnit = spinnerEfficiencyUnit.selectedItem.toString()

                if (nickname.isNotBlank() && efficiency > 0) {
                    saveVehicleToFirestore(nickname, vehicleType, fuelType, efficiency, efficiencyUnit)
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun setupSpinner(spinner: Spinner, arrayResourceId: Int) {
        ArrayAdapter.createFromResource(
            this,
            arrayResourceId,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
    }

    private fun saveVehicleToFirestore(nickname: String, vehicleType: String, fuelType: String, efficiency: Double, unit: String) {
        val userId = auth.currentUser?.uid ?: return

        // Requires Vehicle.kt data class
        val vehicle = Vehicle(
            userId = userId,
            nickname = nickname,
            vehicleType = vehicleType,
            fuelType = fuelType,
            efficiency = efficiency,
            efficiencyUnit = unit
        )

        db.collection("user_vehicles")
            .add(vehicle)
            .addOnSuccessListener {
                Toast.makeText(this, "Vehicle added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Handle the back arrow in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}