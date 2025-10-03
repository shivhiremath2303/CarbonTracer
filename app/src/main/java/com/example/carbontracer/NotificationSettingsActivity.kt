package com.example.carbontracer

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.switchmaterial.SwitchMaterial

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var switchReminder: SwitchMaterial
    private lateinit var switchBadgeUnlocked: SwitchMaterial
    private lateinit var switchChallengeUpdates: SwitchMaterial
    private lateinit var switchWeeklySummary: SwitchMaterial
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        prefs = getSharedPreferences("CarbonTracerPrefs", Context.MODE_PRIVATE)

        switchReminder = findViewById(R.id.switchReminder)
        switchBadgeUnlocked = findViewById(R.id.switchBadgeUnlocked)
        switchChallengeUpdates = findViewById(R.id.switchChallengeUpdates)
        switchWeeklySummary = findViewById(R.id.switchWeeklySummary)

        loadSettings()

        setupListeners()
    }

    private fun loadSettings() {
        switchReminder.isChecked = prefs.getBoolean("notification_reminder", true)
        switchBadgeUnlocked.isChecked = prefs.getBoolean("notification_badge", true)
        switchChallengeUpdates.isChecked = prefs.getBoolean("notification_challenge", true)
        switchWeeklySummary.isChecked = prefs.getBoolean("notification_summary", true)
    }

    private fun setupListeners() {
        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notification_reminder", isChecked).apply()
        }
        switchBadgeUnlocked.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notification_badge", isChecked).apply()
        }
        switchChallengeUpdates.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notification_challenge", isChecked).apply()
        }
        switchWeeklySummary.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notification_summary", isChecked).apply()
        }
    }
}