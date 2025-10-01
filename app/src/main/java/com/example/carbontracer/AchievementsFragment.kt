package com.example.carbontracer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

class AchievementsFragment : Fragment() {

    private lateinit var ivStatusEcoWarrior: ImageView
    private lateinit var ivStatusGreenCommuter: ImageView
    private lateinit var ivStatusEnergySaver: ImageView

    // Placeholder for actual data fetching logic (e.g., from Firebase)
    // In a real app, you would fetch these values based on user progress.
    private var isEcoWarriorUnlocked = false // Example: User has logged < 10 eco-actions
    private var isGreenCommuterUnlocked = true  // Example: User has logged 5+ green commutes
    private var isEnergySaverUnlocked = false // Example: User has not completed the energy pledge

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_achievements, container, false)

        // Initialize views
        ivStatusEcoWarrior = view.findViewById(R.id.ivStatusEcoWarrior)
        ivStatusGreenCommuter = view.findViewById(R.id.ivStatusGreenCommuter)
        ivStatusEnergySaver = view.findViewById(R.id.ivStatusEnergySaver)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load achievement statuses (simulated here)
        loadAchievementData()

        // Update UI based on fetched data
        updateAchievementsUI()
    }

    private fun loadAchievementData() {
        // TODO: Replace this with actual data fetching logic from your backend (e.g., Firebase)
        // For now, we are using the placeholder boolean values defined above.
        // Example: You might fetch a user document from Firestore and check specific fields.
        // val userId = FirebaseAuth.getInstance().currentUser?.uid
        // if (userId != null) {
        //     val db = FirebaseFirestore.getInstance()
        //     db.collection("users").document(userId).collection("achievements").document("summary").get()
        //         .addOnSuccessListener { document ->
        //             if (document != null && document.exists()) {
        //                 isEcoWarriorUnlocked = document.getBoolean("ecoWarriorUnlocked") ?: false
        //                 isGreenCommuterUnlocked = document.getBoolean("greenCommuterUnlocked") ?: false
        //                 isEnergySaverUnlocked = document.getBoolean("energySaverUnlocked") ?: false
        //                 updateAchievementsUI() // Update UI after data is fetched
        //             }
        //         }
        //         .addOnFailureListener {
        //             // Handle error
        //         }
        // }
    }

    private fun updateAchievementsUI() {
        updateSingleAchievementUI(ivStatusEcoWarrior, isEcoWarriorUnlocked)
        updateSingleAchievementUI(ivStatusGreenCommuter, isGreenCommuterUnlocked)
        updateSingleAchievementUI(ivStatusEnergySaver, isEnergySaverUnlocked)
    }

    private fun updateSingleAchievementUI(imageView: ImageView, isUnlocked: Boolean) {
        if (isUnlocked) {
            imageView.setImageResource(R.drawable.ic_status_unlocked)
            // Optional: You could also change the tint or add a celebratory animation
        } else {
            imageView.setImageResource(R.drawable.ic_status_locked)
        }
    }
}
