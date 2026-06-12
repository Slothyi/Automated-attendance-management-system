package com.example.attendancepro.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.attendancepro.R
import com.example.attendancepro.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ VERY IMPORTANT (fix pink bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false  // dark bg → white icons

        setContentView(R.layout.activity_splash)

        val sessionManager = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({

            val targetIntent = Intent(this, RoleSelectionActivity::class.java)
            
            startActivity(targetIntent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_up)

            finish()

        }, 2000)
    }
}