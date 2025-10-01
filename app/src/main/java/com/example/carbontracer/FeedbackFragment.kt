package com.example.carbontracer

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackFragment : Fragment() {

    private lateinit var etFeedbackTitle: TextInputEditText
    private lateinit var etFeedbackMessage: TextInputEditText
    private lateinit var btnSubmitFeedback: Button
    private lateinit var pbFeedbackLoading: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feedback, container, false)

        etFeedbackTitle = view.findViewById(R.id.etFeedbackTitle)
        etFeedbackMessage = view.findViewById(R.id.etFeedbackMessage)
        btnSubmitFeedback = view.findViewById(R.id.btnSubmitFeedback)
        pbFeedbackLoading = view.findViewById(R.id.pbFeedbackLoading)

        btnSubmitFeedback.setOnClickListener {
            handleSubmitFeedback()
        }

        return view
    }

    private fun handleSubmitFeedback() {
        val title = etFeedbackTitle.text.toString().trim()
        val message = etFeedbackMessage.text.toString().trim()

        if (message.isEmpty()) {
            etFeedbackMessage.error = "Feedback message cannot be empty"
            etFeedbackMessage.requestFocus()
            return
        }

        // Update UI for loading state
        setLoadingState(true)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        val feedbackData = hashMapOf(
            "title" to title,
            "message" to message,
            "userId" to userId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance().collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener {
                Toast.makeText(context, "Feedback submitted successfully! Thank you.", Toast.LENGTH_LONG).show()
                etFeedbackTitle.text = null
                etFeedbackMessage.text = null
                // No need to call setLoadingState(false) here as we are navigating back
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                setLoadingState(false)
                Toast.makeText(context, "Failed to submit feedback: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            pbFeedbackLoading.visibility = View.VISIBLE
            btnSubmitFeedback.visibility = View.INVISIBLE // Hide button when loading
            etFeedbackTitle.isEnabled = false
            etFeedbackMessage.isEnabled = false
        } else {
            pbFeedbackLoading.visibility = View.GONE
            btnSubmitFeedback.visibility = View.VISIBLE // Show button again
            etFeedbackTitle.isEnabled = true
            etFeedbackMessage.isEnabled = true
        }
    }
}
