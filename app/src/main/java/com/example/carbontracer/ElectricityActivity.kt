package com.example.carbontracer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.carbontracer.model.Appliance
import com.example.carbontracer.adapter.ApplianceAdapter
import java.lang.Exception
import java.util.Calendar

class ElectricityActivity : AppCompatActivity() {

    // --- Firebase ---
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var userId: String? = null
    private var applianceListener: ListenerRegistration? = null

    // --- UI Variables ---
    private lateinit var textTotalFootprint: TextView
    private lateinit var textPoints: TextView
    private lateinit var textRank: TextView
    private lateinit var textStreakCount: TextView // For the new streak display
    private lateinit var challenge1TextView: TextView
    private lateinit var challenge2TextView: TextView
    private lateinit var badge1ImageView: ImageView
    private lateinit var badge2ImageView: ImageView
    private lateinit var addApplianceButton: FloatingActionButton
    private lateinit var applianceRecyclerView: RecyclerView
    private lateinit var applianceAdapter: ApplianceAdapter

    private val predictionResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val prediction = result.data?.getFloatExtra("predictionResult", 0.0f) ?: 0.0f
            if (prediction > 0.0f) {
                textTotalFootprint.text = "%.1f kg CO2/month".format(prediction)
                
                // Handle Streak Logic
                handleStreakUpdate()
                // Refresh gamification UI to show new streak
                updateGamificationUI(applianceAdapter.itemCount)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electricity)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You are not logged in!", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        userId = currentUser.uid

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

            // Note: Add a TextView with this ID to your layout file for the streak to be visible
            textStreakCount = findViewById(R.id.text_streak_count)

        } catch (e: Exception) {
            Toast.makeText(this, "Error in layout file: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        applianceRecyclerView.layoutManager = LinearLayoutManager(this)
        applianceAdapter = ApplianceAdapter(emptyList(), {}, {})
        applianceRecyclerView.adapter = applianceAdapter

        addApplianceButton.setOnClickListener {
            val intent = Intent(this, PredictionActivity::class.java)
            predictionResultLauncher.launch(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (userId == null) {
            return
        }

        val appliancesRef = db.collection("users").document(userId!!)
            .collection("appliances")
            .orderBy("applianceName", Query.Direction.ASCENDING)

        applianceListener = appliancesRef.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Toast.makeText(this, "Error loading appliances", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (snapshots == null) { return@addSnapshotListener }

            val appliances = snapshots.toObjects<Appliance>()
            applianceAdapter.updateList(appliances)
            updateGamificationUI(appliances.size)
        }
    }

    override fun onStop() {
        super.onStop()
        applianceListener?.remove()
    }

    private fun handleStreakUpdate() {
        val prefs = getSharedPreferences("GamificationPrefs", MODE_PRIVATE)
        val lastUpdateMillis = prefs.getLong("last_streak_update", 0L)
        val currentStreak = prefs.getInt("current_streak", 0)

        val now = Calendar.getInstance()
        val lastUpdate = Calendar.getInstance()
        if (lastUpdateMillis > 0) {
            lastUpdate.timeInMillis = lastUpdateMillis
        }

        // Do nothing if streak was already updated today
        if (lastUpdateMillis > 0 && now.get(Calendar.YEAR) == lastUpdate.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == lastUpdate.get(Calendar.DAY_OF_YEAR)) {
            return
        }

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)

        val newStreak: Int
        if (lastUpdateMillis > 0 && lastUpdate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            lastUpdate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            // Streak continued
            newStreak = currentStreak + 1
            Toast.makeText(this, "Streak continued! $newStreak days!", Toast.LENGTH_SHORT).show()
        } else {
            // Streak broken or first time
            newStreak = 1
            Toast.makeText(this, "New streak started!", Toast.LENGTH_SHORT).show()
        }

        prefs.edit()
            .putInt("current_streak", newStreak)
            .putLong("last_streak_update", now.timeInMillis)
            .apply()
    }

    private fun updateGamificationUI(applianceCount: Int) {
        val prefs = getSharedPreferences("GamificationPrefs", MODE_PRIVATE)

        val totalPoints = prefs.getInt("totalPoints", 0)
        textPoints.text = "$totalPoints GP"
        textRank.text = getRank(totalPoints)

        // Display the streak
        val currentStreak = prefs.getInt("current_streak", 0)
        if (::textStreakCount.isInitialized) {
            textStreakCount.text = "$currentStreak 🔥"
        }

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
