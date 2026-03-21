package com.example.attendancepro.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.LoginRequest
import com.example.attendancepro.models.LoginResponse
import com.example.attendancepro.utils.ParticleView
import com.example.attendancepro.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 EDGE TO EDGE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_login)

        // =========================
        // 🔥 PARTICLES
        // =========================
        val container = findViewById<FrameLayout>(R.id.particleContainer)

        container.addView(
            ParticleView(this),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // =========================
        // UI INIT
        // =========================
        val session = SessionManager(this)

        val email = findViewById<TextInputEditText>(R.id.email)
        val password = findViewById<TextInputEditText>(R.id.password)
        val loginBtn = findViewById<MaterialButton>(R.id.loginBtn)
        val registerBtn = findViewById<TextView>(R.id.registerBtn)

        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // =========================
        // LOGIN CLICK
        // =========================
        loginBtn.setOnClickListener {

            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty()) {
                email.error = "Enter email"
                return@setOnClickListener
            }

            if (passwordText.isEmpty()) {
                password.error = "Enter password"
                return@setOnClickListener
            }

            val request = LoginRequest(emailText, passwordText)

            loginBtn.isEnabled = false
            loginBtn.text = "Logging..."

            RetrofitClient.instance.login(request)
                .enqueue(object : Callback<LoginResponse> {

                    override fun onResponse(
                        call: Call<LoginResponse>,
                        response: Response<LoginResponse>
                    ) {
                        loginBtn.isEnabled = true
                        loginBtn.text = "Login"

                        if (response.isSuccessful && response.body() != null) {

                            val res = response.body()!!

                            if (!res.token.isNullOrEmpty() && !res.name.isNullOrEmpty()) {

                                Log.d("LOGIN_DEBUG", "User: ${res.name}")

                                // 🔥 VERY IMPORTANT FIX
                                session.clearSession()

                                session.saveToken(res.token)
                                session.saveName(res.name)

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Welcome ${res.name}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                                finish()

                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    res.error ?: "Login failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        } else {
                            Toast.makeText(this@LoginActivity, "Login failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        loginBtn.isEnabled = true
                        loginBtn.text = "Login"

                        Toast.makeText(
                            this@LoginActivity,
                            "Error: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }
}