package com.example.carbontracer // Make sure this matches your package

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
// Import your data model
import com.example.carbontracer.model.Appliance

class AppliancesActivity : AppCompatActivity() { // Renamed from AddApplianceActivity

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser!!.uid

    private lateinit var spinnerName: Spinner
    private lateinit var editHours: EditText
    private lateinit var saveButton: Button

    // Your Internal Wattage Map. This is the "brain"
    private val wattageLookUp = mapOf(
        "Refrigerator" to 180,
        "Television" to 150,
        "Air Conditioner" to 1500,
        "Washing Machine" to 500,
        "Microwave" to 1100,
        "Fan" to 75,
        "Light Bulb (LED)" to 10,
        "Wi-Fi Router" to 6
        // Add more appliances here
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure your layout file is named this:
        setContentView(R.layout.activity_appliances)

        spinnerName = findViewById(R.id.spinnerApplianceName)
        editHours = findViewById(R.id.editDailyHours)
        saveButton = findViewById(R.id.saveButton)

        saveButton.setOnClickListener {
            saveAppliance()
        }
    }

    private fun saveAppliance() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "Error: You are not logged in.", Toast.LENGTH_LONG).show()
            return
            }
        // 1. Get user input from the simple form
        val applianceName = spinnerName.selectedItem.toString()
        val dailyHours = editHours.text.toString().toDoubleOrNull() ?: 0.0

        // 2. Look up the wattage from your internal map
        // This is your "ML Protection" step
        val wattage = wattageLookUp[applianceName] ?: 100 // Fallback to 100W if not found

        // 3. Create the new Appliance object (using your model)
        val newAppliance = Appliance(
            // id is set by @DocumentId automatically
            userId = currentUserId,
            applianceName = applianceName,
            applianceCount = 1, // Default to 1
            wattageUsed = wattage,
            dailyHoursUsed = dailyHours
        )

        // 4. Save to Firebase
        db.collection("users").document(currentUserId)
            .collection("appliances")
            .add(newAppliance) // Firebase will use your data class
            .addOnSuccessListener {
                // 5. RUN GAMIFICATION LOGIC
                runGamificationChecks()

                Toast.makeText(this, "$applianceName added!", Toast.LENGTH_SHORT).show()
                finish() // Go back to ProfileFragment
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- GAMIFICATION CODE ---
    // This logic checks for points, badges, and challenges

    private fun runGamificationChecks() {
        val prefs = getSharedPreferences("GamificationPrefs", MODE_PRIVATE)
        val editor = prefs.edit()

        val currentPoints = prefs.getInt("totalPoints", 0)
        val newPoints = currentPoints + 50 // +50 points per appliance
        editor.putInt("totalPoints", newPoints)

        // Update appliance count
        val applianceCount = prefs.getInt("applianceCount", 0) + 1
        editor.putInt("applianceCount", applianceCount)

        // Check for "First Step" Badge
        val firstBadgeUnlocked = prefs.getBoolean("badge_first_step", false)
        if (!firstBadgeUnlocked) {
            editor.putBoolean("badge_first_step", true)
            editor.putBoolean("challenge_first_appliance_complete", true)
            // Show a popup for the badge
            showBadgePopup("Badge Unlocked: First Step!", "You've added your first appliance! +25 GP Bonus!")
            // Add bonus points
            editor.putInt("totalPoints", newPoints + 25)
        }

        // Check for "Home Starter" Badge
        val starterBadgeUnlocked = prefs.getBoolean("badge_home_starter", false)
        if (applianceCount >= 5 && !starterBadgeUnlocked) {
            editor.putBoolean("badge_home_starter", true)
            editor.putBoolean("challenge_5_appliances_complete", true)
            showBadgePopup("Badge Unlocked: Home Starter!", "You've added 5 appliances! +75 GP Bonus!")
            // Add bonus points
            editor.putInt("totalPoints", newPoints + 75)
        }

        editor.apply() // Save all changes
    }

    private fun showBadgePopup(title: String, message: String) {
        // Run on UI thread to be safe
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Awesome!") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}