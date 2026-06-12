package com.example.attendancepro.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.CreateClassRequest
import com.example.attendancepro.models.CreateClassResponse
import com.example.attendancepro.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

import android.text.InputFilter

class CreateClassActivity : AppCompatActivity() {

    // =========================
    // ✅ INPUT FIELDS
    // =========================
    private lateinit var etCourseName: EditText
    private lateinit var etCourseCode: EditText
    private lateinit var etSemester: EditText
    private lateinit var etSection: EditText
    private lateinit var etYear: EditText
    private lateinit var etAcademicSession: EditText
    private lateinit var etDepartment: EditText

    // =========================
    // ✅ BUTTON
    // =========================
    private lateinit var btnCreate: Button

    // =========================
    // ✅ SESSION
    // =========================
    private lateinit var sessionManager: SessionManager

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

        setContentView(R.layout.activity_create_class)

        // =========================
        // ✅ SESSION
        // =========================
        sessionManager = SessionManager(this)

        // =========================
        // ✅ VIEW BINDING
        // =========================
        etCourseName = findViewById(R.id.etCourseName)
        etCourseCode = findViewById(R.id.etCourseCode)
        etSemester = findViewById(R.id.etSemester)
        etSection = findViewById(R.id.etSection)
        etYear = findViewById(R.id.etYear)
        etAcademicSession = findViewById(R.id.etAcademicSession)
        etDepartment = findViewById(R.id.etDepartment)

        btnCreate = findViewById(R.id.btnCreate)

        // =========================
        // ✅ INPUT FORMATTING
        // =========================
        val allCapsFilter = arrayOf<InputFilter>(InputFilter.AllCaps())
        etCourseName.filters = allCapsFilter
        etCourseCode.filters = allCapsFilter
        etSection.filters = allCapsFilter
        etDepartment.filters = allCapsFilter

        etSemester.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) etSemester.setText(formatOrdinal(etSemester.text.toString()))
        }
        
        etYear.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) etYear.setText(formatOrdinal(etYear.text.toString()))
        }

        etAcademicSession.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) etAcademicSession.setText(formatAcademicSession(etAcademicSession.text.toString()))
        }

        // =========================
        // ✅ CREATE BUTTON
        // =========================
        btnCreate.setOnClickListener {
            createClass()
        }
    }

    private fun formatOrdinal(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        // Check if already ends with st, nd, rd, th
        if (trimmed.matches(Regex("^[0-9]+(st|nd|rd|th)$", RegexOption.IGNORE_CASE))) {
            return trimmed.lowercase()
        }
        val numStr = trimmed.replace(Regex("[^0-9]"), "")
        if (numStr.isEmpty()) return trimmed // Leaves things like "Final" as is
        
        val number = numStr.toIntOrNull() ?: return trimmed
        val suffix = when {
            number % 100 in 11..13 -> "th"
            number % 10 == 1 -> "st"
            number % 10 == 2 -> "nd"
            number % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$number$suffix"
    }

    private fun formatAcademicSession(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ""
        // Match things like 2022-2027 or 2022/27
        val match = Regex("^([0-9]{4})[^0-9]*([0-9]{2,4})$").find(trimmed)
        if (match != null) {
            val startYear = match.groupValues[1]
            val endYear = match.groupValues[2]
            val shortEnd = if (endYear.length == 4) endYear.substring(2) else endYear
            return "$startYear-$shortEnd"
        }
        // Match just a single 4-digit year, assume 4-year course gap
        val singleYearMatch = Regex("^([0-9]{4})$").find(trimmed)
        if (singleYearMatch != null) {
            val startYear = singleYearMatch.groupValues[1].toInt()
            val endYear = startYear + 4
            return "$startYear-${endYear.toString().substring(2)}"
        }
        return trimmed
    }

    // =========================
    // 🏫 CREATE CLASS
    // =========================
    private fun createClass() {

        val courseName = etCourseName.text.toString().trim().uppercase()
        val courseCode = etCourseCode.text.toString().trim().uppercase()
        val department = etDepartment.text.toString().trim().uppercase()
        val section = etSection.text.toString().trim().uppercase()
        
        // Also format on submit in case they didn't lose focus
        val semester = formatOrdinal(etSemester.text.toString())
        val year = formatOrdinal(etYear.text.toString())
        val academicSession = formatAcademicSession(etAcademicSession.text.toString())

        // =========================
        // ✅ VALIDATION
        // =========================
        if (
            courseName.isEmpty() ||
            courseCode.isEmpty() ||
            semester.isEmpty() ||
            section.isEmpty() ||
            year.isEmpty() ||
            academicSession.isEmpty() ||
            department.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Fill all fields",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // =========================
        // ✅ REQUEST BODY
        // =========================
        val request = CreateClassRequest(
            course_name = courseName,
            course_code = courseCode,
            semester = semester,
            section = section,
            year = year,
            academic_session = academicSession,
            department = department,
            admin_id = sessionManager.getAdminId() ?: ""
        )

        // =========================
        // ✅ API CALL
        // =========================
        RetrofitClient.instance

            .createClass(request)

            .enqueue(object :
                Callback<CreateClassResponse> {

                override fun onResponse(
                    call: Call<CreateClassResponse>,
                    response: Response<CreateClassResponse>
                ) {

                    if (

                        response.isSuccessful &&

                        response.body() != null
                    ) {

                        val data =
                            response.body()!!

                        // =========================
                        // ✅ SAVE LATEST CLASS
                        // =========================
                        sessionManager
                            .saveLatestClassId(
                                data.class_id
                            )

                        sessionManager
                            .saveLatestClassName(
                                courseName
                            )

                        Toast.makeText(
                            this@CreateClassActivity,
                            "Class Created Successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // =========================
                        // ✅ OPEN ADD STUDENTS
                        // =========================
                        val intent = Intent(

                            this@CreateClassActivity,

                            AddStudentsActivity::class.java
                        )

                        // ✅ SEND CLASS ID
                        intent.putExtra(
                            "CLASS_ID",
                            data.class_id
                        )

                        // ✅ SEND CLASS NAME
                        intent.putExtra(
                            "CLASS_NAME",
                            courseName
                        )

                        startActivity(intent)

                        finish()

                    } else {

                        Toast.makeText(
                            this@CreateClassActivity,
                            "Failed To Create Class",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // =========================
                // ❌ API FAILURE
                // =========================
                override fun onFailure(
                    call: Call<CreateClassResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(
                        this@CreateClassActivity,
                        t.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}