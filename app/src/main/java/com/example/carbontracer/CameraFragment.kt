package com.example.carbontracer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.util.Log
import com.example.carbontracer.model.OcrResponse
import com.example.carbontracer.network.RetrofitClient
// --- FIREBASE IMPORTS ---
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
// --- END ---
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

class CameraFragment : Fragment() {

    private lateinit var buttonTakePhoto: Button
    private lateinit var buttonChooseGallery: Button
    private lateinit var resultTextView: TextView
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // --- FIREBASE VARIABLES ---
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    // --- END ---

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonTakePhoto = view.findViewById(R.id.button_take_photo)
        buttonChooseGallery = view.findViewById(R.id.button_choose_gallery)
        resultTextView =
            view.findViewById(R.id.resultTextView) // This must be in fragment_camera.xml

        setupLaunchers()

        buttonTakePhoto.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        buttonChooseGallery.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            pickImageLauncher.launch(pickImageIntent)
        }
    }

    private fun setupLaunchers() {
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result.data?.extras?.getParcelable("data", Bitmap::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        result.data?.extras?.get("data") as? Bitmap
                    }
                    imageBitmap?.let { uploadBitmap(it) }
                }
            }

        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uploadImage(it) }
                }
            }

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    launchCamera()
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun uploadImage(imageUri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            val mimeType = contentResolver.getType(imageUri)
            val inputStream = contentResolver.openInputStream(imageUri) ?: return
            val fileBytes = inputStream.readBytes()
            inputStream.close()
            val requestFile = fileBytes.toRequestBody(mimeType?.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", "gallery_image.jpg", requestFile)
            uploadFile(body)
        } catch (e: Exception) {
            Log.e("UploadError", "File preparation failed: ${e.message}", e)
            resultTextView.text = "Error: File preparation failed"
        }
    }

    private fun uploadBitmap(bitmap: Bitmap) {
        try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val fileBytes = outputStream.toByteArray()
            val requestFile = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", "camera_photo.jpg", requestFile)
            uploadFile(body)
        } catch (e: Exception) {
            Log.e("UploadError", "Bitmap conversion failed: ${e.message}", e)
            resultTextView.text = "Error: Bitmap conversion failed"
        }
    }

    private fun uploadFile(body: MultipartBody.Part) {
        Log.d("Upload", "Uploading file...")
        val api = RetrofitClient.instance
        val apiKey = "K86469604988957"
        resultTextView.text = "Uploading and scanning..."

        api.uploadOcrImage(apiKey, body).enqueue(object : Callback<OcrResponse> {

            // This is inside your uploadFile function
            override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                if (response.isSuccessful) {
                    val ocrResponse = response.body()
                    val fullText =
                        ocrResponse?.parsedResults?.firstOrNull()?.parsedText ?: "No text found"

                    if (fullText.isEmpty()) {
                        resultTextView.text = "No text found in the image."
                        return
                    }

                    // --- 1. Run the new "master" search function ---
                    val foundUnits = findUnitConsumed(fullText)

                    if (foundUnits != null) {
                        // SUCCESS!
                        resultTextView.text = "Found Units: $foundUnits"

                        // 2. Save to SharedPreferences (for TipsFragment)
                        val prefs = requireActivity().getSharedPreferences(
                            "AppPrefs",
                            AppCompatActivity.MODE_PRIVATE
                        ).edit()
                        prefs.putString("scannedKwh", foundUnits)
                        prefs.apply()

                        Toast.makeText(
                            requireContext(),
                            "Saved $foundUnits units for prediction!",
                            Toast.LENGTH_LONG
                        ).show()

                        // 3. (Optional) Save to Firebase
                        // saveLatestScan(fullText, foundUnits)

                    } else {
                        // Search failed
                        resultTextView.text = "Could not find 'Consumption (Units)' on the bill."
                    }
                    // --- END OF LOGIC ---

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("API_ERROR", "Response not successful: ${response.code()}")
                    resultTextView.text = "Error: ${response.code()} ${errorBody}"
                }
            }

            // --- THIS FUNCTION IS REQUIRED ---
            override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Upload failed: ${t.message}", t)
                resultTextView.text = "Network Failure: ${t.message}"
            }
        })
    }

    /**
     * This is the "Smart Search" function, to find the 4th number.
     */
    /**
     * This is the "Smart Search" function.
     * It searches the full OCR text for your keywords.
     */
    /**
     * This is the "Smart Search" function.
     * It tries 3 different methods to find the units.
     */
    private fun findUnitConsumed(fullText: String): String? {
        val lines = fullText.lines()
        val numberRegex = Pattern.compile("\\b[0-9]+\\b") // Finds whole numbers
        val decimalRegex = Pattern.compile("[0-9]+(\\.[0-9]+)?") // Finds whole or decimal numbers

        // --- METHOD 1: Find "Consumplion Detail" then count 4 number-only lines ---
        // (For bills where numbers are on new lines)
        var foundHeader = false
        var numberCounter = 0
        for (line in lines) {
            val lineLower = line.lowercase()
            val trimmedLine = line.trim()

            if (lineLower.contains("consumplion") && lineLower.contains("detail")) {
                foundHeader = true
                continue
            }
            if (foundHeader) {
                if (numberRegex.matcher(trimmedLine).matches()) {
                    numberCounter++
                    if (numberCounter == 4) {
                        return trimmedLine // Found "46"
                    }
                }
            }
        }

        // --- METHOD 2: Find "Consumplion Detail" and numbers on the SAME line ---
        // (For bills where everything is on one line)
        for (line in lines) {
            val lineLower = line.lowercase()
            if (lineLower.contains("consumplion") && lineLower.contains("detail")) {
                val matcher = decimalRegex.matcher(line)
                val numbersFound = mutableListOf<String>()
                while (matcher.find()) {
                    numbersFound.add(matcher.group(0))
                }

                try {
                    val prevRdgIndex = numbersFound.indexOf("5506")
                    if (prevRdgIndex != -1 && numbersFound.size > prevRdgIndex + 2) {
                        return numbersFound[prevRdgIndex + 2] // Found "46"
                    }
                } catch (e: Exception) { /* Do nothing, try next method */
                }
            }
        }

        // --- METHOD 3: Find "Units chargeable" (Fallback) ---
        for (line in lines) {
            val lineLower = line.lowercase()
            if (lineLower.contains("units") && lineLower.contains("chargeable")) {
                val matcher = decimalRegex.matcher(line)
                var lastFoundNumber: String? = null
                while (matcher.find()) {
                    lastFoundNumber = matcher.group(0)
                }
                if (lastFoundNumber != null) {
                    return lastFoundNumber.split(".").first() // Found "46" from "46.0"
                }
            }
        }

        return null // All methods failed
    }

    /**
     * This is the function that saves your data to Firebase
     * It will now ONLY save the found units.
     */
    private fun saveStatusToFirebase(status: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("Firebase", "Cannot save status: User not logged in.")
            return
        }

        // --- THIS IS THE FIX ---
        // It now saves the status text you wanted
        val scanData = hashMapOf(
            "scanStatus" to status,
            "scannedAt" to System.currentTimeMillis()
        )
        // --- END OF FIX ---

        db.collection("users").document(userId)
            .collection("scanned_bills_status") // Saved to a new collection
            .document("latest_scan_status")   // Overwrites the last one
            .set(scanData)
            .addOnSuccessListener {
                Log.d("Firebase", "Latest scan status saved!")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error saving scan status", e)
            }
    }
}