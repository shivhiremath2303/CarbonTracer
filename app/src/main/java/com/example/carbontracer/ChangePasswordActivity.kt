package com.example.carbontracer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmNewPassword: TextInputEditText
    private lateinit var btnUpdatePassword: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var tvLengthCriterion: TextView
    private lateinit var tvUppercaseCriterion: TextView
    private lateinit var tvDigitCriterion: TextView
    private lateinit var tvSpecialCharCriterion: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword)
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword)
        tvLengthCriterion = findViewById(R.id.tvLengthCriterion)
        tvUppercaseCriterion = findViewById(R.id.tvUppercaseCriterion)
        tvDigitCriterion = findViewById(R.id.tvDigitCriterion)
        tvSpecialCharCriterion = findViewById(R.id.tvSpecialCharCriterion)
        auth = FirebaseAuth.getInstance()

        btnUpdatePassword.setOnClickListener {
            updatePassword()
        }

        etNewPassword.addTextChangedListener(passwordWatcher)
    }

    private val passwordWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            validatePassword(s.toString())
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    private fun validatePassword(password: String) {
        val lengthValid = password.length >= 8
        val uppercaseValid = password.any { it.isUpperCase() }
        val digitValid = password.any { it.isDigit() }
        val specialCharValid = password.any { "@#\$%^&+=!".contains(it) }

        updateCriterionView(tvLengthCriterion, lengthValid)
        updateCriterionView(tvUppercaseCriterion, uppercaseValid)
        updateCriterionView(tvDigitCriterion, digitValid)
        updateCriterionView(tvSpecialCharCriterion, specialCharValid)

        btnUpdatePassword.isEnabled = lengthValid && uppercaseValid && digitValid && specialCharValid
    }

    private fun updateCriterionView(textView: TextView, isValid: Boolean) {
        if (isValid) {
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_green, 0, 0, 0)
            textView.setTextColor(ContextCompat.getColor(this, R.color.green))
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cross_red, 0, 0, 0)
            textView.setTextColor(ContextCompat.getColor(this, R.color.red))
        }
    }

    private fun updatePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmNewPassword = etConfirmNewPassword.text.toString().trim()

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmNewPassword) {
            Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Password updated successfully.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error updating password: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}