package com.example.carbontracer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ---- Data classes ----

data class UserAssets(
    // vehicles
    val hasCar: Boolean = false,
    val hasMotorcycle: Boolean = false,
    val hasScooter: Boolean = false,
    val hasPetrolVehicle: Boolean = false,
    val hasDieselVehicle: Boolean = false,
    val hasCngVehicle: Boolean = false,

    // appliances
    val hasRefrigerator: Boolean = false,
    val hasLedTv: Boolean = false,
    val hasAirConditioner: Boolean = false,
    val hasCeilingFan: Boolean = false,
    val hasLaptop: Boolean = false,
    val hasLedBulb: Boolean = false,
    val hasWashingMachine: Boolean = false,
    val hasMicrowaveOven: Boolean = false,
    val hasGeyser: Boolean = false,
    val hasDieselGenerator: Boolean = false
)

data class EmissionHistoryEntry(
    val timestamp: Long,
    val year: Int,
    val month: Int,
    val predictedEmissionKg: Double
)

class TipsFragment : Fragment() {

    private lateinit var etYear: EditText
    private lateinit var spinnerMonth: Spinner
    private lateinit var etElectricity: EditText
    private lateinit var etLpg: EditText
    private lateinit var etPetrol: EditText
    private lateinit var etDiesel: EditText
    private lateinit var btnPredict: Button
    private lateinit var tvCurrentPrediction: TextView
    private lateinit var layoutFuturePredictions: LinearLayout
    private lateinit var tvSuggestions: TextView

    // ML helpers
    private lateinit var emissionPredictor: EmissionPredictor
    private lateinit var emissionForecaster: EmissionForecaster

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // MinMaxScaler values (from Python)
    private val scalerMin = 402.27917f
    private val scalerMax = 479.00724f

