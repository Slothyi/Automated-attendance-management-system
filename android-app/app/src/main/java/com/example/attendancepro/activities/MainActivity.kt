package com.example.attendancepro.activities

import android.content.Intent
import com.example.attendancepro.models.AttendanceResponse
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        val token = sessionManager.getToken()

        // ❌ If not logged in → go to Log in
        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val attendanceBtn = findViewById<Button>(R.id.attendanceBtn)
        val logoutBtn = findViewById<Button>(R.id.logoutBtn)

        // 🔄 Check attendance status
        RetrofitClient.instance.getStatus().enqueue(object : Callback<AttendanceResponse> {
            override fun onResponse(
                call: Call<AttendanceResponse>,
                response: Response<AttendanceResponse>
            ) {
                val status = response.body()?.status

                if (status == "present") {
                    statusText.text = "Present ✅"
                    attendanceBtn.text = "Already Marked"
                    attendanceBtn.isEnabled = false
                } else {
                    statusText.text = "Absent ❌"
                }
            }

            override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                statusText.text = "Error loading status"
            }
        })

        // 📸 Go to attendance
        attendanceBtn.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }

        // 🚪 Logout
        logoutBtn.setOnClickListener {
            sessionManager.clearToken()

            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}