package com.example.carbontracer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ----------------------
// Data classes
// ----------------------

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

    private lateinit var etYear: TextInputEditText
    private lateinit var spinnerMonth: Spinner
    private lateinit var etElectricity: TextInputEditText
    private lateinit var etLpg: TextInputEditText
    private lateinit var etPetrol: TextInputEditText
    private lateinit var etDiesel: TextInputEditText
    private lateinit var btnPredict: Button
    private lateinit var layoutPredictionResults: LinearLayout
    private lateinit var tvCurrentPrediction: TextView
    private lateinit var layoutFuturePredictions: LinearLayout

    // --- ML model helpers (TF-Lite) ---
    private lateinit var emissionPredictor: EmissionPredictor
    private lateinit var emissionForecaster: EmissionForecaster

    // --- Firebase ---
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // --- MinMaxScaler values from Python's emission_scaler ---
    // last_sequence_scaled = [0.030526307665977193, 0.0, 0.03514208078315839]
    // scaler_min = 402.27917832167833
    // scaler_max = 479.0072404844291
    private val scalerMin = 402.27917f
    private val scalerMax = 479.00724f

    // how many past records to look at
    private val historyLimit = 6L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tips, container, false)

        // Bind views (same IDs as your XML)
        etYear = view.findViewById(R.id.etYear)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        etElectricity = view.findViewById(R.id.etElectricity)
        etLpg = view.findViewById(R.id.etLpg)
        etPetrol = view.findViewById(R.id.etPetrol)
        etDiesel = view.findViewById(R.id.etDiesel)
        btnPredict = view.findViewById(R.id.btnPredict)
        layoutPredictionResults = view.findViewById(R.id.layout_prediction_results)
        tvCurrentPrediction = view.findViewById(R.id.tv_current_prediction)
        layoutFuturePredictions = view.findViewById(R.id.layout_future_predictions)

        // Init TF-Lite models
        emissionPredictor = EmissionPredictor(requireContext())
        emissionForecaster = EmissionForecaster(requireContext())

        // Init Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Month dropdown uses your months_array (UI unchanged)
        val monthAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.months_array,
            android.R.layout.simple_spinner_item
        )
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMonth.adapter = monthAdapter

        // Prefill with current month/year
        val now = Calendar.getInstance()
        etYear.setText(now.get(Calendar.YEAR).toString())
        spinnerMonth.setSelection(now.get(Calendar.MONTH))

        btnPredict.setOnClickListener {
            startPredictionFlow()
        }

        return view
    }

    // -------------------------------------------------------
    //  MAIN FLOW: user taps Predict
    // -------------------------------------------------------
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

        // 1) Predict current month emission (TF-Lite)
        val currentPrediction: Float = emissionPredictor.predict(
            electricityKwh = electricity,
            lpgKg = lpg,
            petrolLiters = petrol,
            dieselLiters = diesel
        )

        // 2) Forecast next 6 months (TF-Lite LSTM)
        val futurePredictions: List<Float> = emissionForecaster.forecastNextSixMonths(
            currentEmission = currentPrediction,
            scalerMin = scalerMin,
            scalerMax = scalerMax
        )

        val user = auth.currentUser
        if (user == null) {
            // Not logged in → no history / assets, just generic suggestions
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

        // 3) Load user's vehicles + appliances + history, then generate suggestions & save
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

                saveEmissionRecordToFirestore(
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
                // fallback: generic suggestions, still save record
                val suggestions = generateSuggestionsUsingAssets(
                    electricityKwh = electricity,
                    lpgKg = lpg,
                    petrolLiters = petrol,
                    dieselLiters = diesel,
                    totalEmission = currentPrediction.toDouble(),
                    assets = UserAssets(),
                    history = emptyList()
                )

                saveEmissionRecordToFirestore(
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

    // -------------------------------------------------------
    //  LOAD VEHICLES + APPLIANCES + HISTORY
    // -------------------------------------------------------
    private fun loadUserAssetsAndHistory(
        uid: String,
        limit: Long,
        onResult: (UserAssets, List<EmissionHistoryEntry>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Mutable flags while we read collections
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

        // 1) Load vehicles
        db.collection("users")
            .document(uid)
            .collection("vehicles")
            .get()
            .addOnSuccessListener { vehicleSnapshot ->
                for (doc in vehicleSnapshot.documents) {
                    // TODO: adjust field names if needed
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

                // 2) Load appliances
                db.collection("users")
                    .document(uid)
                    .collection("appliances")
                    .get()
                    .addOnSuccessListener { applianceSnapshot ->
                        for (doc in applianceSnapshot.documents) {
                            // TODO: adjust field name if needed
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

                        // Build assets object now that we scanned vehicles + appliances
                        val assets = UserAssets(
                            hasCar = hasCar,
                            hasMotorcycle = hasMotorcycle,
                            hasScooter = hasScooter,
                            hasPetrolVehicle = hasPetrolVehicle,
                            hasDieselVehicle = hasDieselVehicle,
                            hasCngVehicle = hasCngVehicle,
                            hasRefrigerator = hasRefrigerator,
                            hasLedTv = hasLedTv,
                            hasAirConditioner = hasAirConditioner,
                            hasCeilingFan = hasCeilingFan,
                            hasLaptop = hasLaptop,
                            hasLedBulb = hasLedBulb,
                            hasWashingMachine = hasWashingMachine,
                            hasMicrowaveOven = hasMicrowaveOven,
                            hasGeyser = hasGeyser,
                            hasDieselGenerator = hasDieselGenerator
                        )

                        // 3) Load last N emission records
                        db.collection("users")
                            .document(uid)
                            .collection("emissions")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(limit)
                            .get()
                            .addOnSuccessListener { historySnapshot ->
                                val history = historySnapshot.documents.mapNotNull { d ->
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
                            .addOnFailureListener { e -> onError(e) }
                    }
                    .addOnFailureListener { e -> onError(e) }
            }
            .addOnFailureListener { e -> onError(e) }
    }

    // -------------------------------------------------------
    //  SAVE EMISSION RECORD
    // -------------------------------------------------------
    private fun saveEmissionRecordToFirestore(
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
        val timestamp = System.currentTimeMillis()

        val record = hashMapOf(
            "userId" to uid,
            "timestamp" to timestamp,
            "year" to year,
            "month" to month,
            "electricityKWh" to electricityKwh,
            "lpgKg" to lpgKg,
            "petrolLiters" to petrolLiters,
            "dieselLiters" to dieselLiters,
            "predictedEmissionKg" to predictedEmission,
            "futureEmissionsKg" to futurePredictions
        )

        db.collection("users")
            .document(uid)
            .collection("emissions")
            .add(record)
            .addOnSuccessListener {
                // optional: Toast / log
            }
            .addOnFailureListener {
                // optional: Toast / log
            }
    }

    // -------------------------------------------------------
    //  UPDATE UI
    // -------------------------------------------------------
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

        for (i in futurePredictions.indices) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthIndex)
                add(Calendar.MONTH, i + 1)
            }
            val futureYear = cal.get(Calendar.YEAR)
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)

            val predictionTextView = TextView(requireContext()).apply {
                text = "$monthName $futureYear: %.2f kg CO2".format(futurePredictions[i])
                setPadding(0, 8, 0, 8)
            }
            layoutFuturePredictions.addView(predictionTextView)
        }

        if (suggestions.isNotEmpty()) {
            val header = TextView(requireContext()).apply {
                text = "Suggestions based on your consumption:"
                setPadding(0, 24, 0, 8)
                textSize = 16f
            }
            layoutFuturePredictions.addView(header)

            suggestions.forEach { suggestion ->
                val suggestionView = TextView(requireContext()).apply {
                    text = "• $suggestion"
                    setPadding(8, 4, 0, 4)
                }
                layoutFuturePredictions.addView(suggestionView)
            }
        }

        layoutPredictionResults.visibility = View.VISIBLE
    }

    // -------------------------------------------------------
    //  SUGGESTION LOGIC (assets + history aware)
    // -------------------------------------------------------
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

        // Always-on baseline suggestion
        suggestions.add(
            "Consider adopting energy-saving habits and optimizing appliance usage to reduce emissions."
        )

        // Thresholds (from dataset)
        val elecHigh = 615.0
        val elecVeryHigh = 726.0
        val lpgHigh = 22.0
        val petrolHigh = 44.0
        val dieselHigh = 30.0
        val totalHigh = 694.0
        val totalVeryHigh = 792.0
        val totalLow = 200.0

        // ---------- ELECTRICITY (no appliance names yet) ----------
        if (electricityKwh > elecVeryHigh) {
            suggestions.add(
                "Your electricity use is very high compared to similar households; try switching off unused lights and appliances and avoid standby mode."
            )
        } else if (electricityKwh > elecHigh) {
            suggestions.add(
                "High electricity use detected; focus on turning off appliances when not needed and using energy-efficient settings."
            )
        }

        // ---------- APPLIANCE-SPECIFIC ----------
        if (assets.hasAirConditioner && electricityKwh > elecHigh) {
            suggestions.add(
                "You have an air conditioner; limiting AC usage hours and keeping the temperature around 24–26°C can reduce electricity use."
            )
        }

        if (assets.hasRefrigerator && electricityKwh > elecHigh) {
            suggestions.add(
                "Since you use a refrigerator, ensure the door seals are good and avoid frequent door openings to save energy."
            )
        }

        if (assets.hasWashingMachine && electricityKwh > elecHigh) {
            suggestions.add(
                "You have a washing machine; wash full loads, use eco modes, and wash with cold water when possible to reduce power consumption."
            )
        }

        if (assets.hasLedTv && electricityKwh > elecHigh) {
            suggestions.add(
                "Your LED TV also adds to electricity use; try reducing screen-on time and turning it off completely when not in use."
            )
        }

        if (assets.hasCeilingFan && electricityKwh > elecHigh) {
            suggestions.add(
                "Using ceiling fans instead of AC whenever possible can significantly cut your cooling-related electricity usage."
            )
        }

        if (assets.hasLaptop && electricityKwh > elecHigh) {
            suggestions.add(
                "For your laptop, lower screen brightness and switch it off instead of keeping it on sleep overnight."
            )
        }

        if (assets.hasLedBulb && electricityKwh > elecHigh) {
            suggestions.add(
                "You are using LED bulbs; keep maximizing their benefit by ensuring lights are turned off in unoccupied rooms."
            )
        }

        if (assets.hasMicrowaveOven && electricityKwh > elecHigh) {
            suggestions.add(
                "Since you use a microwave oven, avoid running it for very small tasks where reheating on the stove might be quicker and more efficient."
            )
        }

        if (assets.hasGeyser && electricityKwh > elecHigh) {
            suggestions.add(
                "You have a water heater; try reducing the thermostat temperature and limiting hot-water usage duration."
            )
        }

        if (assets.hasDieselGenerator && dieselLiters > 0.0) {
            suggestions.add(
                "Because you use a diesel generator, try minimizing generator run-time and explore cleaner backup power options if possible."
            )
        }

        // ---------- FUEL / VEHICLE SUGGESTIONS ----------
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
                "Since you own a car, maintaining proper tyre pressure and avoiding aggressive acceleration can reduce your fuel consumption."
            )
        }

        if ((assets.hasMotorcycle || assets.hasScooter) && petrolLiters > 0.0) {
            suggestions.add(
                "For your two-wheeler, regular servicing and smooth riding at steady speeds can improve mileage and lower emissions."
            )
        }

        if (assets.hasCngVehicle) {
            suggestions.add(
                "Using CNG vehicles is generally cleaner; continue using them and keep them serviced for best efficiency."
            )
        }

        // ---------- HISTORY-BASED TREND ----------
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
                        "Your emissions increased by %.1f kg CO2 compared to your last recorded month. Try focusing on reducing either electricity or fuel usage."
                            .format(absDiff)
                    )
                }
            }
        }

        // ---------- OVERALL LEVEL ----------
        when {
            totalEmission > totalVeryHigh -> {
                suggestions.add(
                    "Your total emissions are among the highest in the dataset; focusing on both home energy efficiency and transport choices can bring them down."
                )
            }
            totalEmission > totalHigh -> {
                suggestions.add(
                    "Your total emissions are above average; small reductions in daily energy and travel habits can make a noticeable difference."
                )
            }
            totalEmission < totalLow -> {
                suggestions.add(
                    "Your emissions are relatively low compared to typical households; keep maintaining your low-carbon lifestyle!"
                )
            }
        }

        return suggestions.distinct()
    }
}
