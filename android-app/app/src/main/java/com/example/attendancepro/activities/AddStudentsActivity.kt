package com.example.attendancepro.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AddStudentsRequest
import com.example.attendancepro.models.MessageResponse
import com.example.attendancepro.models.StudentData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class AddStudentsActivity : AppCompatActivity() {

    // ✅ CLASS NAME DISPLAY
    private lateinit var tvClassName: TextView

    // ✅ STUDENT INPUTS
    private lateinit var etStudentName: EditText
    private lateinit var etStudentRoll: EditText

    // ✅ BUTTONS
    private lateinit var btnAddStudent: Button
    private lateinit var btnFinish: Button

    // ✅ CLASS DATA
    private var classId: String = ""
    private var className: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
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

        setContentView(R.layout.activity_add_students)

        // ✅ VIEW BINDING
        tvClassName = findViewById(R.id.tvClassName)

        etStudentName = findViewById(R.id.etStudentName)

        etStudentRoll = findViewById(R.id.etStudentRoll)

        btnAddStudent = findViewById(R.id.btnAddStudent)

        btnFinish = findViewById(R.id.btnFinish)

        // ✅ GET CLASS DATA
        classId = intent.getStringExtra(
            "CLASS_ID"
        ) ?: ""

        className = intent.getStringExtra(
            "CLASS_NAME"
        ) ?: ""

        // ✅ SHOW CLASS NAME
        tvClassName.text = className

        // ✅ ADD STUDENT
        btnAddStudent.setOnClickListener {

            addStudent()
        }

        // ✅ FINISH BUTTON
        btnFinish.setOnClickListener {

            finish()
        }
    }

    private fun addStudent() {

        val studentName =
            etStudentName.text.toString().trim()

        val studentRoll =
            etStudentRoll.text.toString().trim()

        // ✅ VALIDATION
        if (
            studentName.isEmpty() ||
            studentRoll.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Fill all fields",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ✅ CREATE STUDENT OBJECT
        val student = StudentData(

            name = studentName,

            roll = studentRoll
        )

        // ✅ CREATE REQUEST
        val request = AddStudentsRequest(

            class_id = classId,

            students = listOf(student)
        )

        RetrofitClient.instance

            .addStudents(request)

            .enqueue(object : Callback<MessageResponse> {

                override fun onResponse(
                    call: Call<MessageResponse>,
                    response: Response<MessageResponse>
                ) {

                    if (
                        response.isSuccessful &&
                        response.body() != null
                    ) {

                        Toast.makeText(
                            this@AddStudentsActivity,
                            "Student Added Successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ✅ CLEAR INPUTS
                        etStudentName.text.clear()

                        etStudentRoll.text.clear()

                        // ✅ FOCUS AGAIN
                        etStudentName.requestFocus()

                    } else {

                        Toast.makeText(
                            this@AddStudentsActivity,
                            "Failed To Add Student",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<MessageResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(
                        this@AddStudentsActivity,
                        t.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}