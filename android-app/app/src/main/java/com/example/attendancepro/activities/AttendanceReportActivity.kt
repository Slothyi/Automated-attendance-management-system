package com.example.attendancepro.activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.adapters.AttendanceReportAdapter
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceReportResponse

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class AttendanceReportActivity : AppCompatActivity() {

    private lateinit var tvClassName:
            TextView

    private lateinit var recyclerReport:
            RecyclerView

    private var classId = ""

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        // EDGE TO EDGE
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // TRANSPARENT STATUS BAR
        window.statusBarColor = Color.TRANSPARENT

        // WHITE STATUS BAR ICONS
        val controller =
            WindowInsetsControllerCompat(
                window,
                window.decorView
            )

        controller.isAppearanceLightStatusBars = false

        setContentView(
            R.layout.activity_attendance_report
        )

        tvClassName =
            findViewById(R.id.tvClassName)

        recyclerReport =
            findViewById(R.id.recyclerReport)

        recyclerReport.layoutManager =
            LinearLayoutManager(this)

        classId = intent.getStringExtra(
            "CLASS_ID"
        ) ?: ""

        loadReport()
    }

    private fun loadReport() {

        RetrofitClient.instance

            .getClassAttendanceReport(
                classId
            )

            .enqueue(object :
                Callback<AttendanceReportResponse> {

                override fun onResponse(
                    call: Call<AttendanceReportResponse>,
                    response: Response<AttendanceReportResponse>
                ) {

                    if (
                        response.isSuccessful &&
                        response.body() != null
                    ) {

                        val data = response.body()!!

                        tvClassName.text =
                            "${data.class_name} Report"

                        recyclerReport.adapter =

                            AttendanceReportAdapter(
                                this@AttendanceReportActivity,
                                data.report
                            )

                    } else {

                        Toast.makeText(

                            this@AttendanceReportActivity,

                            "Failed To Load Report",

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<AttendanceReportResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(

                        this@AttendanceReportActivity,

                        t.message,

                        Toast.LENGTH_LONG

                    ).show()
                }
            })
    }
}