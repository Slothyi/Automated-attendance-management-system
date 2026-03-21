package com.example.attendancepro.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceResponse
import com.example.attendancepro.models.AttendanceHistoryResponse
import com.example.attendancepro.utils.SessionManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var profileBtn: Button
    private lateinit var toolbar: MaterialToolbar

    private lateinit var welcomeText: TextView
    private lateinit var historyText: TextView
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

        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        profileBtn = findViewById(R.id.profileBtn)

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.open, R.string.close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        profileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        welcomeText = findViewById(R.id.welcomeText)
        historyText = findViewById(R.id.historyText)
        todayStatus = findViewById(R.id.todayStatus)
        totalAttendance = findViewById(R.id.totalAttendance)
        markBtn = findViewById(R.id.markBtn)

        val name = session.getName()
        welcomeText.text = "Welcome ${name ?: "Student"} 👋"

        markBtn.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }

        // BACK
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    startActivity(Intent(this@DashboardActivity, LoginActivity::class.java))
                    finish()
                }
            }
        })
    }

    // 🔥 ALWAYS REFRESH WHEN SCREEN OPENS
    override fun onResume() {
        super.onResume()
        checkLocationAndLoad()
    }

    // 📍 LOCATION CHECK
    private fun checkLocationAndLoad() {

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            loadData()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->

            if (location == null) {
                loadData()
                return@addOnSuccessListener
            }

            val distance = calculateDistance(
                location.latitude,
                location.longitude,
                collegeLat,
                collegeLng
            )

            if (distance > 0.2) {
                session.clearSession() // ✅ FIXED

                Toast.makeText(
                    this,
                    "You left college area. Login again.",
                    Toast.LENGTH_LONG
                ).show()

                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                loadData()
            }
        }
    }

    private fun loadData() {
        loadStatus()
        loadHistory()
    }

    // 🔥 STATUS (FIXED)
    private fun loadStatus() {

        todayStatus.text = "Checking..."

        RetrofitClient.instance.getStatus()
            .enqueue(object : Callback<AttendanceResponse> {

                override fun onResponse(
                    call: Call<AttendanceResponse>,
                    response: Response<AttendanceResponse>
                ) {

                    if (!response.isSuccessful || response.body() == null) {
                        todayStatus.text = "Error ❌"
                        markBtn.isEnabled = true
                        markBtn.text = "Mark Attendance"
                        return
                    }

                    val res = response.body()!!

                    if (res.status == "present") {
                        todayStatus.text = "Present ✅"
                        todayStatus.setTextColor(getColor(R.color.green))

                        markBtn.isEnabled = false
                        markBtn.text = "Already Marked"
                    } else {
                        todayStatus.text = "Absent ❌"
                        todayStatus.setTextColor(getColor(R.color.red))

                        markBtn.isEnabled = true
                        markBtn.text = "Mark Attendance"
                    }
                }

                override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                    todayStatus.text = "Network Error ❌"
                    markBtn.isEnabled = true
                }
            })
    }

    // 📅 HISTORY (FIXED)
    private fun loadHistory() {

        RetrofitClient.instance.getHistory()
            .enqueue(object : Callback<AttendanceHistoryResponse> {

                override fun onResponse(
                    call: Call<AttendanceHistoryResponse>,
                    response: Response<AttendanceHistoryResponse>
                ) {

                    if (!response.isSuccessful || response.body() == null) {
                        historyText.text = "Error loading history"
                        return
                    }

                    val list = response.body()!!.history

                    if (list.isEmpty()) {
                        historyText.text = "No records"
                        totalAttendance.text = "0%"
                        return
                    }

                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

                    val formatted = list.takeLast(7).joinToString("\n") {
                        val date = sdf.parse(it.date)
                        val day = dayFormat.format(date!!)
                        "$day → ${it.status}"
                    }

                    historyText.text = formatted

                    val present = list.count { it.status == "present" }
                    val percentage = (present * 100) / list.size

                    totalAttendance.text = "Total Attendance: $percentage%"
                }

                override fun onFailure(call: Call<AttendanceHistoryResponse>, t: Throwable) {
                    historyText.text = "Network error"
                }
            })
    }

    // 📏 DISTANCE
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {

        val R = 6371
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
}