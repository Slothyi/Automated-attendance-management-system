package com.example.attendancepro.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceResponse
import com.example.attendancepro.utils.SessionManager
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private lateinit var todayStatus: TextView
    private lateinit var totalAttendance: TextView
    private lateinit var markBtn: Button

    private lateinit var session: SessionManager

    private val collegeLat = 23.526515
    private val collegeLng = 87.742507

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        session = SessionManager(this)

        todayStatus = findViewById(R.id.todayStatus)
        totalAttendance = findViewById(R.id.totalAttendance)
        markBtn = findViewById(R.id.markBtn)

        todayStatus.text = "Checking..."

        checkLocationAndLoad()

        markBtn.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }
    }

    // ============================
    // 📍 LOCATION CHECK (IMPORTANT)
    // ============================
    private fun checkLocationAndLoad() {

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            loadStatus() // fallback
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (location != null) {

                val distance = calculateDistance(
                    location.latitude,
                    location.longitude,
                    collegeLat,
                    collegeLng
                )

                if (distance > 0.2) {
                    // ❌ OUTSIDE CAMPUS → LOGOUT
                    session.clearToken()

                    Toast.makeText(
                        this,
                        "You left college area. Login again.",
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    loadStatus()
                }

            } else {
                loadStatus()
            }
        }
    }

    // ============================
    // 📊 LOAD STATUS FROM BACKEND
    // ============================
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

    // ============================
    // 📏 DISTANCE CALCULATION
    // ============================
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {

        val R = 6371 // km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) *
                Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    override fun onResume() {
        super.onResume()
        checkLocationAndLoad()
    }
}