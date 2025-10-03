package com.example.carbontracer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils // Import AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val badgeManager = BadgeManager()

    private lateinit var ivLengthMet: ImageView
    private lateinit var ivUppercaseMet: ImageView
    private lateinit var ivDigitMet: ImageView
    private lateinit var ivSpecialMet: ImageView

    // Password validation function used by both TextWatcher and sign-up logic
    private fun isPasswordValid(password: String): Boolean {
        var isValid = true
        // Length check
        if (password.length >= 8) {
            ivLengthMet.visibility = View.VISIBLE
        } else {
            ivLengthMet.visibility = View.GONE
            isValid = false
        }

        // Uppercase check
        if (password.any { it.isUpperCase() }) {
            ivUppercaseMet.visibility = View.VISIBLE
        } else {
            ivUppercaseMet.visibility = View.GONE
            isValid = false
        }

        // Digit check
        if (password.any { it.isDigit() }) {
            ivDigitMet.visibility = View.VISIBLE
        } else {
            ivDigitMet.visibility = View.GONE
            isValid = false
        }

        // Special character check
        if (password.any { it in "@#$%^&+=!" }) {
            ivSpecialMet.visibility = View.VISIBLE
        } else {
            ivSpecialMet.visibility = View.GONE
            isValid = false
        }
        return isValid
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = Firebase.auth

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignup)
        val tvLoginRedirect = findViewById<TextView>(R.id.tvLoginRedirect)

        ivLengthMet = findViewById(R.id.ivLengthMet)
        ivUppercaseMet = findViewById(R.id.ivUppercaseMet)
        ivDigitMet = findViewById(R.id.ivDigitMet)
        ivSpecialMet = findViewById(R.id.ivSpecialMet)

        // Load the animation
        val slideInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_bottom)

        // Stagger the animations
        val viewsToAnimate = listOf(etFullName, etEmail, etPassword, etConfirmPassword, btnSignUp, tvLoginRedirect)
        viewsToAnimate.forEachIndexed { index, view ->
            view.startAnimation(slideInAnimation.apply {
                startOffset = (index * 100).toLong() // 100ms delay between each item
            })
        }

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                isPasswordValid(password) // This will update the ticks
            }
        })

        btnSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (fullName.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    if (isPasswordValid(password)) { // Reuse the validation logic that also updates ticks
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    badgeManager.awardNewcomerBadge()
                                    Toast.makeText(baseContext, "Account created.", Toast.LENGTH_SHORT).show()
                                    // TODO: Save fullName to Firebase Firestore or Realtime Database
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                                    finish()
                                } else {
                                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
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

        tvLoginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            // Optionally finish SignUpActivity if you want LoginActivity to be the only one in back stack when navigating from here
            // finish() 
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
