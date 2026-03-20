package com.example.attendancepro.activities

import android.content.Intent
import android.os.Bundle
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

        val session = SessionManager(this)

        // ✅ AUTO LOGIN → GO TO DASHBOARD
        if (!session.getToken().isNullOrEmpty()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.loginBtn)

        loginBtn.setOnClickListener {

            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            // ✅ VALIDATION
            if (emailText.isEmpty()) {
                email.error = "Enter email"
                return@setOnClickListener
            }

            if (passwordText.isEmpty()) {
                password.error = "Enter password"
                return@setOnClickListener
            }

            val request = LoginRequest(emailText, passwordText)

            // ✅ LOADING STATE
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

                        if (response.isSuccessful) {

                            val token = response.body()?.token

                            if (!token.isNullOrEmpty()) {

                                session.saveToken(token)

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Login Success",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // ✅ GO TO DASHBOARD
                                startActivity(
                                    Intent(
                                        this@LoginActivity,
                                        DashboardActivity::class.java
                                    )
                                )
                                finish()

                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Invalid server response",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Invalid credentials",
                                Toast.LENGTH_SHORT
                            ).show()
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