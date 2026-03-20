package com.example.attendancepro.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.LoginRequest
import com.example.attendancepro.models.LoginResponse
import com.example.attendancepro.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val session = SessionManager(this)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val registerBtn = findViewById<Button>(R.id.registerBtn)

        val token = session.getToken()

        // =========================
        // 🔐 TOKEN EXISTS → CONTINUE MODE
        // =========================
        if (!token.isNullOrEmpty()) {
            loginBtn.text = "Continue"
            registerBtn.visibility = View.VISIBLE
        } else {
            loginBtn.text = "Login"
            registerBtn.visibility = View.VISIBLE
        }

        // =========================
        // 🔘 REGISTER BUTTON
        // =========================
        registerBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // =========================
        // 🔘 LOGIN BUTTON
        // =========================
        loginBtn.setOnClickListener {

            val existingToken = session.getToken()

            // ✅ IF TOKEN EXISTS → SKIP LOGIN API
            if (!existingToken.isNullOrEmpty()) {
                startActivity(Intent(this, DashboardActivity::class.java))
                return@setOnClickListener
            }

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
            loginBtn.text = "Logging in..."

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

                            if (res.token != null) {
                                session.saveToken(res.token)

                                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                                finish()

                            } else {
                                val errorMsg = res.error ?: "Login failed"

                                if (errorMsg == "User not found") {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "New user? Please register first",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@LoginActivity,
                                        errorMsg,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
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