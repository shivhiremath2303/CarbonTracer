package com.example.carbontracer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var ivLengthMet: ImageView
    private lateinit var ivUppercaseMet: ImageView
    private lateinit var ivDigitMet: ImageView
    private lateinit var ivSpecialMet: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()

        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)

        ivLengthMet = findViewById(R.id.ivLengthMet)
        ivUppercaseMet = findViewById(R.id.ivUppercaseMet)
        ivDigitMet = findViewById(R.id.ivDigitMet)
        ivSpecialMet = findViewById(R.id.ivSpecialMet)

        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                isPasswordValid(password)
            }
        })

        btnResetPassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val actionCode = intent.getStringExtra("actionCode")

            if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (newPassword == confirmPassword) {
                    if (isPasswordValid(newPassword)) {
                        if (actionCode != null) {
                            auth.confirmPasswordReset(actionCode, newPassword)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val intent = Intent(this, PasswordSuccessActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Failed to reset password: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Invalid password reset link.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Password does not meet all requirements.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        var isLengthMet = false
        var isUppercaseMet = false
        var isDigitMet = false
        var isSpecialMet = false

        // Length check
        if (password.length >= 8) {
            ivLengthMet.setImageResource(R.drawable.ic_check_green)
            ivLengthMet.visibility = View.VISIBLE
            isLengthMet = true
        } else {
            ivLengthMet.setImageResource(R.drawable.ic_check_red)
            ivLengthMet.visibility = View.VISIBLE
        }

        // Uppercase check
        if (password.any { it.isUpperCase() }) {
            ivUppercaseMet.setImageResource(R.drawable.ic_check_green)
            ivUppercaseMet.visibility = View.VISIBLE
            isUppercaseMet = true
        } else {
            ivUppercaseMet.setImageResource(R.drawable.ic_check_red)
            ivUppercaseMet.visibility = View.VISIBLE
        }

        // Digit check
        if (password.any { it.isDigit() }) {
            ivDigitMet.setImageResource(R.drawable.ic_check_green)
            ivDigitMet.visibility = View.VISIBLE
            isDigitMet = true
        } else {
            ivDigitMet.setImageResource(R.drawable.ic_check_red)
            ivDigitMet.visibility = View.VISIBLE
        }

        // Special character check
        if (password.any { it in "@#$%^&+=!" }) {
            ivSpecialMet.setImageResource(R.drawable.ic_check_green)
            ivSpecialMet.visibility = View.VISIBLE
            isSpecialMet = true
        } else {
            ivSpecialMet.setImageResource(R.drawable.ic_check_red)
            ivSpecialMet.visibility = View.VISIBLE
        }
        return isLengthMet && isUppercaseMet && isDigitMet && isSpecialMet
    }
}