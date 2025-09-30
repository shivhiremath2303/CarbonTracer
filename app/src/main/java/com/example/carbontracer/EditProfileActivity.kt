package com.example.carbontracer

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etDOB: TextInputEditText
    private lateinit var etHouseBuildingName: TextInputEditText
    private lateinit var etAreaColony: TextInputEditText
    private lateinit var etPincode: TextInputEditText
    private lateinit var etState: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var progressBar: ProgressBar

    private lateinit var ivProfileImage: ImageView
    private lateinit var btnChangeProfilePicture: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var db: FirebaseFirestore // Added Firestore instance

    private val calendar = Calendar.getInstance()
    private var originalEmail: String? = null
    private var originalDisplayName: String? = null
    private var currentPhotoUri: Uri? = null
    private var newImageSelectedForUploadUri: Uri? = null

    // Store original values for Firestore fields
    private var originalDOB: String? = ""
    private var originalHouseBuildingName: String? = ""
    private var originalAreaColony: String? = ""
    private var originalPincode: String? = ""
    private var originalState: String? = ""
    private var originalCity: String? = ""

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestStoragePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private var pendingOperations = 0
    private var successfulOperations = 0
    private var failedOperations = 0

    companion object {
        private const val TAG = "EditProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarEditProfile)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        initializeViews()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        db = FirebaseFirestore.getInstance() // Initialize Firestore

        setupPermissionsLaunchers()
        setupImageLaunchers()

        loadUserProfile()
        setupDOBField()

        btnChangeProfilePicture.setOnClickListener {
            if (progressBar.visibility == View.GONE) {
                showImageSourceDialog()
            }
        }
    }

    private fun initializeViews() {
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnChangeProfilePicture = findViewById(R.id.btnChangeProfilePicture)
        etDOB = findViewById(R.id.etDOB)
        etHouseBuildingName = findViewById(R.id.etHouseBuildingName)
        etAreaColony = findViewById(R.id.etAreaColony)
        etPincode = findViewById(R.id.etPincode)
        etState = findViewById(R.id.etState)
        etCity = findViewById(R.id.etCity)
        progressBar = findViewById(R.id.progressBarEditProfile)
    }

    private fun setupPermissionsLaunchers() {
        requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
            if (isGranted) openCamera() else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
        requestStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted: Boolean ->
            if (isGranted) openGallery() else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupImageLaunchers() {
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let {
                    Glide.with(this).load(it).circleCrop().into(ivProfileImage)
                    newImageSelectedForUploadUri = it
                }
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                Glide.with(this).load(it).circleCrop().into(ivProfileImage)
                newImageSelectedForUploadUri = it
            }
        }
    }

    private fun checkAndRequestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> openCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "Camera permission is needed to take pictures.", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkAndRequestStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> openGallery()
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(this, "Storage permission is needed to select images.", Toast.LENGTH_LONG).show()
                requestStoragePermissionLauncher.launch(permission)
            }
            else -> requestStoragePermissionLauncher.launch(permission)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val localPhotoUri: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", photoFile)
            currentPhotoUri = localPhotoUri
            takePictureLauncher.launch(localPhotoUri)
        } catch (ex: IOException) {
            Log.e(TAG, "Error creating image file for camera", ex)
            Toast.makeText(this, "Error creating file for photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun showImageSourceDialog() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(this).setTitle("Change Profile Picture").setItems(options) { dialog, item ->
            when (options[item]) {
                "Take Photo" -> checkAndRequestCameraPermission()
                "Choose from Gallery" -> checkAndRequestStoragePermission()
                "Cancel" -> dialog.dismiss()
            }
        }.show()
    }

    private fun loadUserProfile() {
        showLoading(true)
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            originalDisplayName = user.displayName
            if (!originalDisplayName.isNullOrEmpty()) {
                val parts = originalDisplayName!!.split(" ", limit = 2)
                etFirstName.setText(parts.getOrNull(0) ?: "")
                etLastName.setText(parts.getOrNull(1) ?: "")
            }
            etEmail.setText(user.email)
            originalEmail = user.email

            user.photoUrl?.let {
                Glide.with(this@EditProfileActivity)
                    .load(it)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivProfileImage)
            } ?: ivProfileImage.setImageResource(R.drawable.ic_profile)

            // Load additional details from Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        originalDOB = document.getString("dob") ?: ""
                        etDOB.setText(originalDOB)
                        originalDOB?.takeIf { it.isNotEmpty() }?.let {
                            try {
                                SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(it)?.let { date -> calendar.time = date }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing stored DOB: $it", e)
                            }
                        }

                        originalHouseBuildingName = document.getString("houseBuildingName") ?: ""
                        etHouseBuildingName.setText(originalHouseBuildingName)

                        originalAreaColony = document.getString("areaColony") ?: ""
                        etAreaColony.setText(originalAreaColony)

                        originalPincode = document.getString("pincode") ?: ""
                        etPincode.setText(originalPincode)

                        originalState = document.getString("state") ?: ""
                        etState.setText(originalState)

                        originalCity = document.getString("city") ?: ""
                        etCity.setText(originalCity)
                        Log.d(TAG, "User profile details loaded from Firestore.")
                    } else {
                        Log.d(TAG, "No profile details found in Firestore.")
                        // Ensure original fields are empty if no document exists
                        originalDOB = ""; originalHouseBuildingName = ""; originalAreaColony = ""; originalPincode = ""; originalState = ""; originalCity = "";
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading profile details from Firestore", e)
                    Toast.makeText(this, "Failed to load address/DOB details.", Toast.LENGTH_SHORT).show()
                    originalDOB = ""; originalHouseBuildingName = ""; originalAreaColony = ""; originalPincode = ""; originalState = ""; originalCity = "";
                }
                .addOnCompleteListener {
                    showLoading(false) // Hide loading indicator after all load attempts
                }
        } ?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            showLoading(false)
            finish()
        }
    }

    private fun setupDOBField() {
        etDOB.isFocusable = false
        etDOB.isClickable = true
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year); calendar.set(Calendar.MONTH, monthOfYear); calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDOBInView()
        }
        etDOB.setOnClickListener {
            DatePickerDialog(this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun updateDOBInView() {
        etDOB.setText(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(calendar.time))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (progressBar.visibility == View.GONE) finish()
                true
            }
            R.id.action_save_profile -> {
                if (progressBar.visibility == View.GONE) saveUserProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        etFirstName.isEnabled = !isLoading
        etLastName.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        btnChangeProfilePicture.isEnabled = !isLoading
        etDOB.isEnabled = !isLoading
        etHouseBuildingName.isEnabled = !isLoading
        etAreaColony.isEnabled = !isLoading
        etPincode.isEnabled = !isLoading
        etState.isEnabled = !isLoading
        etCity.isEnabled = !isLoading
    }

    private fun operationCompleted(success: Boolean, operationType: String) {
        if (success) {
            successfulOperations++
            Log.d(TAG, "$operationType successful.")
            if (operationType == "Profile photo update") {
                auth.currentUser?.reload()?.addOnCompleteListener { reloadTask ->
                    if (reloadTask.isSuccessful) {
                        loadUserProfile() // Reload the user profile to show the new image
                    }
                }
            }
        } else {
            failedOperations++
            Log.e(TAG, "$operationType failed.")
        }

        if ((successfulOperations + failedOperations) == pendingOperations) {
            showLoading(false)
            if (failedOperations > 0) {
                Toast.makeText(this, "Some changes failed. Check logs for details.", Toast.LENGTH_LONG).show()
            } else if (successfulOperations > 0) {
                finishActivityWithMessage("Profile updated successfully.")
            }
            newImageSelectedForUploadUri = null
            pendingOperations = 0
            successfulOperations = 0
            failedOperations = 0
        }
    }

    private fun uploadProfileImageAndUpdateAuth(imageUri: Uri, user: FirebaseUser, onComplete: (success: Boolean) -> Unit) {
        val userId = user.uid
        val photoRef = storage.reference.child("profile_images/$userId.jpg")

        photoRef.putFile(imageUri)
            .addOnSuccessListener { uploadTask ->
                Log.d(TAG, "Image uploaded to Storage: ${uploadTask.metadata?.path}")
                photoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    Log.d(TAG, "Got download URL: $downloadUrl")
                    val profileUpdates = UserProfileChangeRequest.Builder().setPhotoUri(downloadUrl).build()
                    user.updateProfile(profileUpdates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onComplete(true)
                        } else {
                            Log.e(TAG, "Auth photoUrl update failed.", task.exception)
                            runOnUiThread {
                                Toast.makeText(this, "Failed to update profile picture. Please try again.", Toast.LENGTH_LONG).show()
                            }
                            onComplete(false)
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Getting download URL failed.", e)
                    runOnUiThread {
                        Toast.makeText(this, "Failed to get image URL. Check network and Firebase rules.", Toast.LENGTH_LONG).show()
                    }
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Image upload to Storage failed.", e)
                runOnUiThread {
                    Toast.makeText(this, "Image upload failed. Check network and Firebase rules.", Toast.LENGTH_LONG).show()
                }
                onComplete(false)
            }
    }

    private fun saveUserProfile() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()

        if (firstName.isEmpty()) { etFirstName.error = "First name required"; etFirstName.requestFocus(); return }
        if (newEmail.isEmpty()) { etEmail.error = "Email required"; etEmail.requestFocus(); return }
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) { etEmail.error = "Valid email required"; etEmail.requestFocus(); return }

        val currentUser = auth.currentUser ?: run { Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show(); return }

        val newDisplayName = "$firstName $lastName".trim()
        val nameChanged = newDisplayName != originalDisplayName
        val emailChanged = newEmail != originalEmail
        val photoChanged = newImageSelectedForUploadUri != null

        // Get new detail values
        val dob = etDOB.text.toString().trim()
        val houseBuildingName = etHouseBuildingName.text.toString().trim()
        val areaColony = etAreaColony.text.toString().trim()
        val pincode = etPincode.text.toString().trim()
        val state = etState.text.toString().trim()
        val city = etCity.text.toString().trim()

        val detailsChanged = dob != originalDOB ||
                             houseBuildingName != originalHouseBuildingName ||
                             areaColony != originalAreaColony ||
                             pincode != originalPincode ||
                             state != originalState ||
                             city != originalCity

        if (!nameChanged && !emailChanged && !photoChanged && !detailsChanged) {
            Toast.makeText(this, "No changes to save.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        pendingOperations = 0
        successfulOperations = 0
        failedOperations = 0

        if (nameChanged) {
            pendingOperations++
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newDisplayName).build()
            currentUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) originalDisplayName = newDisplayName
                operationCompleted(task.isSuccessful, "Display name update")
            }
        }

        if (emailChanged) {
            pendingOperations++
            currentUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Verification email sent to $newEmail. You will need to re-login after verification.", Toast.LENGTH_LONG).show()
                    // originalEmail will be updated by Firebase upon successful verification and re-login
                } else {
                    etEmail.error = task.exception?.message ?: "Failed to update email"
                    etEmail.requestFocus()
                    Toast.makeText(this, "Failed to start email update: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
                operationCompleted(task.isSuccessful, "Email verification process")
            }
        }

        if (photoChanged) {
            pendingOperations++
            newImageSelectedForUploadUri?.let {
                uploadProfileImageAndUpdateAuth(it, currentUser) { success ->
                    operationCompleted(success, "Profile photo update")
                }
            } ?: operationCompleted(false, "Profile photo update (no URI provided)")
        }

        if (detailsChanged) {
            pendingOperations++
            val userProfileDetails = hashMapOf(
                "dob" to dob,
                "houseBuildingName" to houseBuildingName,
                "areaColony" to areaColony,
                "pincode" to pincode,
                "state" to state,
                "city" to city
            )
            db.collection("users").document(currentUser.uid)
                .set(userProfileDetails, SetOptions.merge())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        originalDOB = dob
                        originalHouseBuildingName = houseBuildingName
                        originalAreaColony = areaColony
                        originalPincode = pincode
                        originalState = state
                        originalCity = city
                    }
                    operationCompleted(task.isSuccessful, "Address/DOB details update")
                }
        }
    }

    private fun finishActivityWithMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500)
    }
}
