package com.example.attendancepro.activities

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceHistoryResponse
import com.example.attendancepro.models.HistoryItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class StudentCalendarActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvTime: TextView
    private lateinit var statusDot: View
    private lateinit var tvTotalPresent: TextView
    private lateinit var tvTotalAbsent: TextView
    private lateinit var tvTotalClasses: TextView

    // Stores all history keyed by date string "yyyy-MM-dd"
    private val historyMap = mutableMapOf<String, HistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_student_calendar)

        // Views
        calendarView      = findViewById(R.id.calendarView)
        tvSelectedDate    = findViewById(R.id.tvSelectedDate)
        tvStatus          = findViewById(R.id.tvStatus)
        tvStatusBadge     = findViewById(R.id.tvStatusBadge)
        tvTime            = findViewById(R.id.tvTime)
        statusDot         = findViewById(R.id.statusDot)
        tvTotalPresent    = findViewById(R.id.tvTotalPresent)
        tvTotalAbsent     = findViewById(R.id.tvTotalAbsent)
        tvTotalClasses    = findViewById(R.id.tvTotalClasses)

        // Back button
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Date selection listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selected = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            showDateDetail(selected)
        }

        loadHistory()
    }

    // =========================
    // LOAD HISTORY FROM API
    // =========================
    private fun loadHistory() {
        RetrofitClient.instance.getHistory().enqueue(object : Callback<AttendanceHistoryResponse> {

            override fun onResponse(
                call: Call<AttendanceHistoryResponse>,
                response: Response<AttendanceHistoryResponse>
            ) {
                val list = response.body()?.history ?: emptyList()

                historyMap.clear()
                for (item in list) {
                    historyMap[item.date] = item
                }

                // Populate summary
                val present = list.count { it.status.equals("present", ignoreCase = true) }
                val absent  = list.size - present

                tvTotalPresent.text  = present.toString()
                tvTotalAbsent.text   = absent.toString()
                tvTotalClasses.text  = list.size.toString()

                // Show today's detail by default
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
                showDateDetail(today)
            }

            override fun onFailure(call: Call<AttendanceHistoryResponse>, t: Throwable) {
                tvStatus.text = "Error loading data"
            }
        })
    }

    // =========================
    // SHOW DETAIL FOR DATE
    // =========================
    private fun showDateDetail(dateStr: String) {

        // Format date nicely for display e.g. "30 May 2025"
        val parseFmt   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFmt = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val displayDate = try {
            displayFmt.format(parseFmt.parse(dateStr)!!)
        } catch (e: Exception) { dateStr }

        tvSelectedDate.text = displayDate

        val item = historyMap[dateStr]

        when {
            item == null -> {
                // No record
                tvStatus.text = "No record for this date"
                tvStatus.setTextColor(Color.parseColor("#7B8D9E"))
                tvStatusBadge.visibility = View.GONE
                tvTime.text = ""
                statusDot.setBackgroundResource(R.drawable.glow_circle)
            }
            item.status.equals("present", ignoreCase = true) -> {
                tvStatus.text = "Present"
                tvStatus.setTextColor(Color.parseColor("#00C853"))

                tvStatusBadge.visibility = View.VISIBLE
                tvStatusBadge.text       = "✓ Present"
                tvStatusBadge.setTextColor(Color.parseColor("#00C853"))
                tvStatusBadge.setBackgroundColor(Color.parseColor("#1A00C853"))

                tvTime.text = if (!item.time.isNullOrEmpty()) "Time: ${item.time}" else ""

                // Green dot
                statusDot.background = null
                statusDot.setBackgroundColor(Color.parseColor("#00C853"))
            }
            else -> {
                tvStatus.text = "Absent"
                tvStatus.setTextColor(Color.parseColor("#FF3B30"))

                tvStatusBadge.visibility = View.VISIBLE
                tvStatusBadge.text       = "✗ Absent"
                tvStatusBadge.setTextColor(Color.parseColor("#FF3B30"))
                tvStatusBadge.setBackgroundColor(Color.parseColor("#1AFF3B30"))

                tvTime.text = ""

                // Red dot
                statusDot.background = null
                statusDot.setBackgroundColor(Color.parseColor("#FF3B30"))
            }
        }
    }
}
