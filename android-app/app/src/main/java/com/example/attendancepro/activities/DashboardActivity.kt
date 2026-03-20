package com.example.attendancepro.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private lateinit var todayStatus: TextView
    private lateinit var totalAttendance: TextView
    private lateinit var markBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ✅ Bind views
        todayStatus = findViewById(R.id.todayStatus)
        totalAttendance = findViewById(R.id.totalAttendance)
        markBtn = findViewById(R.id.markBtn)

        // ✅ Load data from backend
        loadStatus()

        // ✅ Navigate to Attendance Screen
        markBtn.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }
    }

    private fun loadStatus() {

        RetrofitClient.instance.getStatus()
            .enqueue(object : Callback<AttendanceResponse> {

                override fun onResponse(
                    call: Call<AttendanceResponse>,
                    response: Response<AttendanceResponse>
                ) {
                    val res = response.body()

                    if (res?.status == "present") {

                        todayStatus.text = "Today: Present ✅"

                        markBtn.text = "Already Marked"
                        markBtn.isEnabled = false

                    } else {

                        todayStatus.text = "Today: Not Marked ❌"

                        markBtn.text = "Mark Attendance"
                        markBtn.isEnabled = true
                    }
                }

                override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {

                    todayStatus.text = "Error loading status"

                    Toast.makeText(
                        this@DashboardActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        // 🔄 Refresh when coming back from Attendance screen
        loadStatus()
    }
}