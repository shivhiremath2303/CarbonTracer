package com.example.carbontracer

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import android.widget.Toast
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.util.Log
import com.example.carbontracer.model.OcrResponse
import com.example.carbontracer.network.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.graphics.BitmapFactory

class CameraFragment : Fragment() {

    private lateinit var buttonTakePhoto: Button
    private lateinit var buttonChooseGallery: Button
    private lateinit var resultTextView: TextView
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var cameraImageUri: Uri? = null
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
        resultTextView = view.findViewById(R.id.resultTextView)

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
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // We no longer get the thumbnail.
                // We use the full-size image saved at cameraImageUri
                cameraImageUri?.let { uri ->
                    uploadImage(uri) // Send the full-size photo
                }
            }
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    uploadImage(uri) // Send the gallery photo
                }
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
                // Handle permission denial
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
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
        cameraImageUri = createImageUri() // Create a new URI for the photo
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        }
        takePictureLauncher.launch(takePictureIntent)
    }

    private fun uploadImage(imageUri: Uri) {
        try {
            // This is the FIX: Get the bitmap and correct its rotation
            val correctedBitmap = getCorrectlyRotatedBitmap(imageUri)

            // Now, upload the corrected bitmap using your existing function
            uploadBitmap(correctedBitmap)

        } catch (e: Exception) {
            Log.e("UploadError", "File preparation failed: ${e.message}", e)
            resultTextView.text = "Error preparing image: ${e.message}"
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
        }
    }

    private fun uploadFile(body: MultipartBody.Part) {
        Log.d("Upload", "Uploading file...")
        val api = RetrofitClient.instance
        val apiKey = "K86469604988957" // Make sure to secure this key later!

        // Show a loading message
        resultTextView.text = "Processing bill..."

        api.uploadOcrImage(apiKey, body).enqueue(object : Callback<OcrResponse> {
            override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                if (response.isSuccessful) {
                    val ocrResponse = response.body()
                    val parsedText = ocrResponse?.parsedResults?.firstOrNull()?.parsedText

                    // Check if OCR actually found text
                    if (parsedText.isNullOrBlank()) {
                        resultTextView.text = "Failed: No readable text found in the image."
                    } else {
                        // --- THIS IS THE MAIN CHANGE ---
                        // OCR was successful, now send the text to your backend
                        Log.d("Upload", "OCR success. Saving text to backend...")
                        saveTextToBackend(parsedText)
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    resultTextView.text = getString(R.string.error_api, response.code(), errorBody)
                }
            }

            override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                resultTextView.text = getString(R.string.network_failure, t.message)
            }
        })
    }
    /**
     * Creates a temporary file URI to store the full-resolution camera image.
     */
    private fun createImageUri(): Uri {
        val imageFile = File(requireContext().filesDir, "camera_photo.jpg")
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            imageFile
        )
    }

    /**
     * This is the main fix. It loads a bitmap from a URI and manually
     * corrects its rotation based on EXIF data.
     */
    @Throws(IOException::class)
    private fun getCorrectlyRotatedBitmap(uri: Uri): Bitmap {
        // 1. Get the stream for EXIF data
        val inputStream = requireContext().contentResolver.openInputStream(uri)!!
        val ei = ExifInterface(inputStream)
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        inputStream.close()

        // 2. Get the stream for the bitmap itself
        val bitmapStream = requireContext().contentResolver.openInputStream(uri)!!
        val bitmap = BitmapFactory.decodeStream(bitmapStream)
        bitmapStream.close()

        // 3. Rotate the bitmap if necessary
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    /**
     * Helper function to rotate a bitmap
     */
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }
    private fun saveTextToBackend(rawText: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            resultTextView.text = "Error: User not logged in."
            return
        }

        // Create a data object to save
        val billData = hashMapOf(
            "userId" to userId,
            "rawText" to rawText,
            "timestamp" to Timestamp.now(),
            "status" to "unprocessed" // Your ML model can look for this status
        )

        // Save to a new collection called "bill_uploads"
        db.collection("bill_uploads")
            .add(billData)
            .addOnSuccessListener {
                Log.d("Firestore", "Bill text saved successfully!")

                // --- THIS IS YOUR NEW SUCCESS MESSAGE ---
                resultTextView.text = "Success! Bill uploaded for processing."
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error saving text to backend", e)
                resultTextView.text = "Error saving data. Please try again."
            }
    }
}
