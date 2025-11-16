package com.example.carbontracer // Make sure this matches your package

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.example.carbontracer.model.Appliance // Your model
import com.example.carbontracer.adapter.ApplianceAdapter // Your adapter

class ElectricityActivity : AppCompatActivity() {

    // --- Firebase ---
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId = auth.currentUser!!.uid
    private var applianceListener: ListenerRegistration? = null // To stop listener

    // --- UI Variables ---
    private lateinit var textTotalFootprint: TextView
    private lateinit var textPoints: TextView
    private lateinit var textRank: TextView
    private lateinit var challenge1TextView: TextView
    private lateinit var challenge2TextView: TextView
    private lateinit var badge1ImageView: ImageView
    private lateinit var badge2ImageView: ImageView
    private lateinit var addApplianceButton: FloatingActionButton

    // --- RecyclerView ---
    private lateinit var applianceRecyclerView: RecyclerView
    private lateinit var applianceAdapter: ApplianceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electricity)

        // --- Find all your UI elements ---
        textTotalFootprint = findViewById(R.id.text_total_footprint)
        textPoints = findViewById(R.id.text_gamification_points)
        textRank = findViewById(R.id.text_gamification_rank)
        challenge1TextView = findViewById(R.id.text_challenge_1)
        challenge2TextView = findViewById(R.id.text_challenge_2)
        badge1ImageView = findViewById(R.id.image_badge_1)
        badge2ImageView = findViewById(R.id.image_badge_2)
        addApplianceButton = findViewById(R.id.fab_add_appliance)

        // --- Setup RecyclerView ---
        applianceRecyclerView = findViewById(R.id.recycler_view_appliances)
        applianceRecyclerView.layoutManager = LinearLayoutManager(this)
        applianceAdapter = ApplianceAdapter(emptyList())
        applianceRecyclerView.adapter = applianceAdapter

        // --- Setup "Add" Button ---
        addApplianceButton.setOnClickListener {
            // This launches the form you built
            val intent = Intent(this, AppliancesActivity::class.java)
            startActivity(intent)
        }
    }

    // This is called every time the activity becomes visible
    override fun onStart() {
        super.onStart()

        // --- Start the Firebase Listener ---
        val appliancesRef = db.collection("users").document(userId)
            .collection("appliances")
            .orderBy("applianceName", Query.Direction.ASCENDING)

        applianceListener = appliancesRef.addSnapshotListener { snapshots, e ->

            if (e != null) {
                Toast.makeText(this, "Error loading appliances", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snapshots == null) { return@addSnapshotListener }

            // 1. Get the list of appliance objects
            val appliances = snapshots.toObjects<Appliance>()

            // 2. Update your RecyclerView
            applianceAdapter.updateList(appliances)

            // 3. Calculate Total Footprint (for ML model)
            var totalKwhPerMonth = 0.0
            for (appliance in appliances) {
                val kwh = (appliance.wattageUsed * appliance.dailyHoursUsed * 30.0) / 1000.0
                totalKwhPerMonth += kwh
            }

            // 4. Convert kWh to CO2 (Example: 0.4 kg CO2 per kWh - change this)
            val totalCo2 = totalKwhPerMonth * 0.4
            textTotalFootprint.text = "%.1f kg CO2/month".format(totalCo2)

            // 5. Update All Gamification UI
            updateGamificationUI(appliances.size)
        }
    }

    // This is called when the activity is no longer visible
    override fun onStop() {
        super.onStop()
        // --- Stop the listener to save resources ---
        applianceListener?.remove()
    }

    /**
     * GAMIFICATION: Reads from SharedPreferences and updates all
     * the text and images on your screen.
     */
    private fun updateGamificationUI(applianceCount: Int) {
        val prefs = getSharedPreferences("GamificationPrefs", MODE_PRIVATE)

        // --- Update Points & Rank ---
        val totalPoints = prefs.getInt("totalPoints", 0)
        textPoints.text = "$totalPoints GP"
        textRank.text = getRank(totalPoints) // Use the helper function below

        // --- Update Badges (Trophy Shelf) ---
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

        // --- Update Challenges Checklist ---
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

    /**
     * GAMIFICATION: Helper to get Rank from points.
     */
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