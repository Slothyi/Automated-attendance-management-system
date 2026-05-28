package com.example.attendancepro.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.adapters.StudentAdapter
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.ClassStudentsResponse

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class ClassStudentsActivity : AppCompatActivity() {

    // =========================
    // ✅ CLASS INFO
    // =========================
    private lateinit var tvClassName:
            TextView

    private lateinit var tvClassId:
            TextView

    private lateinit var tvStats:
            TextView

    // =========================
    // ✅ BUTTON
    // =========================
    private lateinit var btnViewReport:
            Button

    // =========================
    // ✅ RECYCLER
    // =========================
    private lateinit var recyclerStudents:
            RecyclerView

    // =========================
    // ✅ CLASS ID
    // =========================
    private var classId: String = ""

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
            R.layout.activity_class_details
        )

        // =========================
        // ✅ VIEW BINDING
        // =========================
        tvClassName =
            findViewById(R.id.tvClassName)

        tvClassId =
            findViewById(R.id.tvClassId)

        tvStats =
            findViewById(R.id.tvStats)

        btnViewReport =
            findViewById(R.id.btnViewReport)

        recyclerStudents =
            findViewById(R.id.recyclerStudents)

        // =========================
        // ✅ RECYCLER
        // =========================
        recyclerStudents.layoutManager =

            LinearLayoutManager(this)

        // =========================
        // ✅ GET CLASS ID
        // =========================
        classId = intent.getStringExtra(
            "CLASS_ID"
        ) ?: ""

        tvClassId.text =
            "Class ID: $classId"

        // =========================
        // ✅ LOAD STUDENTS
        // =========================
        loadStudents()

        // =========================
        // 📊 VIEW REPORT
        // =========================
        btnViewReport.setOnClickListener {

            val intent = Intent(

                this,

                AttendanceReportActivity::class.java
            )

            intent.putExtra(
                "CLASS_ID",
                classId
            )

            startActivity(intent)
        }
    }

    // =========================
    // 👨‍🎓 LOAD STUDENTS
    // =========================
    private fun loadStudents() {

        RetrofitClient.instance

            .getClassStudents(classId)

            .enqueue(

                object :
                    Callback<ClassStudentsResponse> {

                    override fun onResponse(

                        call:
                        Call<ClassStudentsResponse>,

                        response:
                        Response<ClassStudentsResponse>

                    ) {

                        if (

                            response.isSuccessful &&

                            response.body() != null

                        ) {

                            val data =
                                response.body()!!

                            // =========================
                            // ✅ CLASS NAME
                            // =========================
                            tvClassName.text =

                                data.class_name

                            // =========================
                            // ✅ STATS
                            // =========================
                            tvStats.text =

                                "Present: ${data.present_students}    " +

                                        "Absent: ${data.absent_students}    " +

                                        "N/A: ${data.na_students}"

                            // =========================
                            // ✅ RECYCLER
                            // =========================
                            recyclerStudents.adapter =

                                StudentAdapter(

                                    this@ClassStudentsActivity,

                                    data.students
                                )

                        } else {

                            Toast.makeText(

                                this@ClassStudentsActivity,

                                "Failed To Load Students",

                                Toast.LENGTH_LONG

                            ).show()
                        }
                    }

                    override fun onFailure(

                        call:
                        Call<ClassStudentsResponse>,

                        t: Throwable

                    ) {

                        Toast.makeText(

                            this@ClassStudentsActivity,

                            t.message,

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }
            )
    }
}