package com.example.carbontracer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
// Removed: import android.util.Log
// Removed: import android.view.MenuItem
import androidx.fragment.app.Fragment
// Removed: import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
// Removed: import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
// Explicitly import your fragment classes
import com.example.carbontracer.HomeFragment
import com.example.carbontracer.TipsFragment
import com.example.carbontracer.CameraFragment
import com.example.carbontracer.LeaderboardFragment
import com.example.carbontracer.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var lastNavigationClickTime: Long = 0

    private val homeFragment = HomeFragment()
    private val tipsFragment = TipsFragment()
    private val cameraFragment = CameraFragment()
    private val leaderboardFragment = LeaderboardFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment

    // Removed: private var navProfileImageView: ShapeableImageView? = null
    // Removed: private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Removed: bottomNavigation.post { ... } block for profile image

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, profileFragment, "5").hide(profileFragment)
                add(R.id.fragment_container, leaderboardFragment, "4").hide(leaderboardFragment)
                add(R.id.fragment_container, cameraFragment, "3").hide(cameraFragment)
                add(R.id.fragment_container, tipsFragment, "2").hide(tipsFragment)
                add(R.id.fragment_container, homeFragment, "1")
            }.commitAllowingStateLoss()
            activeFragment = homeFragment
        } else {
            val savedActiveFragmentTag = savedInstanceState.getString("activeFragmentTag", "1")
            activeFragment = supportFragmentManager.findFragmentByTag(savedActiveFragmentTag) ?: homeFragment
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

    // Removed: private fun loadProfileImageInNavBar() { ... }

    override fun onResume() {
        super.onResume()
        // Removed: Logic related to navProfileImageView and loadProfileImageInNavBar()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        activeFragment.tag?.let { 
            outState.putString("activeFragmentTag", it)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
