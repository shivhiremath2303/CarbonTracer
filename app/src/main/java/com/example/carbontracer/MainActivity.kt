package com.example.carbontracer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var lastNavigationClickTime: Long = 0

    // Declare instances of all fragments
    private val homeFragment = HomeFragment()
    private val tipsFragment = TipsFragment()
    private val cameraFragment = CameraFragment()
    private val leaderboardFragment = LeaderboardFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment // Default active fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Apply slide_in_right for LoginActivity and slide_out_left for MainActivity
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish() // MainActivity finishes, using the specified slide_out_left
            return
        }

        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Initialize fragments only once
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, profileFragment, "5").hide(profileFragment)
                add(R.id.fragment_container, leaderboardFragment, "4").hide(leaderboardFragment)
                add(R.id.fragment_container, cameraFragment, "3").hide(cameraFragment)
                add(R.id.fragment_container, tipsFragment, "2").hide(tipsFragment)
                add(R.id.fragment_container, homeFragment, "1") // Show homeFragment by default
            }.commitAllowingStateLoss()
            activeFragment = homeFragment
        } else {
            activeFragment = supportFragmentManager.findFragmentByTag("1") ?: homeFragment
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            if (SystemClock.elapsedRealtime() - lastNavigationClickTime < 400) {
                return@setOnItemSelectedListener true
            }
            lastNavigationClickTime = SystemClock.elapsedRealtime()

            val targetFragment = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_tips -> tipsFragment
                R.id.nav_camera -> cameraFragment
                R.id.nav_leaderboard -> leaderboardFragment
                R.id.nav_profile -> profileFragment
                else -> null
            }

            if (targetFragment != null && targetFragment != activeFragment) {
                supportFragmentManager.beginTransaction()
                    // If you want fragment transition animations, add them here, e.g.:
                    // .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commitAllowingStateLoss()
                activeFragment = targetFragment
            }
            true
        }
        
        if (savedInstanceState != null) {
             supportFragmentManager.beginTransaction().show(activeFragment).commitAllowingStateLoss()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Apply slide_in_left for the previous activity and slide_out_right for MainActivity
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    // Example for starting new activities as you mentioned:
    // fun startOtherActivity() {
    //     val intent = Intent(this, OtherActivity::class.java) // Replace OtherActivity with your actual class
    //     startActivity(intent)
    //     overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    // }

    // fun startTransportActivity() {
    //     val intent = Intent(this, TransportActivity::class.java) // Replace TransportActivity with your actual class
    //     startActivity(intent)
    //     overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    // }

    // fun startElectricityActivity() {
    //     val intent = Intent(this, ElectricityActivity::class.java) // Replace ElectricityActivity with your actual class
    //     startActivity(intent)
    //     overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    // }

    // loadFragment method is no longer needed (as it was commented out in original)
}
