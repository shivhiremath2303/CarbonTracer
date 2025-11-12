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

class CameraFragment : Fragment() {

    private lateinit var buttonTakePhoto: Button
    private lateinit var buttonChooseGallery: Button
    private lateinit var resultTextView: TextView
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

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
                val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.extras?.get("data") as? Bitmap
                }

                imageBitmap?.let { uploadBitmap(it) }
            }
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uploadImage(it) }
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
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
            resultTextView.text = getString(R.string.error_file_preparation_failed, e.message)
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
            resultTextView.text = getString(R.string.error_bitmap_conversion_failed, e.message)
        }
    }

    private fun uploadFile(body: MultipartBody.Part) {
        Log.d("Upload", "Uploading file...")
        val api = RetrofitClient.instance
        val apiKey = "K86469604988957"

        resultTextView.text = getString(R.string.uploading_image)

        api.uploadOcrImage(apiKey, body).enqueue(object : Callback<OcrResponse> {
            override fun onResponse(call: Call<OcrResponse>, response: Response<OcrResponse>) {
                if (response.isSuccessful) {
                    val ocrResponse = response.body()
                    val parsedText = ocrResponse?.parsedResults?.firstOrNull()?.parsedText ?: "No text found"
                    Log.d("API_SUCCESS", "Text: $parsedText")
                    resultTextView.text = getString(R.string.success_ocr, parsedText)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("API_ERROR", "Response not successful: ${response.code()}")
                    resultTextView.text = getString(R.string.error_api, response.code(), errorBody)
                }
            }

            override fun onFailure(call: Call<OcrResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Upload failed: ${t.message}", t)
                resultTextView.text = getString(R.string.network_failure, t.message)
            }
        })
    }
}
