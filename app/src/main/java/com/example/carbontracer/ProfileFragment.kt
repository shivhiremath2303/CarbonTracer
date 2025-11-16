package com.example.carbontracer

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.example.carbontracer.AppliancesActivity

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var tvProfileName: TextView
    private lateinit var ivUserProfileHeader: ImageView
    private lateinit var tvViewAchievements: TextView
    private lateinit var tvSendFeedback: TextView
    private lateinit var tvAppSettings: TextView
    private lateinit var tvHomeAppliances: TextView
    private lateinit var tvVehicleInfo: TextView

    private var lastClickTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()

        // Initialize views
        tvProfileName = view.findViewById(R.id.tvUserNameHeader)
        ivUserProfileHeader = view.findViewById(R.id.ivUserProfileHeader)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val tvEditProfile = view.findViewById<TextView>(R.id.tvEditProfile)
        tvViewAchievements = view.findViewById(R.id.tvViewAchievements)
        tvSendFeedback = view.findViewById(R.id.tvSendFeedback)
        tvAppSettings = view.findViewById(R.id.tvAppSettings)
        tvHomeAppliances = view.findViewById(R.id.tvHomeAppliances)
        tvVehicleInfo = view.findViewById(R.id.tvVehicleInfo)

        btnLogout.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000){
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()

            auth.signOut()
            Toast.makeText(activity, "Logged out.", Toast.LENGTH_SHORT).show()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        tvEditProfile.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000){
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()

            val intent = Intent(activity, EditProfileActivity::class.java)
            startActivity(intent)
        }

        tvViewAchievements.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()

            val intent = Intent(activity, BadgesActivity::class.java)
            startActivity(intent)
        }

        tvSendFeedback.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()

            // Navigate to FeedbackFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FeedbackFragment()) // Assuming your container ID is fragment_container
                .addToBackStack(null)
                .commit()
        }

        tvAppSettings.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()

            val intent = Intent(activity, SettingsActivity::class.java)
            startActivity(intent)
        }

        tvHomeAppliances.setOnClickListener {
            // Add click debouncing, just like your other buttons
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()

            // Go to AddApplianceActivity instead
            val intent = Intent(activity, AppliancesActivity::class.java)
            startActivity(intent)
        }

        tvVehicleInfo.setOnClickListener {
            // Add click debouncing like your other buttons
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000) {
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime()

            // Start the VehicleInfoActivity
            val intent = Intent(activity, VehicleInfoActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentUser = auth.currentUser
    }

    override fun onResume() {
        super.onResume()
        currentUser = auth.currentUser
        loadUserProfileData()
    }

    private fun loadUserProfileData() {
        currentUser?.let { user ->
            val displayName = user.displayName
            if (!displayName.isNullOrEmpty()) {
                tvProfileName.text = displayName
            } else {
                tvProfileName.text = getString(R.string.user_name)
            }

            val storageRef = FirebaseStorage.getInstance().reference
            val profileImageRef = storageRef.child("profile_images/${user.uid}.jpg")

            profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this@ProfileFragment)
                    .load(uri)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivUserProfileHeader)
            }.addOnFailureListener {
                Glide.with(this@ProfileFragment)
                    .load(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivUserProfileHeader)
            }

        } ?: run {
            tvProfileName.text = getString(R.string.user_not_logged_in)
            Glide.with(this@ProfileFragment)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivUserProfileHeader)
        }
    }
}