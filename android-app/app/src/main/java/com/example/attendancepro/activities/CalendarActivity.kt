package com.example.attendancepro.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.CalendarView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.adapters.CalendarClassAdapter
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.CalendarClass
import com.example.attendancepro.models.CalendarResponse

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarView:
            CalendarView

    private lateinit var rvClasses:
            RecyclerView

    private var allClasses =
        mutableListOf<CalendarClass>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        window.statusBarColor =
            Color.TRANSPARENT

        val controller =
            WindowInsetsControllerCompat(
                window,
                window.decorView
            )

        controller.isAppearanceLightStatusBars =
            false

        setContentView(
            R.layout.activity_calendar
        )

        calendarView =
            findViewById(
                R.id.calendarView
            )

        rvClasses =
            findViewById(
                R.id.rvClasses
            )

        rvClasses.layoutManager =
            LinearLayoutManager(this)

        loadCalendarClasses()

        // =========================
        // 📅 DATE SELECT
        // =========================
        calendarView.setOnDateChangeListener {

                _,
                year,
                month,
                dayOfMonth ->

            val selectedDate = String.format(

                Locale.getDefault(),

                "%04d-%02d-%02d",

                year,

                month + 1,

                dayOfMonth
            )

            filterClasses(
                selectedDate
            )
        }
    }

    // =========================
    // 📚 LOAD CLASSES
    // =========================
    private fun loadCalendarClasses() {

        RetrofitClient.instance

            .getCalendarClasses()

            .enqueue(object :
                Callback<CalendarResponse> {

                override fun onResponse(
                    call: Call<CalendarResponse>,
                    response: Response<CalendarResponse>
                ) {

                    if (
                        response.isSuccessful &&
                        response.body() != null
                    ) {

                        allClasses.clear()

                        allClasses.addAll(

                            response.body()!!.classes
                        )

                        rvClasses.adapter =

                            CalendarClassAdapter(
                                allClasses,
                                ::openAttendanceReport
                            )
                    }
                }

                override fun onFailure(
                    call: Call<CalendarResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(

                        this@CalendarActivity,

                        t.message,

                        Toast.LENGTH_LONG

                    ).show()
                }
            })
    }

    // =========================
    // 📅 FILTER BY DATE
    // =========================
    private fun filterClasses(
        selectedDate: String
    ) {

        val filteredList =
            allClasses.filter {

                try {

                    it.created_at
                        .startsWith(
                            selectedDate
                        )

                } catch (e: Exception) {

                    false
                }
            }

        rvClasses.adapter =

            CalendarClassAdapter(
                filteredList,
                ::openAttendanceReport
            )

        if (
            filteredList.isEmpty()
        ) {

            Toast.makeText(

                this,

                "No classes found",

                Toast.LENGTH_SHORT

            ).show()
        }
    }

    private fun openAttendanceReport(
        calendarClass: CalendarClass
    ) {

        if (calendarClass.class_id.isBlank()) {

            Toast.makeText(
                this,
                "Class ID not found",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val intent = Intent(
            this,
            AttendanceReportActivity::class.java
        )

        intent.putExtra(
            "CLASS_ID",
            calendarClass.class_id
        )

        startActivity(intent)
    }
}
