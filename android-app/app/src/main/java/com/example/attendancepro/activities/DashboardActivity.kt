package com.example.attendancepro.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.attendancepro.R
import com.example.attendancepro.adapters.RecentAttendanceAdapter
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceHistoryResponse
import com.example.attendancepro.models.AttendanceResponse
import com.example.attendancepro.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var markBtnContainer: LinearLayout
    private lateinit var tvMarkTitle: TextView
    private lateinit var userName: TextView
    private lateinit var profileName: TextView
    
    // Stats Views
    private lateinit var percentageText: TextView
    private lateinit var presentCountText: TextView
    private lateinit var absentCountText: TextView
    private lateinit var totalClassesText: TextView
    private lateinit var progressBar: ProgressBar
    
    // Recycler
    private lateinit var rvRecentAttendance: RecyclerView
    
    // Success Anim
    private lateinit var successAnim: LottieAnimationView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up fullscreen and transparent status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_dashboard)

        // Init views
        initViews()
        setupDrawer()
        setupBottomNavigation()
        
        val session = SessionManager(this)
        userName.text = "${session.getName()} 👋"
        profileName.text = session.getName()

        markBtnContainer.setOnClickListener {
            // Navigate to the dedicated AttendanceActivity
            startActivity(Intent(this, AttendanceActivity::class.java))
        }

        loadHistory()
        checkCooldownOnStart()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload history when returning (e.g. after marking attendance)
        loadHistory()
        checkCooldownOnStart()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        markBtnContainer = findViewById(R.id.markBtnContainer)
        tvMarkTitle = findViewById(R.id.tvMarkTitle)
        userName = findViewById(R.id.userName)
        profileName = findViewById(R.id.profileName)
        
        percentageText = findViewById(R.id.percentageText)
        presentCountText = findViewById(R.id.presentCountText)
        absentCountText = findViewById(R.id.absentCountText)
        totalClassesText = findViewById(R.id.totalClassesText)
        progressBar = findViewById(R.id.progressBar)
        
        rvRecentAttendance = findViewById(R.id.rvRecentAttendance)
        rvRecentAttendance.layoutManager = LinearLayoutManager(this)
        
        successAnim = findViewById(R.id.successAnim)
    }

    private fun setupDrawer() {
        findViewById<View>(R.id.profileBtn).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            handler.postDelayed({
                startActivity(Intent(this, ProfileActivity::class.java))
            }, 250)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        // Bottom Nav
        findViewById<View>(R.id.tabClasses).setOnClickListener {
            startActivity(Intent(this, StudentClassesActivity::class.java))
        }
        findViewById<View>(R.id.tabCalendar).setOnClickListener {
            startActivity(Intent(this, StudentCalendarActivity::class.java))
        }
        findViewById<View>(R.id.tabProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun loadHistory() {
        RetrofitClient.instance.getHistory().enqueue(object : Callback<AttendanceHistoryResponse> {
            override fun onResponse(call: Call<AttendanceHistoryResponse>, response: Response<AttendanceHistoryResponse>) {
                val res = response.body()
                if (response.isSuccessful && res != null) {
                    val historyList = res.history
                    
                    // Populate Recycler View (Max 3 items)
                    val recentItems = historyList.take(3)
                    rvRecentAttendance.adapter = RecentAttendanceAdapter(recentItems)
                    
                    // Calculate Stats
                    val totalClasses = historyList.size
                    val presentCount = historyList.count { it.status.equals("present", ignoreCase = true) }
                    val absentCount = totalClasses - presentCount
                    
                    val percentage = if (totalClasses > 0) (presentCount * 100) / totalClasses else 0
                    
                    // Update UI
                    presentCountText.text = presentCount.toString()
                    absentCountText.text = absentCount.toString()
                    totalClassesText.text = totalClasses.toString()
                    percentageText.text = "$percentage%"
                    
                    // Animate Progress Bar
                    val progressAnim = ObjectAnimator.ofInt(progressBar, "progress", 0, percentage)
                    progressAnim.duration = 1000
                    progressAnim.start()
                }
            }
            override fun onFailure(call: Call<AttendanceHistoryResponse>, t: Throwable) {
                Toast.makeText(this@DashboardActivity, "Error loading history", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkCooldownOnStart() {
        RetrofitClient.instance.getStatus().enqueue(object : Callback<AttendanceResponse> {
            override fun onResponse(call: Call<AttendanceResponse>, response: Response<AttendanceResponse>) {
                val res = response.body()
                if (res?.status.equals("Present", ignoreCase = true)) {
                    tvMarkTitle.text = "Already Marked"
                    markBtnContainer.isEnabled = false
                    markBtnContainer.alpha = 0.5f
                } else {
                    tvMarkTitle.text = "Mark Attendance"
                    markBtnContainer.isEnabled = true
                    markBtnContainer.alpha = 1.0f
                }
            }
            override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                // Ignore
            }
        })
    }

    // Optional: Keep Lottie logic if needed for any reason, although actual attendance marking is now in AttendanceActivity
    fun showSuccessAnimation() {
        successAnim.cancelAnimation()
        successAnim.visibility = View.VISIBLE
        successAnim.alpha = 1f
        successAnim.progress = 0f
        successAnim.playAnimation()
        handler.postDelayed({
            successAnim.animate().alpha(0f).setDuration(300).withEndAction {
                successAnim.cancelAnimation()
                successAnim.visibility = View.GONE
                successAnim.alpha = 1f
            }
        }, 1500)
    }
}
