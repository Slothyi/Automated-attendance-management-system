package com.example.attendancepro.activities

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.attendancepro.R

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // EDGE TO EDGE
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // TRANSPARENT STATUS BAR
        window.statusBarColor = Color.TRANSPARENT

        // WHITE STATUS BAR ICONS
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_role_selection)

        val btnAdmin = findViewById<CardView>(R.id.btnAdmin)
        val btnStudent = findViewById<CardView>(R.id.btnStudent)

        val logoContainer = findViewById<View>(R.id.logoContainer)

        // FLOATING LOGO
        ObjectAnimator.ofFloat(
            logoContainer,
            "translationY",
            -10f,
            10f
        ).apply {

            duration = 3500

            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE

            start()
        }

        // ENTRY ANIMATION
        logoContainer.alpha = 0f
        logoContainer.translationY = -40f

        logoContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(900)
            .start()

        // BUTTON CLICK ANIMATION + NAVIGATION

        btnAdmin.setOnClickListener {

            animateCard(it)

            startActivity(
                Intent(
                    this,
                    AdminLoginActivity::class.java
                )
            )
        }

        btnStudent.setOnClickListener {

            animateCard(it)

            startActivity(
                Intent(
                    this,
                    StudentLoginActivity::class.java
                )
            )
        }
    }

    private fun animateCard(view: View) {

        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(70)
            .withEndAction {

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(70)
                    .start()
            }
            .start()
    }
}