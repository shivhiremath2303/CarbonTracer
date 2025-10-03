package com.example.carbontracer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BadgesActivity : AppCompatActivity() {

    private lateinit var rvBadges: RecyclerView
    private lateinit var adapter: BadgeAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbarBadges)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvBadges = findViewById(R.id.rvBadges)
        rvBadges.layoutManager = LinearLayoutManager(this)

        loadBadges()
    }

    private fun loadBadges() {
        val allBadges = getAllBadges()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            adapter = BadgeAdapter(allBadges)
            rvBadges.adapter = adapter
            return
        }

        db.collection("user_badges").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val earnedBadges = document?.get("earned_badges") as? List<String> ?: emptyList()
                allBadges.forEach { badge ->
                    if (earnedBadges.contains(badge.name)) {
                        badge.isEarned = true
                    }
                }
                adapter = BadgeAdapter(allBadges)
                rvBadges.adapter = adapter
            }
            .addOnFailureListener {
                adapter = BadgeAdapter(allBadges)
                rvBadges.adapter = adapter
            }
    }

    private fun getAllBadges(): List<Badge> {
        return listOf(
            Badge("Profile Pro", "Awarded for completing 100% of the user profile, including adding a profile picture.", R.drawable.ic_badge_profile_pro),
            Badge("Newcomer", "A welcome badge for successfully creating an account and logging in for the first time.", R.drawable.ic_badge_newcomer),
            Badge("Age-Wise", "Awarded for providing a date of birth.", R.drawable.ic_badge_age_wise),
            Badge("Community Member", "Given when the user provides their location details (City, State).", R.drawable.ic_badge_community_member),
            Badge("Daily Tracker", "Awarded for logging an activity every day for a week.", R.drawable.ic_badge_daily_tracker),
            Badge("Consistent Contributor", "For logging activities for 30 consecutive days.", R.drawable.ic_badge_consistent_contributor),
            Badge("Data Pioneer", "For logging your first transportation, energy, or food-related emission.", R.drawable.ic_badge_data_pioneer),
            Badge("Weekend Warrior", "For consistently logging activities over a weekend.", R.drawable.ic_badge_weekend_warrior),
            Badge("Perfect Month", "Awarded for not missing a single day of tracking in a calendar month.", R.drawable.ic_badge_perfect_month),
            Badge("Eco-Warrior", "Achieve a personal goal of reducing your carbon footprint by 10% compared to the previous month.", R.drawable.ic_badge_eco_warrior),
            Badge("Green Commuter", "Log 10 trips using public transport, cycling, or walking instead of a private car.", R.drawable.ic_badge_green_commuter),
            Badge("Energy Saver", "Show a consistent reduction in home energy consumption over a period of three months.", R.drawable.ic_badge_energy_saver),
            Badge("Low Carbon Eater", "Log a certain number of low-carbon meals (e.g., plant-based) in a week.", R.drawable.ic_badge_low_carbon_eater),
            Badge("Net-Zero Hero", "Achieve a day with a net-zero carbon footprint, perhaps through a combination of low-impact activities and carbon offsets.", R.drawable.ic_badge_net_zero_hero),
            Badge("Personal Best", "Awarded when a user achieves their lowest weekly or monthly carbon footprint to date.", R.drawable.ic_badge_personal_best),
            Badge("Community Leader", "If you implement community features, this could be for starting a popular discussion or leading a local group challenge.", R.drawable.ic_badge_community_leader),
            Badge("Team Player", "Awarded for participating in a group or community-wide carbon reduction challenge.", R.drawable.ic_badge_team_player),
            Badge("Influencer", "For successfully inviting a certain number of friends to join the CarbonTracer app.", R.drawable.ic_badge_influencer)
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