    private val historyLimit = 6L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tips, container, false)

        // Bind views with IDs from XML
        etYear = view.findViewById(R.id.edit_year)
        spinnerMonth = view.findViewById(R.id.spinner_month)
        etElectricity = view.findViewById(R.id.edit_electricity)
        etLpg = view.findViewById(R.id.edit_lpg)
        etPetrol = view.findViewById(R.id.edit_petrol)
        etDiesel = view.findViewById(R.id.edit_diesel)
        btnPredict = view.findViewById(R.id.button_predict)
        tvCurrentPrediction = view.findViewById(R.id.text_current_prediction)
        layoutFuturePredictions = view.findViewById(R.id.layout_future_predictions)
        tvSuggestions = view.findViewById(R.id.text_suggestions)

        // Init ML
        emissionPredictor = EmissionPredictor(requireContext())
        emissionForecaster = EmissionForecaster(requireContext())

        // Init Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Month spinner
        val monthAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.months_array,
            android.R.layout.simple_spinner_item
        )
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter

        // Prefill current year but user can edit
        val now = Calendar.getInstance()
        etYear.setText(now.get(Calendar.YEAR).toString())
        spinnerMonth.setSelection(now.get(Calendar.MONTH))

        btnPredict.setOnClickListener { startPredictionFlow() }

        return view
    }

    // ---------------- MAIN FLOW ----------------

    private fun startPredictionFlow() {
        val year = etYear.text?.toString()?.toIntOrNull()
        val monthIndex = spinnerMonth.selectedItemPosition
        val month = monthIndex + 1

        val electricity = etElectricity.text?.toString()?.toDoubleOrNull() ?: 0.0
        val lpg = etLpg.text?.toString()?.toDoubleOrNull() ?: 0.0
        val petrol = etPetrol.text?.toString()?.toDoubleOrNull() ?: 0.0
        val diesel = etDiesel.text?.toString()?.toDoubleOrNull() ?: 0.0

        if (year == null) {
            Toast.makeText(requireContext(), "Please enter a valid year.", Toast.LENGTH_SHORT).show()
            return
        }
        if (electricity == 0.0 && lpg == 0.0 && petrol == 0.0 && diesel == 0.0) {
            Toast.makeText(
                requireContext(),
                "Please enter at least one consumption value.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 1) Predict current emission
        val currentPrediction = emissionPredictor.predict(
            electricityKwh = electricity,
            lpgKg = lpg,
            petrolLiters = petrol,
            dieselLiters = diesel
        )

        // 2) Forecast 6 months
        val futurePredictions = emissionForecaster.forecastNextSixMonths(
            currentEmission = currentPrediction,
            scalerMin = scalerMin,
            scalerMax = scalerMax
        )

        val user = auth.currentUser
        if (user == null) {
            val suggestions = generateSuggestionsUsingAssets(
                electricityKwh = electricity,
                lpgKg = lpg,
                petrolLiters = petrol,
                dieselLiters = diesel,
                totalEmission = currentPrediction.toDouble(),
                assets = UserAssets(),
                history = emptyList()
            )
            updateUiWithResults(
                year = year,
                monthIndex = monthIndex,
                currentPrediction = currentPrediction.toDouble(),
                futurePredictions = futurePredictions.map { it.toDouble() },
                suggestions = suggestions
            )
            Toast.makeText(
                requireContext(),
                "Not logged in: results not saved to cloud.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val uid = user.uid

        loadUserAssetsAndHistory(
            uid = uid,
            limit = historyLimit,
            onResult = { assets, history ->
                val suggestions = generateSuggestionsUsingAssets(
                    electricityKwh = electricity,
                    lpgKg = lpg,
                    petrolLiters = petrol,
                    dieselLiters = diesel,
                    totalEmission = currentPrediction.toDouble(),
                    assets = assets,
                    history = history
                )

                saveEmissionRecord(
                    uid = uid,
                    year = year,
                    month = month,
                    electricityKwh = electricity,
                    lpgKg = lpg,
                    petrolLiters = petrol,
                    dieselLiters = diesel,
                    predictedEmission = currentPrediction.toDouble(),
                    futurePredictions = futurePredictions.map { it.toDouble() }
                )

                updateUiWithResults(
                    year = year,
                    monthIndex = monthIndex,
                    currentPrediction = currentPrediction.toDouble(),
                    futurePredictions = futurePredictions.map { it.toDouble() },
                    suggestions = suggestions
                )
            },
            onError = {
                val suggestions = generateSuggestionsUsingAssets(
                    electricityKwh = electricity,
                    lpgKg = lpg,
                    petrolLiters = petrol,
                    dieselLiters = diesel,
                    totalEmission = currentPrediction.toDouble(),
                    assets = UserAssets(),
                    history = emptyList()
                )

                saveEmissionRecord(
                    uid = uid,
                    year = year,
                    month = month,
                    electricityKwh = electricity,
                    lpgKg = lpg,
                    petrolLiters = petrol,
                    dieselLiters = diesel,
                    predictedEmission = currentPrediction.toDouble(),
                    futurePredictions = futurePredictions.map { it.toDouble() }
                )

                updateUiWithResults(
                    year = year,
                    monthIndex = monthIndex,
                    currentPrediction = currentPrediction.toDouble(),
                    futurePredictions = futurePredictions.map { it.toDouble() },
                    suggestions = suggestions
                )
            }
        )
    }

    // --------------- LOAD ASSETS + HISTORY ----------------

    private fun loadUserAssetsAndHistory(
        uid: String,
        limit: Long,
        onResult: (UserAssets, List<EmissionHistoryEntry>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        var hasCar = false
        var hasMotorcycle = false
        var hasScooter = false
        var hasPetrolVehicle = false
        var hasDieselVehicle = false
        var hasCngVehicle = false

        var hasRefrigerator = false
        var hasLedTv = false
        var hasAirConditioner = false
        var hasCeilingFan = false
        var hasLaptop = false
        var hasLedBulb = false
        var hasWashingMachine = false
        var hasMicrowaveOven = false
        var hasGeyser = false
        var hasDieselGenerator = false

        db.collection("users").document(uid).collection("vehicles")
            .get()
            .addOnSuccessListener { vehicleSnap ->
                for (doc in vehicleSnap.documents) {
                    val type = (doc.getString("vehicleType") ?: doc.getString("type") ?: "").trim()
                    val fuel = (doc.getString("fuelType") ?: doc.getString("fuel") ?: "").trim()

                    when (type) {
                        "Car" -> hasCar = true
                        "Motorcycle" -> hasMotorcycle = true
                        "Scooter" -> hasScooter = true
                    }
                    when (fuel) {
                        "Petrol" -> hasPetrolVehicle = true
                        "Diesel" -> hasDieselVehicle = true
                        "CNG" -> hasCngVehicle = true
                    }
                }

                db.collection("users").document(uid).collection("appliances")
                    .get()
                    .addOnSuccessListener { appSnap ->
                        for (doc in appSnap.documents) {
                            val name =
                                (doc.getString("applianceName") ?: doc.getString("name") ?: "").trim()
                            when (name) {
                                "Refrigerator" -> hasRefrigerator = true
                                "LED TV" -> hasLedTv = true
                                "Air Conditioner" -> hasAirConditioner = true
                                "Ceiling Fan" -> hasCeilingFan = true
                                "Laptop" -> hasLaptop = true
                                "Light Bulb (LED)" -> hasLedBulb = true
                                "Washing Machine" -> hasWashingMachine = true
                                "Microwave Oven" -> hasMicrowaveOven = true
                                "Water Heater (Geyser)" -> hasGeyser = true
                                "Diesel Generator" -> hasDieselGenerator = true
                            }
                        }

                        val assets = UserAssets(
                            hasCar, hasMotorcycle, hasScooter,
                            hasPetrolVehicle, hasDieselVehicle, hasCngVehicle,
                            hasRefrigerator, hasLedTv, hasAirConditioner,
                            hasCeilingFan, hasLaptop, hasLedBulb,
                            hasWashingMachine, hasMicrowaveOven,
                            hasGeyser, hasDieselGenerator
                        )

                        db.collection("users").document(uid).collection("emissions")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(limit)
                            .get()
                            .addOnSuccessListener { histSnap ->
                                val history = histSnap.documents.mapNotNull { d ->
                                    val ts = d.getLong("timestamp")
                                    val y = d.getLong("year")
                                    val m = d.getLong("month")
                                    val pred = d.getDouble("predictedEmissionKg")
                                    if (ts != null && y != null && m != null && pred != null) {
                                        EmissionHistoryEntry(
                                            timestamp = ts,
                                            year = y.toInt(),
                                            month = m.toInt(),
                                            predictedEmissionKg = pred
                                        )
                                    } else null
                                }
                                onResult(assets, history)
                            }
                            .addOnFailureListener(onError)
                    }
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    // --------------- SAVE EMISSIONS ----------------

    private fun saveEmissionRecord(
        uid: String,
        year: Int,
        month: Int,
        electricityKwh: Double,
        lpgKg: Double,
        petrolLiters: Double,
        dieselLiters: Double,
        predictedEmission: Double,
        futurePredictions: List<Double>
    ) {
        val record = hashMapOf(
            "userId" to uid,
            "timestamp" to System.currentTimeMillis(),
            "year" to year,
            "month" to month,
            "electricityKWh" to electricityKwh,
            "lpgKg" to lpgKg,
            "petrolLiters" to petrolLiters,
            "dieselLiters" to dieselLiters,
            "predictedEmissionKg" to predictedEmission,
            "futureEmissionsKg" to futurePredictions
        )

        db.collection("users").document(uid)
            .collection("emissions")
            .add(record)
    }

    // --------------- UPDATE UI ----------------

    private fun updateUiWithResults(
        year: Int,
        monthIndex: Int,
        currentPrediction: Double,
        futurePredictions: List<Double>,
        suggestions: List<String>
    ) {
        val currentMonthName = spinnerMonth.selectedItem?.toString() ?: ""
        tvCurrentPrediction.text =
            "Predicted Emission for $currentMonthName $year: %.2f kg CO2".format(currentPrediction)

        layoutFuturePredictions.removeAllViews()

        futurePredictions.forEachIndexed { i, value ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthIndex)
                add(Calendar.MONTH, i + 1)
            }
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
            val futureYear = cal.get(Calendar.YEAR)

            val tv = TextView(requireContext()).apply {
                text = "$monthName $futureYear: %.2f kg CO2".format(value)
                setPadding(0, 8, 0, 8)
            }
            layoutFuturePredictions.addView(tv)
        }

        // Show suggestions as lines in one TextView
        if (suggestions.isEmpty()) {
            tvSuggestions.text = "No specific suggestions available."
        } else {
            tvSuggestions.text = suggestions.joinToString(
                separator = "\n"
            ) { "• $it" }
        }
    }

    // --------------- SUGGESTION LOGIC ----------------

    private fun generateSuggestionsUsingAssets(
        electricityKwh: Double,
        lpgKg: Double,
        petrolLiters: Double,
        dieselLiters: Double,
        totalEmission: Double,
        assets: UserAssets,
        history: List<EmissionHistoryEntry>
    ): List<String> {
        val suggestions = mutableListOf<String>()

        suggestions.add(
            "Consider adopting energy-saving habits and optimizing appliance usage to reduce emissions."
        )

        val elecHigh = 615.0
        val elecVeryHigh = 726.0
        val lpgHigh = 22.0
        val petrolHigh = 44.0
        val dieselHigh = 30.0
        val totalHigh = 694.0
        val totalVeryHigh = 792.0
        val totalLow = 200.0

        if (electricityKwh > elecVeryHigh) {
            suggestions.add(
                "Your electricity use is very high; switch off unused lights and appliances and avoid standby mode."
            )
        } else if (electricityKwh > elecHigh) {
            suggestions.add(
                "High electricity use detected; focus on turning off devices when not needed and using efficient settings."
            )
        }

        if (assets.hasAirConditioner && electricityKwh > elecHigh) {
            suggestions.add(
                "You have an air conditioner; limit AC usage and keep temperature around 24–26°C to save power."
            )
        }

        if (assets.hasRefrigerator && electricityKwh > elecHigh) {
            suggestions.add(
                "Since you use a refrigerator, make sure the door seals are good and avoid opening the door frequently."
            )
        }

        if (assets.hasWashingMachine && electricityKwh > elecHigh) {
            suggestions.add(
                "You have a washing machine; wash full loads, use eco modes, and prefer cold-water washes when possible."
            )
        }

        if (assets.hasLedTv && electricityKwh > elecHigh) {
            suggestions.add(
                "Your LED TV adds to electricity use; reduce screen-on time and turn it off completely when not watching."
            )
        }

        if (assets.hasCeilingFan && electricityKwh > elecHigh) {
            suggestions.add(
                "Using ceiling fans instead of AC whenever possible can significantly reduce electricity consumption."
            )
        }

        if (assets.hasLaptop && electricityKwh > elecHigh) {
            suggestions.add(
                "For your laptop, lower brightness and shut it down instead of leaving it in sleep overnight."
            )
        }

        if (assets.hasLedBulb && electricityKwh > elecHigh) {
            suggestions.add(
                "You already use LED bulbs; keep maximizing savings by turning off lights in empty rooms."
            )
        }

        if (assets.hasMicrowaveOven && electricityKwh > elecHigh) {
            suggestions.add(
                "Since you use a microwave oven, avoid running it for very small tasks where reheating on the stove might be more efficient."
            )
        }

        if (assets.hasGeyser && electricityKwh > elecHigh) {
            suggestions.add(
                "You have a water heater (geyser); reduce thermostat temperature and limit hot-water usage duration."
            )
        }

        if (assets.hasDieselGenerator && dieselLiters > 0.0) {
            suggestions.add(
                "Because you use a diesel generator, try minimizing generator run-time and explore cleaner backup options."
            )
        }

        if (assets.hasPetrolVehicle && petrolLiters > petrolHigh) {
            suggestions.add(
                "Your petrol usage is high; consider carpooling, using public transport, or combining multiple trips into one."
            )
        }

        if (assets.hasDieselVehicle && dieselLiters > dieselHigh) {
            suggestions.add(
                "Your diesel usage is high; avoid unnecessary idling and keep your vehicle well maintained for better mileage."
            )
        }

        if (assets.hasCar && (petrolLiters > 0.0 || dieselLiters > 0.0)) {
            suggestions.add(
                "Since you own a car, proper tyre pressure and smooth driving can lower fuel consumption."
            )
        }

        if ((assets.hasMotorcycle || assets.hasScooter) && petrolLiters > 0.0) {
            suggestions.add(
                "For your two-wheeler, regular servicing and riding at steady speeds improve mileage and reduce emissions."
            )
        }

        if (assets.hasCngVehicle) {
            suggestions.add(
                "Using CNG vehicles is cleaner; keep them well maintained for best efficiency."
            )
        }

        val lastRecord = history.firstOrNull()
        if (lastRecord != null) {
            val diff = totalEmission - lastRecord.predictedEmissionKg
            val absDiff = kotlin.math.abs(diff)
            if (absDiff >= 10.0) {
                if (diff < 0) {
                    suggestions.add(
                        "Good job! Your emissions decreased by %.1f kg CO2 compared to your last recorded month."
                            .format(absDiff)
                    )
                } else {
                    suggestions.add(
                        "Your emissions increased by %.1f kg CO2 compared to your last recorded month. Try focusing on either electricity or fuel savings."
                            .format(absDiff)
                    )
                }
            }
        }

        when {
            totalEmission > totalVeryHigh -> suggestions.add(
                "Your total emissions are among the highest; tackling both home energy and transport use will help bring them down."
            )
            totalEmission > totalHigh -> suggestions.add(
                "Your total emissions are above average; small daily changes in energy and travel habits can make a noticeable difference."
            )
            totalEmission < totalLow -> suggestions.add(
                "Your emissions are relatively low; keep maintaining your low-carbon lifestyle!"
            )
        }

        return suggestions.distinct()
    }
}
