package com.example.carbontracer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate

class HomeFragment : Fragment() {

    private lateinit var themeToggle: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnElectricity = view.findViewById<Button>(R.id.button_electricity)
        btnElectricity.setOnClickListener {
            val intent = Intent(activity, ElectricityActivity::class.java)
            startActivity(intent)
        }

        val btnTransport = view.findViewById<Button>(R.id.button_transport)
        btnTransport.setOnClickListener {
            val intent = Intent(activity, TransportActivity::class.java)
            startActivity(intent)
        }

        themeToggle = view.findViewById(R.id.theme_toggle)
        themeToggle.setOnClickListener { toggleTheme() }

        updateThemeToggleIcon()
    }

    private fun toggleTheme() {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val newNightMode = when (currentNightMode) {
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_YES
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(newNightMode)
    }

    private fun updateThemeToggleIcon() {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> {
                themeToggle.isActivated = false // Light mode
            }
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                themeToggle.isActivated = true // Dark mode
            }
        }
    }
}