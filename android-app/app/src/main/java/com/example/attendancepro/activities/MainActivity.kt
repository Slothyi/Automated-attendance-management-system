package com.example.attendancepro.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancepro.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val attendanceBtn = findViewById<Button>(R.id.attendanceBtn)

        attendanceBtn.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }
    }
}