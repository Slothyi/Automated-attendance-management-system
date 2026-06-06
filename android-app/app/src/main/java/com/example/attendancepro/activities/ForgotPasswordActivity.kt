package com.example.attendancepro.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.ResetPasswordRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    // ── VIEWS ──
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var btnCheckEmail: MaterialButton
    private lateinit var tvEmailStatus: TextView

    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnResetPassword: MaterialButton
    private lateinit var tvBackToLogin: TextView

    // ── STATE ──
    private var verifiedEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_forgot_password)

        // ── BIND VIEWS ──
        etEmail           = findViewById(R.id.etEmail)
        tilEmail          = findViewById(R.id.tilEmail)
        btnCheckEmail     = findViewById(R.id.btnCheckEmail)
        tvEmailStatus     = findViewById(R.id.tvEmailStatus)

        tilNewPassword    = findViewById(R.id.tilNewPassword)
        etNewPassword     = findViewById(R.id.etNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        btnResetPassword  = findViewById(R.id.btnResetPassword)
        tvBackToLogin     = findViewById(R.id.tvBackToLogin)

        // ── CHECK EMAIL BUTTON ──
        btnCheckEmail.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                tilEmail.error = "Please enter your email"
                return@setOnClickListener
            }
            tilEmail.error = null
            checkEmailVerification(email)
        }

        // ── RESET PASSWORD BUTTON ──
        btnResetPassword.setOnClickListener {
            val newPass     = etNewPassword.text.toString().trim()
            val confirmPass = etConfirmPassword.text.toString().trim()

            if (newPass.isEmpty()) {
                tilNewPassword.error = "Enter new password"
                return@setOnClickListener
            }
            if (newPass.length < 6) {
                tilNewPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }
            if (newPass != confirmPass) {
                tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            }
            tilNewPassword.error = null
            tilConfirmPassword.error = null

            doResetPassword(verifiedEmail, newPass)
        }

        // ── BACK TO LOGIN ──
        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    // =========================
    // 🔍 CHECK EMAIL VERIFICATION
    // =========================
    private fun checkEmailVerification(email: String) {
        btnCheckEmail.isEnabled = false
        btnCheckEmail.text = "Checking..."
        tvEmailStatus.text = ""

        RetrofitClient.instance
            .checkVerificationStatus(email)
            .enqueue(object : Callback<Map<String, Any>> {

                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    btnCheckEmail.isEnabled = true
                    btnCheckEmail.text = "Check Email"

                    val body = response.body()
                    val verified = body?.get("verified") as? Boolean ?: false

                    if (verified) {
                        // ✅ Email is verified — unlock password section
                        verifiedEmail = email
                        tvEmailStatus.text = "✓ Email verified!"
                        tvEmailStatus.setTextColor(0xFF10B981.toInt())

                        etEmail.isEnabled = false
                        btnCheckEmail.isEnabled = false

                        etNewPassword.isEnabled = true
                        etConfirmPassword.isEnabled = true
                        btnResetPassword.isEnabled = true

                    } else {
                        // ❌ Not verified
                        tvEmailStatus.text = "✗ Email not verified or not registered"
                        tvEmailStatus.setTextColor(0xFFEF4444.toInt())
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    btnCheckEmail.isEnabled = true
                    btnCheckEmail.text = "Check Email"
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Server error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // =========================
    // 🔑 DO RESET PASSWORD
    // =========================
    private fun doResetPassword(email: String, newPassword: String) {
        btnResetPassword.isEnabled = false
        btnResetPassword.text = "Resetting..."

        val request = ResetPasswordRequest(email, newPassword)

        RetrofitClient.instance
            .resetPassword(request)
            .enqueue(object : Callback<Map<String, Any>> {

                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Reset Password"

                    val body    = response.body()
                    val success = body?.get("success") as? Boolean ?: false
                    val message = body?.get("message") as? String ?: "Unknown error"

                    if (success) {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            "✅ $message",
                            Toast.LENGTH_LONG
                        ).show()
                        // Go back to login
                        finish()
                    } else {
                        Toast.makeText(
                            this@ForgotPasswordActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    btnResetPassword.isEnabled = true
                    btnResetPassword.text = "Reset Password"
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Server error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
