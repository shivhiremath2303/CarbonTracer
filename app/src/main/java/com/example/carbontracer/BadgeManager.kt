package com.example.carbontracer

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.Calendar

class BadgeManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun checkAndAwardProfileBadges() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                // Check for Age-Wise badge
                if (!document.getString("dob").isNullOrEmpty()) {
                    awardBadge(userId, "Age-Wise")
                }

                // Check for Community Member badge
                if (!document.getString("city").isNullOrEmpty() && !document.getString("state").isNullOrEmpty()) {
                    awardBadge(userId, "Community Member")
                }

                // Check for Profile Pro badge
                if (user.photoUrl != null &&
                    !user.displayName.isNullOrEmpty() &&
                    !document.getString("dob").isNullOrEmpty() &&
                    !document.getString("houseBuildingName").isNullOrEmpty() &&
                    !document.getString("areaColony").isNullOrEmpty() &&
                    !document.getString("city").isNullOrEmpty() &&
                    !document.getString("state").isNullOrEmpty() &&
                    !document.getString("pincode").isNullOrEmpty()
                ) {
                    awardBadge(userId, "Profile Pro")
                }
            }
        }
    }

    fun awardNewcomerBadge() {
        val user = auth.currentUser ?: return
        awardBadge(user.uid, "Newcomer")
    }

    fun checkAndAwardDataBadges() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        db.collection("emissions").whereEqualTo("userId", userId).get().addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                // Award Data Pioneer badge for the first emission
                awardBadge(userId, "Data Pioneer")

                val emissions = documents.map { it.toObject(Emission::class.java) }

                // Daily Tracker (7 consecutive days)
                if (hasConsecutiveDays(emissions, 7)) {
                    awardBadge(userId, "Daily Tracker")
                }

                // Consistent Contributor (30 consecutive days)
                if (hasConsecutiveDays(emissions, 30)) {
                    awardBadge(userId, "Consistent Contributor")
                }

                // Weekend Warrior
                if (loggedOnWeekend(emissions)) {
                    awardBadge(userId, "Weekend Warrior")
                }

                // Perfect Month
                if (loggedEveryDayOfMonth(emissions)) {
                    awardBadge(userId, "Perfect Month")
                }

                // Green Commuter
                if (isGreenCommuter(emissions)) {
                    awardBadge(userId, "Green Commuter")
                }

                // Eco-Warrior
                if (isEcoWarrior(emissions)) {
                    awardBadge(userId, "Eco-Warrior")
                }

                // Energy Saver
                if (isEnergySaver(emissions)) {
                    awardBadge(userId, "Energy Saver")
                }

                // Personal Best
                if (isPersonalBest(emissions)) {
                    awardBadge(userId, "Personal Best")
                }
            }
        }
    }

    fun checkAndAwardCommunityBadges() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // Placeholder for Community Leader
        // awardBadge(userId, "Community Leader")

        // Placeholder for Team Player
        // awardBadge(userId, "Team Player")

        // Placeholder for Influencer
        // awardBadge(userId, "Influencer")
    }

    private fun isEcoWarrior(emissions: List<Emission>): Boolean {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        cal.add(Calendar.MONTH, -1)
        val previousMonth = cal.get(Calendar.MONTH)

        val currentMonthEmissions = emissions.filter { 
            val emissionCal = Calendar.getInstance()
            emissionCal.timeInMillis = it.timestamp
            emissionCal.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.carbon_emissions }

        val previousMonthEmissions = emissions.filter { 
            val emissionCal = Calendar.getInstance()
            emissionCal.timeInMillis = it.timestamp
            emissionCal.get(Calendar.MONTH) == previousMonth
        }.sumOf { it.carbon_emissions }

        return currentMonthEmissions > 0 && previousMonthEmissions > 0 && currentMonthEmissions < previousMonthEmissions * 0.9
    }

    private fun isEnergySaver(emissions: List<Emission>): Boolean {
        val monthlyConsumption = emissions
            .filter { it.type == "electricity" }
            .groupBy {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.timestamp
                cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        if (monthlyConsumption.size < 3) return false

        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1) // Last month
        val lastMonthKey = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
        val lastMonthConsumption = monthlyConsumption[lastMonthKey] ?: return false

        cal.add(Calendar.MONTH, -1) // 2 months ago
        val twoMonthsAgoKey = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
        val twoMonthsAgoConsumption = monthlyConsumption[twoMonthsAgoKey] ?: return false

        cal.add(Calendar.MONTH, -1) // 3 months ago
        val threeMonthsAgoKey = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
        val threeMonthsAgoConsumption = monthlyConsumption[threeMonthsAgoKey] ?: return false

        return lastMonthConsumption < twoMonthsAgoConsumption && twoMonthsAgoConsumption < threeMonthsAgoConsumption
    }

    private fun isPersonalBest(emissions: List<Emission>): Boolean {
        if (emissions.isEmpty()) return false

        // Weekly
        val weeklyEmissions = emissions.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) * 52 + cal.get(Calendar.WEEK_OF_YEAR)
        }.mapValues { it.value.sumOf { e -> e.carbon_emissions } }

        if (weeklyEmissions.size > 1) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            val lastWeekKey = cal.get(Calendar.YEAR) * 52 + cal.get(Calendar.WEEK_OF_YEAR)
            val lastWeekEmissions = weeklyEmissions[lastWeekKey]

            if (lastWeekEmissions != null) {
                val olderWeeksEmissions = weeklyEmissions.filter { it.key < lastWeekKey }.values
                if (olderWeeksEmissions.isNotEmpty() && lastWeekEmissions < olderWeeksEmissions.minOrNull()!!) {
                    return true
                }
            }
        }

        // Monthly
        val monthlyEmissions = emissions.groupBy {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
        }.mapValues { it.value.sumOf { e -> e.carbon_emissions } }
        
        if (monthlyEmissions.size > 1) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -1)
            val lastMonthKey = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
            val lastMonthEmissions = monthlyEmissions[lastMonthKey]

            if (lastMonthEmissions != null) {
                val olderMonthsEmissions = monthlyEmissions.filter { it.key < lastMonthKey }.values
                if (olderMonthsEmissions.isNotEmpty() && lastMonthEmissions < olderMonthsEmissions.minOrNull()!!) {
                    return true
                }
            }
        }

        return false
    }

    private fun isGreenCommuter(emissions: List<Emission>): Boolean {
        val greenModes = listOf("Bus", "Train", "Walk/Cycle")
        val greenTripCount = emissions.count { it.transport_type in greenModes }
        return greenTripCount >= 10
    }

    private fun hasConsecutiveDays(emissions: List<Emission>, days: Int): Boolean {
        if (emissions.size < days) return false
        val distinctDays = emissions.map { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.DAY_OF_YEAR)
        }.toSet()

        if (distinctDays.size < days) return false

        val sortedDays = distinctDays.sorted()
        for (i in 0..sortedDays.size - days) {
            var isConsecutive = true
            for (j in 0 until days - 1) {
                if (sortedDays[i + j + 1] - sortedDays[i + j] != 1) {
                    isConsecutive = false
                    break
                }
            }
            if (isConsecutive) return true
        }
        return false
    }

    private fun loggedOnWeekend(emissions: List<Emission>): Boolean {
        return emissions.any { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        }
    }

    private fun loggedEveryDayOfMonth(emissions: List<Emission>): Boolean {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val loggedDays = emissions.filter{
            val logCal = Calendar.getInstance()
            logCal.timeInMillis = it.timestamp
            logCal.get(Calendar.MONTH) == currentMonth
        }.map { 
            val logCal = Calendar.getInstance()
            logCal.timeInMillis = it.timestamp
            logCal.get(Calendar.DAY_OF_MONTH)
        }.toSet()

        return loggedDays.size == daysInMonth
    }

    private fun awardBadge(userId: String, badgeName: String) {
        val userBadgesRef = db.collection("user_badges").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userBadgesRef)
            if (snapshot.exists()) {
                transaction.update(userBadgesRef, "earned_badges", FieldValue.arrayUnion(badgeName))
            } else {
                val data = hashMapOf("earned_badges" to listOf(badgeName))
                transaction.set(userBadgesRef, data)
            }
            null
        }
    }
}

data class Emission(
    val userId: String = "",
    val type: String = "",
    val amount: Double = 0.0,
    val carbon_emissions: Double = 0.0,
    val timestamp: Long = 0,
    val transport_type: String? = null
)
