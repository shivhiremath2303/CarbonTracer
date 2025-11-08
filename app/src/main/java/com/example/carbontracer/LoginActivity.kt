package com.example.carbontracer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View // Import View
import android.view.animation.AnimationUtils // Import AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ActionCodeSettings

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for password reset link
        handleIncomingIntent(intent)

        auth = Firebase.auth // Initialize auth

        // If user is already logged in, go to MainActivity
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
            return
        }

        // If no user, show the login UI
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUpRedirect = findViewById<TextView>(R.id.tvSignUpRedirect)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        
        val slideInAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_bottom)
        val viewsToAnimate = listOf(etEmail, etPassword, btnLogin, tvSignUpRedirect, tvForgotPassword)
        viewsToAnimate.forEachIndexed { index, view ->
            view.startAnimation(slideInAnimation.apply { 
                startOffset = (index * 100).toLong()
            })
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(baseContext, "Login successful.", Toast.LENGTH_SHORT).show()
                            val mainIntent = Intent(this, MainActivity::class.java)
                            mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(mainIntent)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                            finish()
                        } else {
                            val errorMessage = task.exception?.message ?: "Unknown error."
                            Log.w(TAG, "signInWithEmail:failure", task.exception)
                            Toast.makeText(baseContext, "Authentication failed: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        tvSignUpRedirect.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val action: String? = intent?.action
        val data: android.net.Uri? = intent?.data

        if (action == Intent.ACTION_VIEW && data != null) {
            val oobCode = data.getQueryParameter("oobCode")
            if (oobCode != null) {
                auth.checkActionCode(oobCode).addOnSuccessListener {
                    val resetIntent = Intent(this, ResetPasswordActivity::class.java).apply {
                        putExtra("actionCode", oobCode)
                    }
                    startActivity(resetIntent)
                    finish()
                }.addOnFailureListener {
                    Toast.makeText(this, "Invalid or expired password reset link.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Password")
        val view = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val etEmailDialog = view.findViewById<EditText>(R.id.etEmailDialog)
        builder.setView(view)
        builder.setPositiveButton("Send") { _, _ ->
            val email = etEmailDialog.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun sendPasswordResetEmail(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://carbon-tracker-app-5433f.firebaseapp.com") // URL is a placeholder, but required
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                packageName,
                true, /* installIfNotAvailable */
                null /* minimumVersion */
            )
            .build()

        auth.sendPasswordResetEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    AlertDialog.Builder(this)
                        .setTitle("Password Reset Email Sent")
                        .setMessage("Please check your email inbox to reset your password. If you don't see it, be sure to check your spam folder.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to send reset email."
                    Log.w(TAG, "sendPasswordResetEmail:failure", task.exception)
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
