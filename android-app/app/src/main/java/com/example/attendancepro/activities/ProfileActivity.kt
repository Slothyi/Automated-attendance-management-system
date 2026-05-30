package com.example.attendancepro.activities

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceHistoryResponse
import com.example.attendancepro.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var nameText: TextView
    private lateinit var tvPresentCount: TextView
    private lateinit var tvAbsentCount: TextView
    private lateinit var percentageText: TextView
    private lateinit var tvProgressPct: TextView
    private lateinit var tvAttendanceLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var streakText: TextView
    private lateinit var grid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent, edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_profile)

        // Back
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Views
        nameText           = findViewById(R.id.nameText)
        tvPresentCount     = findViewById(R.id.tvPresentCount)
        tvAbsentCount      = findViewById(R.id.tvAbsentCount)
        percentageText     = findViewById(R.id.percentageText)
        tvProgressPct      = findViewById(R.id.tvProgressPct)
        tvAttendanceLabel  = findViewById(R.id.tvAttendanceLabel)
        progressBar        = findViewById(R.id.progressBar)
        streakText         = findViewById(R.id.streakText)
        grid               = findViewById(R.id.calendarGrid)

        val session = SessionManager(this)
        nameText.text = session.getName() ?: "Student"

        loadHistory()
    }

    private fun loadHistory() {
        RetrofitClient.instance.getHistory().enqueue(object : Callback<AttendanceHistoryResponse> {

            override fun onResponse(
                call: Call<AttendanceHistoryResponse>,
                response: Response<AttendanceHistoryResponse>
            ) {
                val list = response.body()?.history ?: emptyList()

                // ── Counts ──────────────────────────────────────
                val presentCount = list.count { it.status.equals("present", ignoreCase = true) }
                val absentCount  = list.size - presentCount
                val total        = list.size

                tvPresentCount.text = presentCount.toString()
                tvAbsentCount.text  = absentCount.toString()

                val pct = if (total > 0) (presentCount * 100) / total else 0
                percentageText.text = "$pct%"
                tvProgressPct.text  = "$pct%"

                // Motivational label
                tvAttendanceLabel.text = when {
                    pct >= 90 -> "Excellent! Keep it up 🎉"
                    pct >= 75 -> "Good standing ✓"
                    pct >= 60 -> "Attendance is borderline ⚠️"
                    else      -> "Attendance too low — attend more classes!"
                }

                // Animated progress bar
                ObjectAnimator.ofInt(progressBar, "progress", 0, pct).apply {
                    duration = 1000
                    start()
                }

                // ── Streak ──────────────────────────────────────
                val sorted = list.sortedByDescending { it.date }
                var streak = 0
                for (item in sorted) {
                    if (item.status.equals("present", ignoreCase = true)) streak++ else break
                }
                streakText.text = "$streak Day Streak"
                if (streak > 0) animateStreak(streakText)

                // ── 28-day Heatmap ──────────────────────────────
                buildHeatmap(list)
            }

            override fun onFailure(call: Call<AttendanceHistoryResponse>, t: Throwable) {
                nameText.append("\nError loading data")
            }
        })
    }

    // ═══════════════════════════════════
    // BUILD 28-DAY HEATMAP GRID
    // ═══════════════════════════════════
    private fun buildHeatmap(list: List<com.example.attendancepro.models.HistoryItem>) {
        grid.removeAllViews()

        val sdf      = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val density  = resources.displayMetrics.density
        val boxSize  = (36 * density).toInt()
        val margin   = (4  * density).toInt()
        val radius   = (8  * density)

        for (i in 27 downTo 0) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(cal.time)

            val record = list.find { it.date == dateStr }
            val status = record?.status ?: "none"

            val box = View(this)
            val params = GridLayout.LayoutParams().apply {
                width  = boxSize
                height = boxSize
                setMargins(margin, margin, margin, margin)
            }
            box.layoutParams = params

            // Rounded rect background via ShapeDrawable
            val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = radius
                setColor(when {
                    status.equals("present", ignoreCase = true) -> Color.parseColor("#00C853")
                    status == "none"                             -> Color.parseColor("#1A2E3D")
                    else                                         -> Color.parseColor("#FF3B30")
                })
            }
            box.background = bgDrawable

            // Fade in each box with a slight stagger
            box.alpha = 0f
            box.animate()
                .alpha(1f)
                .setStartDelay((27L - i) * 20L)
                .setDuration(200)
                .start()

            grid.addView(box)
        }
    }

    // ═══════════════════════════════════
    // STREAK PULSE ANIMATION
    // ═══════════════════════════════════
    private fun animateStreak(view: TextView) {
        ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.15f, 1f).apply { duration = 700; start() }
        ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.15f, 1f).apply { duration = 700; start() }
    }
}