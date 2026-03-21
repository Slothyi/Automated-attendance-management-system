package com.example.attendancepro.activities

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var percentageText: TextView
    private lateinit var streakText: TextView
    private lateinit var nameText: TextView
    private lateinit var grid: GridLayout
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContentView(R.layout.activity_profile)

        val session = SessionManager(this)

        nameText = findViewById(R.id.nameText)
        grid = findViewById(R.id.calendarGrid)
        percentageText = findViewById(R.id.percentageText)
        streakText = findViewById(R.id.streakText)
        progressBar = findViewById(R.id.progressBar)

        nameText.text = "Welcome ${session.getName()} 👋"

        loadHistory()
    }

    private fun loadHistory() {

        RetrofitClient.instance.getHistory().enqueue(object : Callback<AttendanceHistoryResponse> {

            override fun onResponse(
                call: Call<AttendanceHistoryResponse>,
                response: Response<AttendanceHistoryResponse>
            ) {
                val list = response.body()?.history ?: emptyList()

                grid.removeAllViews()

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()

                var presentCount = 0

                // ======================
                // 🔥 STREAK CALCULATION
                // ======================
                val sortedList = list.sortedByDescending { it.date }

                var streak = 0
                for (item in sortedList) {
                    if (item.status == "present") {
                        streak++
                    } else break
                }

                // ======================
                // 📊 HEATMAP (28 DAYS)
                // ======================
                for (i in 27 downTo 0) {

                    val cal = calendar.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, -i)

                    val dateStr = sdf.format(cal.time)

                    val record = list.find { it.date == dateStr }
                    val status = record?.status ?: "absent"

                    if (status == "present") presentCount++

                    val box = TextView(this@ProfileActivity)

                    val params = GridLayout.LayoutParams()
                    params.width = 60
                    params.height = 60
                    params.setMargins(6, 6, 6, 6)

                    box.layoutParams = params

                    val color = when (status) {
                        "present" -> Color.parseColor("#2E7D32")
                        else -> Color.parseColor("#E0E0E0")
                    }

                    box.setBackgroundColor(color)

                    grid.addView(box)
                }

                // ======================
                // 🎯 PERCENTAGE + ANIMATION
                // ======================
                val percentage = (presentCount / 28.0 * 100).toInt()
                percentageText.text = "$percentage%"

                val progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 0, percentage)
                progressAnim.duration = 1200
                progressAnim.start()

                // 🔄 rotation animation (premium feel)
                val rotate = ObjectAnimator.ofFloat(progressBar, "rotation", 0f, 360f)
                rotate.duration = 1200
                rotate.start()

                // ======================
                // 🔥 STREAK UI
                // ======================
                streakText.text = "🔥 $streak Day Streak"
                animateStreak(streakText)
            }

            override fun onFailure(call: Call<AttendanceHistoryResponse>, t: Throwable) {
                nameText.append("\nError loading data")
            }
        })
    }

    // ======================
    // 🔥 ADVANCED STREAK ANIMATION
    // ======================
    private fun animateStreak(view: TextView) {

        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.6f, 1f)

        scaleX.duration = 800
        scaleY.duration = 800
        alpha.duration = 800

        scaleX.start()
        scaleY.start()
        alpha.start()

        // 🔥 glow effect
        view.setShadowLayer(20f, 0f, 0f, Color.parseColor("#FF5722"))
    }
}