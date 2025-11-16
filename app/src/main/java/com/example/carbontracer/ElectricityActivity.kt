package com.example.carbontracer

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.example.carbontracer.model.Appliance
import com.example.carbontracer.adapter.ApplianceAdapter
import java.lang.Exception // Import this

class ElectricityActivity : AppCompatActivity() {

    // --- Firebase ---
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var userId: String? = null // <-- Make this nullable (safer)
    private var applianceListener: ListenerRegistration? = null

    // --- UI Variables ---
    private lateinit var textTotalFootprint: TextView
    private lateinit var textPoints: TextView
    private lateinit var textRank: TextView
    private lateinit var challenge1TextView: TextView
    private lateinit var challenge2TextView: TextView
    private lateinit var badge1ImageView: ImageView
    private lateinit var badge2ImageView: ImageView
    private lateinit var addApplianceButton: FloatingActionButton
    private lateinit var applianceRecyclerView: RecyclerView
    private lateinit var applianceAdapter: ApplianceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electricity)

        // --- NEW: SAFE AUTH CHECK ---
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You are not logged in!", Toast.LENGTH_LONG).show()
            finish() // Close this activity
            return // Stop running onCreate
        }
        userId = currentUser.uid
        // --- END OF CHECK ---

        // --- NEW: SAFER FINDVIEWBYID ---
        try {
            textTotalFootprint = findViewById(R.id.text_total_footprint)
            textPoints = findViewById(R.id.text_gamification_points)
            textRank = findViewById(R.id.text_gamification_rank)
            challenge1TextView = findViewById(R.id.text_challenge_1)
            challenge2TextView = findViewById(R.id.text_challenge_2)
            badge1ImageView = findViewById(R.id.image_badge_1)
            badge2ImageView = findViewById(R.id.image_badge_2)
            addApplianceButton = findViewById(R.id.fab_add_appliance)
            applianceRecyclerView = findViewById(R.id.recycler_view_appliances)
        } catch (e: Exception) {
            // This will catch a wrong ID in your XML
            Toast.makeText(this, "Error in layout file: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        // --- END OF SAFER FINDVIEWBYID ---

        // --- Setup RecyclerView ---
        applianceRecyclerView.layoutManager = LinearLayoutManager(this)
        applianceAdapter = ApplianceAdapter(emptyList())
        applianceRecyclerView.adapter = applianceAdapter

        // --- Setup "Add" Button ---
        addApplianceButton.setOnClickListener {
            val intent = Intent(this, AppliancesActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        // If userId is null, don't even try to start the listener
        if (userId == null) {
            return
        }

        // --- Start the Firebase Listener ---
        val appliancesRef = db.collection("users").document(userId!!) // Safe to use !! here
            .collection("appliances")
            .orderBy("applianceName", Query.Direction.ASCENDING)

        applianceListener = appliancesRef.addSnapshotListener { snapshots, e ->
            // ... (rest of the file is the same) ...
            if (e != null) {
                Toast.makeText(this, "Error loading appliances", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snapshots == null) { return@addSnapshotListener }

            val appliances = snapshots.toObjects<Appliance>()
            applianceAdapter.updateList(appliances)

            var totalKwhPerMonth = 0.00
            for (appliance in appliances) {
                val kwh = (appliance.wattageUsed * appliance.dailyHoursUsed * 30.0) / 1000.0
                totalKwhPerMonth += kwh
            }

            val totalCo2 = totalKwhPerMonth * 0.4
            textTotalFootprint.text = "%.1f kg CO2/month".format(totalCo2)

            updateGamificationUI(appliances.size)
        }
    }

    override fun onStop() {
        super.onStop()
        applianceListener?.remove()
    }

    // --- updateGamificationUI() and getRank() functions are unchanged ---
    // (Copy them from your previous file)
    private fun updateGamificationUI(applianceCount: Int) {
        val prefs = getSharedPreferences("GamificationPrefs", MODE_PRIVATE)

        val totalPoints = prefs.getInt("totalPoints", 0)
        textPoints.text = "$totalPoints GP"
        textRank.text = getRank(totalPoints)

        if (prefs.getBoolean("badge_first_step", false)) {
            badge1ImageView.setImageResource(R.drawable.badge_first_step_unlocked)
        } else {
            badge1ImageView.setImageResource(R.drawable.badge_locked)
        }

        if (prefs.getBoolean("badge_home_starter", false)) {
            badge2ImageView.setImageResource(R.drawable.badge_home_starter_unlocked)
        } else {
            badge2ImageView.setImageResource(R.drawable.badge_locked)
        }

        if (applianceCount > 0) {
            challenge1TextView.text = "[✓] Add your first appliance"
        } else {
            challenge1TextView.text = "[ ] Add your first appliance"
        }

        if (applianceCount >= 5) {
            challenge2TextView.text = "[✓] Add 5 appliances"
        } else {
            challenge2TextView.text = "[ ] Add 5 appliances"
        }
    }

    private fun getRank(points: Int): String {
        return when {
            points > 1000 -> "Eco-Hero"
            points > 500 -> "Tree Hugger"
            points > 200 -> "Recycler"
            points > 50 -> "Seedling"
            else -> "Sprout"
        }
    }
}