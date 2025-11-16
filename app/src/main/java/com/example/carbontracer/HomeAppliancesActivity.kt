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
import com.example.carbontracer.adapter.ApplianceAdapter
import com.example.carbontracer.model.Appliance
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_appliances)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbarHomeAppliances)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvAppliances = findViewById(R.id.rvAppliances)
        fabAddAppliance = findViewById(R.id.fabAddAppliance)
        tvNoAppliances = findViewById(R.id.tvNoAppliances)

        setupRecyclerView()
        loadAppliances()

        fabAddAppliance.setOnClickListener {
            showAddApplianceDialog()
        }
    }

    private fun setupRecyclerView() {
        applianceAdapter = ApplianceAdapter(emptyList(), {
            showEditApplianceDialog(it)
        }, {
            showDeleteConfirmationDialog(it)
        })
        rvAppliances.layoutManager = LinearLayoutManager(this)
        rvAppliances.adapter = applianceAdapter
    }

    private fun loadAppliances() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("appliances")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading appliances", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val updatedList = snapshots.toObjects(Appliance::class.java)
                    applianceAdapter.updateList(updatedList)
                    tvNoAppliances.visibility = if (updatedList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun showAddApplianceDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_appliance, null)

        val spinnerApplianceName = dialogView.findViewById<Spinner>(R.id.spinnerApplianceName)
        val etApplianceCount = dialogView.findViewById<EditText>(R.id.etApplianceCount)
        val etWattage = dialogView.findViewById<EditText>(R.id.etWattage)
        val etDailyHours = dialogView.findViewById<EditText>(R.id.etDailyHours)
        val etDiesel = dialogView.findViewById<EditText>(R.id.etDiesel)

        val applianceNames = arrayOf("Refrigerator", "LED TV", "Air Conditioner", "Ceiling Fan", "Laptop", "Light Bulb (LED)", "Washing Machine", "Microwave Oven", "Water Heater (Geyser)", "Diesel Generator")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, applianceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerApplianceName.adapter = adapter

        MaterialAlertDialogBuilder(this)
            .setTitle("Add New Appliance")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { dialog, _ ->
                val name = spinnerApplianceName.selectedItem.toString()
                val count = etApplianceCount.text.toString().toIntOrNull() ?: 0
                val wattage = etWattage.text.toString().toIntOrNull() ?: 0
                val hours = etDailyHours.text.toString().toDoubleOrNull() ?: 0.0
                val diesel = etDiesel.text.toString().toDoubleOrNull() ?: 0.0

                if (count > 0 && (wattage >= 0 || diesel >= 0)) {
                    saveApplianceToFirestore(null, name, count, wattage, hours, diesel)
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showEditApplianceDialog(appliance: Appliance) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_appliance, null)

        val spinnerApplianceName = dialogView.findViewById<Spinner>(R.id.spinnerApplianceName)
        val etApplianceCount = dialogView.findViewById<EditText>(R.id.etApplianceCount)
        val etWattage = dialogView.findViewById<EditText>(R.id.etWattage)
        val etDailyHours = dialogView.findViewById<EditText>(R.id.etDailyHours)
        val etDiesel = dialogView.findViewById<EditText>(R.id.etDiesel)

        val applianceNames = arrayOf("Refrigerator", "LED TV", "Air Conditioner", "Ceiling Fan", "Laptop", "Light Bulb (LED)", "Washing Machine", "Microwave Oven", "Water Heater (Geyser)", "Diesel Generator")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, applianceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerApplianceName.adapter = adapter

        spinnerApplianceName.setSelection(adapter.getPosition(appliance.applianceName))
        etApplianceCount.setText(appliance.applianceCount.toString())
        etWattage.setText(appliance.wattageUsed.toString())
        etDailyHours.setText(appliance.dailyHoursUsed.toString())
        etDiesel.setText(appliance.dieselLiters.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Appliance")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { dialog, _ ->
                val name = spinnerApplianceName.selectedItem.toString()
                val count = etApplianceCount.text.toString().toIntOrNull() ?: 0
                val wattage = etWattage.text.toString().toIntOrNull() ?: 0
                val hours = etDailyHours.text.toString().toDoubleOrNull() ?: 0.0
                val diesel = etDiesel.text.toString().toDoubleOrNull() ?: 0.0

                if (count > 0 && (wattage >= 0 || diesel >= 0)) {
                    saveApplianceToFirestore(appliance.id, name, count, wattage, hours, diesel)
                } else {
                    Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(appliance: Appliance) {
        AlertDialog.Builder(this)
            .setTitle("Delete Appliance")
            .setMessage("Are you sure you want to delete this appliance?")
            .setPositiveButton("Delete") { _, _ -> deleteAppliance(appliance) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveApplianceToFirestore(id: String?, name: String, count: Int, wattage: Int, hours: Double, diesel: Double) {
        val userId = auth.currentUser?.uid ?: return

        val appliance = Appliance(
            id = id ?: "",
            userId = userId,
            applianceName = name,
            applianceCount = count,
            wattageUsed = wattage,
            dailyHoursUsed = hours,
            dieselLiters = diesel
        )

        val collection = db.collection("users").document(userId).collection("appliances")

        if (id == null) {
            collection.add(appliance)
                .addOnSuccessListener {
                    Toast.makeText(this, "Appliance added", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            collection.document(id).set(appliance)
                .addOnSuccessListener {
                    Toast.makeText(this, "Appliance updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteAppliance(appliance: Appliance) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("appliances").document(appliance.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Appliance deleted", Toast.LENGTH_SHORT).show()
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