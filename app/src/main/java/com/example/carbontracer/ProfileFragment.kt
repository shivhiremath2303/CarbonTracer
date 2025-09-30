package com.example.carbontracer

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock // Import for SystemClock
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

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private lateinit var tvProfileName: TextView
    private lateinit var ivUserProfileHeader: ImageView

    private var lastClickTime: Long = 0 // Variable to store last click time

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

        btnLogout.setOnClickListener {
            // Debounce for logout button as well
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000){ // 1000ms debounce
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
            // Debounce logic: if less than 1 second has passed since last click, ignore
            if (SystemClock.elapsedRealtime() - lastClickTime < 1000){ // 1000ms = 1 second
                return@setOnClickListener
            }
            lastClickTime = SystemClock.elapsedRealtime() // Update last click time

            val intent = Intent(activity, EditProfileActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentUser = auth.currentUser
        // loadUserProfileData() // Data will be loaded in onResume initially
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
                tvProfileName.text = "User Name"
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
            tvProfileName.text = "User not logged in"
            Glide.with(this@ProfileFragment)
                .load(R.drawable.ic_profile)
                .circleCrop()
                .into(ivUserProfileHeader)
        }
    }
}