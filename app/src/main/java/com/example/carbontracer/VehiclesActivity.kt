package com.example.carbontracer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carbontracer.model.Vehicle
import com.example.carbontracer.ui.VehicleAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VehiclesActivity : AppCompatActivity() {

    private lateinit var rvVehicles: RecyclerView
    private lateinit var fabAddVehicle: FloatingActionButton
    private lateinit var tvNoVehicles: TextView
    private lateinit var vehicleAdapter: VehicleAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val vehicleList = mutableListOf<Vehicle>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_info)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbarVehicleInfo)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvVehicles = findViewById(R.id.rvVehicles)
        fabAddVehicle = findViewById(R.id.fabAddVehicle)
        tvNoVehicles = findViewById(R.id.tvNoVehicles)

        setupRecyclerView()
        loadVehicles()

        fabAddVehicle.setOnClickListener {
            showAddVehicleDialog()
        }
    }

    private fun setupRecyclerView() {
        vehicleAdapter = VehicleAdapter(vehicleList, {
            showEditVehicleDialog(it)
        }, {
            showDeleteConfirmationDialog(it)
        })
        rvVehicles.layoutManager = LinearLayoutManager(this)
        rvVehicles.adapter = vehicleAdapter
    }

    private fun loadVehicles() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("vehicles")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading vehicles", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val updatedList = snapshots.toObjects(Vehicle::class.java)
                    vehicleAdapter.updateList(updatedList)
                    tvNoVehicles.visibility = if (updatedList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun showAddVehicleDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_vehicle, null)

        val etNickname = dialogView.findViewById<EditText>(R.id.etVehicleNickname)
        val spinnerVehicleType = dialogView.findViewById<Spinner>(R.id.spinnerVehicleType)
        val spinnerFuelType = dialogView.findViewById<Spinner>(R.id.spinnerFuelType)
        val etEfficiency = dialogView.findViewById<EditText>(R.id.etVehicleEfficiency)
        val spinnerEfficiencyUnit = dialogView.findViewById<Spinner>(R.id.spinnerEfficiencyUnit)
        val etDiesel = dialogView.findViewById<EditText>(R.id.etDiesel)

        setupSpinner(spinnerVehicleType, R.array.vehicle_types)
        setupSpinner(spinnerFuelType, R.array.fuel_types)
        setupSpinner(spinnerEfficiencyUnit, R.array.efficiency_units)

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Vehicle")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val nickname = etNickname.text.toString()
                val vehicleType = spinnerVehicleType.selectedItem.toString()
                val fuelType = spinnerFuelType.selectedItem.toString()
                val efficiency = etEfficiency.text.toString().toDoubleOrNull() ?: 0.0
                val efficiencyUnit = spinnerEfficiencyUnit.selectedItem.toString()
                val diesel = etDiesel.text.toString().toDoubleOrNull() ?: 0.0


                if (nickname.isNotBlank()) {
                    saveVehicleToFirestore(null, nickname, vehicleType, fuelType, efficiency, efficiencyUnit, diesel)
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showEditVehicleDialog(vehicle: Vehicle) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_vehicle, null)

        val etNickname = dialogView.findViewById<EditText>(R.id.etVehicleNickname)
        val spinnerVehicleType = dialogView.findViewById<Spinner>(R.id.spinnerVehicleType)
        val spinnerFuelType = dialogView.findViewById<Spinner>(R.id.spinnerFuelType)
        val etEfficiency = dialogView.findViewById<EditText>(R.id.etVehicleEfficiency)
        val spinnerEfficiencyUnit = dialogView.findViewById<Spinner>(R.id.spinnerEfficiencyUnit)
        val etDiesel = dialogView.findViewById<EditText>(R.id.etDiesel)

        setupSpinner(spinnerVehicleType, R.array.vehicle_types)
        setupSpinner(spinnerFuelType, R.array.fuel_types)
        setupSpinner(spinnerEfficiencyUnit, R.array.efficiency_units)

        etNickname.setText(vehicle.nickname)
        etEfficiency.setText(vehicle.efficiency.toString())
        etDiesel.setText(vehicle.dieselLiters.toString())

        (spinnerVehicleType.adapter as? ArrayAdapter<String>)?.let {
            spinnerVehicleType.setSelection(it.getPosition(vehicle.vehicleType))
        }
        (spinnerFuelType.adapter as? ArrayAdapter<String>)?.let {
            spinnerFuelType.setSelection(it.getPosition(vehicle.fuelType))
        }
        (spinnerEfficiencyUnit.adapter as? ArrayAdapter<String>)?.let {
            spinnerEfficiencyUnit.setSelection(it.getPosition(vehicle.efficiencyUnit))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Vehicle")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val nickname = etNickname.text.toString()
                val vehicleType = spinnerVehicleType.selectedItem.toString()
                val fuelType = spinnerFuelType.selectedItem.toString()
                val efficiency = etEfficiency.text.toString().toDoubleOrNull() ?: 0.0
                val efficiencyUnit = spinnerEfficiencyUnit.selectedItem.toString()
                val diesel = etDiesel.text.toString().toDoubleOrNull() ?: 0.0

                if (nickname.isNotBlank()) {
                    saveVehicleToFirestore(vehicle.id, nickname, vehicleType, fuelType, efficiency, efficiencyUnit, diesel)
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(vehicle: Vehicle) {
        AlertDialog.Builder(this)
            .setTitle("Delete Vehicle")
            .setMessage("Are you sure you want to delete this vehicle?")
            .setPositiveButton("Delete") { _, _ -> deleteVehicle(vehicle) }
            .setNegativeButton("Cancel", null)
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

    private fun saveVehicleToFirestore(id: String?, nickname: String, vehicleType: String, fuelType: String, efficiency: Double, unit: String, diesel: Double) {
        val userId = auth.currentUser?.uid ?: return

        val vehicle = Vehicle(
            id = id ?: "",
            userId = userId,
            nickname = nickname,
            vehicleType = vehicleType,
            fuelType = fuelType,
            efficiency = efficiency,
            efficiencyUnit = unit,
            dieselLiters = diesel
        )

        val collection = db.collection("users").document(userId).collection("vehicles")
        val task = if (id == null) {
            collection.add(vehicle)
        } else {
            collection.document(id).set(vehicle)
        }

        task.addOnSuccessListener {
            val message = if (id == null) "Vehicle added" else "Vehicle updated"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteVehicle(vehicle: Vehicle) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("vehicles").document(vehicle.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Vehicle deleted", Toast.LENGTH_SHORT).show()
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
