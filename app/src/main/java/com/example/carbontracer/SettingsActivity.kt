package com.example.carbontracer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SettingsActivity : AppCompatActivity() {

    private lateinit var rvSettings: RecyclerView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("CarbonTracerPrefs", Context.MODE_PRIVATE)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbarSettings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvSettings = findViewById(R.id.rvSettings)
        rvSettings.layoutManager = LinearLayoutManager(this)

        val settings = getSettingsList()
        val adapter = SettingsAdapter(settings) { setting ->
            when (setting.title) {
                "Change Password" -> startActivity(Intent(this, ChangePasswordActivity::class.java))
                "Location Settings" -> startActivity(Intent(this, EditProfileActivity::class.java))
                "Delete Account" -> showDeleteAccountConfirmationDialog()
                "Notifications" -> startActivity(Intent(this, NotificationSettingsActivity::class.java))
                "Theme" -> showThemeSelectionDialog()
                "Units of Measurement" -> showUnitsSelectionDialog()
                "Send Feedback" -> sendFeedback()
                "Rate the App" -> rateApp()
                "About CarbonTracer" -> showAboutDialog()
                "App Version" -> showAppVersion()
                else -> Toast.makeText(this, "Clicked: ${setting.title}", Toast.LENGTH_SHORT).show()
            }
        }
        rvSettings.adapter = adapter
    }

    private fun getSettingsList(): List<Setting> {
        return listOf(
            Setting("Change Password", "Update your login credentials."),
            Setting("Location Settings", "Update your city and state for tailored content."),
            Setting("Delete Account", "Permanently delete your account and data."),
            Setting("Notifications", "Manage push notifications and alerts."),
            Setting("Theme", "Choose between Light, Dark, or System Default."),
            Setting("Units of Measurement", "Switch between metric and imperial units."),
            Setting("About CarbonTracer", "Our mission, and what we are all about."),
            Setting("Send Feedback", "Report bugs or suggest new features."),
            Setting("Rate the App", "Leave a review on the Google Play Store."),
            Setting("App Version", "Version 1.0.0") // Example version
        )
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Light", "Dark", "System Default")
        AlertDialog.Builder(this)
            .setTitle("Theme")
            .setItems(themes) { _, which ->
                val mode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_NO
                    1 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
            }
            .show()
    }

    private fun showUnitsSelectionDialog() {
        val units = arrayOf("Metric (kg, km)", "Imperial (lbs, miles)")
        val currentUnit = if (prefs.getString("units", "metric") == "metric") 0 else 1
        AlertDialog.Builder(this)
            .setTitle("Units of Measurement")
            .setSingleChoiceItems(units, currentUnit) { dialog, which ->
                val selectedUnit = if (which == 0) "metric" else "imperial"
                prefs.edit().putString("units", selectedUnit).apply()
                dialog.dismiss()
            }
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // 1. Delete Firestore data
        db.collection("users").document(userId).delete()
        db.collection("user_badges").document(userId).delete()
        db.collection("emissions").whereEqualTo("userId", userId).get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                document.reference.delete()
            }
        }

        // 2. Delete Storage data
        storage.reference.child("profile_images/$userId.jpg").delete()

        // 3. Delete user from Auth
        user.delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Error deleting account: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Only email apps should handle this
            putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@carbontracer.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback for CarbonTracer App")
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No email client found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rateApp() {
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("About CarbonTracer")
            .setMessage("CarbonTracer is a mobile application designed to help you track and reduce your carbon footprint. Our mission is to empower individuals to make a positive impact on the environment.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAppVersion() {
        try {
            val version = packageManager.getPackageInfo(packageName, 0).versionName
            AlertDialog.Builder(this)
                .setTitle("App Version")
                .setMessage("Version $version")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
