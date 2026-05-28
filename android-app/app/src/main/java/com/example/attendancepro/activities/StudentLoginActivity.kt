package com.example.attendancepro.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.LoginRequest
import com.example.attendancepro.models.LoginResponse
import com.example.attendancepro.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class StudentLoginActivity : AppCompatActivity() {

    // =========================
    // ✅ VIEWS
    // =========================
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    private lateinit var btnLogin: Button

    private lateinit var tvRegister: TextView

    // =========================
    // ✅ SESSION
    // =========================
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // EDGE TO EDGE
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // TRANSPARENT STATUS BAR
        window.statusBarColor = Color.TRANSPARENT

        // WHITE STATUS BAR ICONS
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        setContentView(
            R.layout.activity_student_login
        )

        // =========================
        // ✅ SESSION
        // =========================
        sessionManager = SessionManager(this)

        // =========================
        // ✅ VIEW BINDING
        // =========================
        etEmail =
            findViewById(R.id.etEmail)

        etPassword =
            findViewById(R.id.etPassword)

        btnLogin =
            findViewById(R.id.btnLogin)

        tvRegister =
            findViewById(R.id.tvRegister)

        // =========================
        // ✅ LOGIN BUTTON
        // =========================
        btnLogin.setOnClickListener {

            loginStudent()
        }

        // =========================
        // ✅ OPEN REGISTER SCREEN
        // =========================
        tvRegister.setOnClickListener {

            startActivity(

                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }
    }

    // =========================
    // 🔐 LOGIN STUDENT
    // =========================
    private fun loginStudent() {

        val email =

            etEmail.text
                .toString()
                .trim()

        val password =

            etPassword.text
                .toString()
                .trim()

        // =========================
        // ✅ VALIDATION
        // =========================
        if (

            email.isEmpty() ||

            password.isEmpty()
        ) {

            Toast.makeText(

                this,

                "Please fill all fields",

                Toast.LENGTH_SHORT

            ).show()

            return
        }

        // =========================
        // ✅ REQUEST BODY
        // =========================
        val request = LoginRequest(

            email,

            password
        )

        // =========================
        // ✅ API CALL
        // =========================
        RetrofitClient.instance

            .login(request)

            .enqueue(object :
                Callback<LoginResponse> {

                // =========================
                // ✅ API SUCCESS
                // =========================
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {

                    if (

                        response.isSuccessful &&

                        response.body() != null
                    ) {

                        val loginResponse =
                            response.body()!!

                        // =========================
                        // ❌ LOGIN FAILED
                        // =========================
                        if (
                            !loginResponse.success
                        ) {

                            Toast.makeText(

                                this@StudentLoginActivity,

                                loginResponse.message
                                    ?: "Login failed",

                                Toast.LENGTH_SHORT

                            ).show()

                            return
                        }

                        // =========================
                        // ❌ TOKEN NULL
                        // =========================
                        if (
                            loginResponse.token == null
                        ) {

                            Toast.makeText(

                                this@StudentLoginActivity,

                                "Invalid server response",

                                Toast.LENGTH_SHORT

                            ).show()

                            return
                        }

                        // =========================
                        // ✅ SAVE TOKEN
                        // =========================
                        sessionManager.saveToken(

                            loginResponse.token
                        )

                        // =========================
                        // ✅ SAVE NAME
                        // =========================
                        sessionManager.saveName(

                            loginResponse.name ?: ""
                        )

                        // =========================
                        // ✅ SUCCESS MESSAGE
                        // =========================
                        Toast.makeText(

                            this@StudentLoginActivity,

                            "Login Successful",

                            Toast.LENGTH_SHORT

                        ).show()

                        // =========================
                        // ✅ OPEN DASHBOARD
                        // =========================
                        startActivity(

                            Intent(

                                this@StudentLoginActivity,

                                DashboardActivity::class.java
                            )
                        )

                        finish()

                    } else {

                        Toast.makeText(

                            this@StudentLoginActivity,

                            "Invalid Credentials",

                            Toast.LENGTH_SHORT

                        ).show()
                    }
                }

                // =========================
                // ❌ API FAILURE
                // =========================
                override fun onFailure(
                    call: Call<LoginResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(

                        this@StudentLoginActivity,

                        "Server Error: ${t.message}",

                        Toast.LENGTH_LONG

                    ).show()
                }
            })
    }
}