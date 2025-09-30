package com.example.carbontracer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class CameraFragment : Fragment() {

    private lateinit var buttonTakePhoto: Button
    private lateinit var buttonChooseGallery: Button
    // Add an ImageView to display the selected or captured image (optional)
    // private lateinit var imageView: ImageView 

    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonTakePhoto = view.findViewById(R.id.button_take_photo)
        buttonChooseGallery = view.findViewById(R.id.button_choose_gallery)
        // imageView = view.findViewById(R.id.image_view_placeholder) // Initialize if you added an ImageView

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                // Handle the captured imageBitmap (e.g., display it in an ImageView)
                // imageView.setImageBitmap(imageBitmap) 
            }
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                // Handle the selected imageUri (e.g., display it in an ImageView)
                // imageView.setImageURI(imageUri)
            }
        }

        buttonTakePhoto.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                takePictureLauncher.launch(takePictureIntent)
            } catch (e: Exception) {
                // Handle exception, e.g., if no camera app is available
            }
        }

        buttonChooseGallery.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageIntent.type = "image/*" 
            try {
                pickImageLauncher.launch(pickImageIntent)
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }
}