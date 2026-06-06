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
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.attendancepro.models.StudentItem
import com.example.attendancepro.models.ManualAttendanceRequest
import com.example.attendancepro.models.MessageResponse

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

    private lateinit var btnConfirmManual: Button
    private var hasModifications = false
    private var studentList: List<StudentItem> = listOf()

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
            
        btnConfirmManual = findViewById(R.id.btnConfirmManual)
        
        btnConfirmManual.setOnClickListener {
            confirmManualChanges()
        }

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
                            studentList = data.students
                            val adapter = StudentAdapter(this@ClassStudentsActivity, studentList)
                            
                            adapter.setOnStatusChangeListener(object : StudentAdapter.OnStatusChangeListener {
                                override fun onStatusChanged() {
                                    if (!hasModifications) {
                                        hasModifications = true
                                        btnConfirmManual.visibility = View.VISIBLE
                                        btnConfirmManual.alpha = 0f
                                        btnConfirmManual.animate().alpha(1f).setDuration(300).start()
                                    }
                                }
                            })
                            
                            recyclerStudents.adapter = adapter

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
    // =========================
    // 💾 CONFIRM MANUAL CHANGES
    // =========================
    private fun confirmManualChanges() {
        btnConfirmManual.isEnabled = false
        btnConfirmManual.text = "Saving..."
        
        val request = ManualAttendanceRequest(classId, studentList)
        
        RetrofitClient.instance.updateManualAttendance(request)
            .enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ClassStudentsActivity, "Attendance updated successfully", Toast.LENGTH_SHORT).show()
                        hasModifications = false
                        btnConfirmManual.animate().alpha(0f).setDuration(300).withEndAction {
                            btnConfirmManual.visibility = View.GONE
                            btnConfirmManual.text = "Confirm Manual Changes"
                            btnConfirmManual.isEnabled = true
                        }.start()
                        loadStudents() // Reload stats
                    } else {
                        Toast.makeText(this@ClassStudentsActivity, "Failed to update", Toast.LENGTH_SHORT).show()
                        btnConfirmManual.isEnabled = true
                        btnConfirmManual.text = "Confirm Manual Changes"
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    Toast.makeText(this@ClassStudentsActivity, t.message, Toast.LENGTH_SHORT).show()
                    btnConfirmManual.isEnabled = true
                    btnConfirmManual.text = "Confirm Manual Changes"
                }
            })
    }
}