package com.example.attendancepro.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AdminLoginRequest
import com.example.attendancepro.models.AdminLoginResponse
import com.example.attendancepro.utils.SessionManager

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class AdminLoginActivity : AppCompatActivity() {

    // =========================
    // ✅ INPUTS
    // =========================
    private lateinit var etAdminName:
            TextInputEditText

    private lateinit var etAdminEmail:
            TextInputEditText

    private lateinit var etAdminPassword:
            TextInputEditText

    // =========================
    // ✅ BUTTON
    // =========================
    private lateinit var btnAdminLogin:
            MaterialButton

    // =========================
    // ✅ SESSION
    // =========================
    private lateinit var sessionManager:
            SessionManager

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        // EDGE TO EDGE
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // TRANSPARENT STATUS BAR
        window.statusBarColor = Color.TRANSPARENT

        // WHITE STATUS BAR ICONS
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        setContentView(
            R.layout.activity_admin_login
        )

        // =========================
        // ✅ SESSION
        // =========================
        sessionManager = SessionManager(this)

        // =========================
        // ✅ VIEW BINDING
        // =========================
        etAdminName =
            findViewById(R.id.etAdminName)

        etAdminEmail =
            findViewById(R.id.etAdminEmail)

        etAdminPassword =
            findViewById(R.id.etAdminPassword)

        btnAdminLogin =
            findViewById(R.id.btnAdminLogin)

        // =========================
        // ✅ LOGIN CLICK
        // =========================
        btnAdminLogin.setOnClickListener {

            loginAdmin()
        }
    }

    // =========================
    // 🔐 LOGIN ADMIN
    // =========================
    private fun loginAdmin() {

        val name =

            etAdminName.text
                .toString()
                .trim()

        val email =

            etAdminEmail.text
                .toString()
                .trim()

        val password =

            etAdminPassword.text
                .toString()
                .trim()

        // =========================
        // ✅ VALIDATION
        // =========================
        if (

            name.isEmpty() ||

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

        val normalizedName =
            name.uppercase(Locale.ROOT)

        val normalizedEmail =
            email.lowercase(Locale.ROOT)

        if (
            !normalizedEmail.contains("@") ||
            !normalizedEmail.contains(".")
        ) {

            Toast.makeText(

                this,

                "Invalid email address",

                Toast.LENGTH_SHORT

            ).show()

            return
        }

        // =========================
        // ✅ REQUEST
        // =========================
        val request = AdminLoginRequest(

            normalizedName,

            normalizedEmail,

            password
        )

        // =========================
        // ✅ API CALL
        // =========================
        RetrofitClient.instance

            .adminLogin(request)

            .enqueue(

                object :
                    Callback<AdminLoginResponse> {

                    override fun onResponse(

                        call:
                        Call<AdminLoginResponse>,

                        response:
                        Response<AdminLoginResponse>

                    ) {

                        // =========================
                        // ✅ RESPONSE CHECK
                        // =========================
                        if (

                            response.isSuccessful &&

                            response.body() != null

                        ) {

                            val data =
                                response.body()!!

                            // =========================
                            // ❌ INVALID LOGIN
                            // =========================
                            if (
                                data.error != null
                            ) {

                                Toast.makeText(

                                    this@AdminLoginActivity,

                                    data.error,

                                    Toast.LENGTH_LONG

                                ).show()

                                return
                            }

                            // =========================
                            // ❌ TOKEN NULL
                            // =========================
                            if (

                                data.token.isNullOrEmpty()

                            ) {

                                Toast.makeText(

                                    this@AdminLoginActivity,

                                    "Invalid Credentials",

                                    Toast.LENGTH_LONG

                                ).show()

                                return
                            }

                            // =========================
                            // ✅ SAVE TOKEN
                            // =========================
                            sessionManager.saveToken(

                                data.token
                            )

                            // =========================
                            // ✅ SAVE NAME
                            // =========================
                            sessionManager.saveName(

                                data.name ?: normalizedName
                            )

                            // =========================
                            // ✅ SAVE ADMIN ID
                            // =========================
                            data.admin_id?.let {
                                sessionManager.saveAdminId(it)
                            }

                            // =========================
                            // ✅ SUCCESS
                            // =========================
                            Toast.makeText(

                                this@AdminLoginActivity,

                                "Admin Login Successful",

                                Toast.LENGTH_SHORT

                            ).show()

                            // =========================
                            // ✅ OPEN DASHBOARD
                            // =========================
                            startActivity(

                                Intent(

                                    this@AdminLoginActivity,

                                    AdminDashboardActivity::class.java
                                )
                            )

                            finish()

                        } else {

                            // =========================
                            // ❌ FAILED
                            // =========================
                            Toast.makeText(

                                this@AdminLoginActivity,

                                "Invalid Credentials",

                                Toast.LENGTH_LONG

                            ).show()
                        }
                    }

                    override fun onFailure(

                        call:
                        Call<AdminLoginResponse>,

                        t: Throwable

                    ) {

                        Toast.makeText(

                            this@AdminLoginActivity,

                            "Server Error: ${t.message}",

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }
            )
    }
}
