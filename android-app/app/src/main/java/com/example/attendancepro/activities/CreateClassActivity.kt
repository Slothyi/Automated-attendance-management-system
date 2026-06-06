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

class CreateClassActivity : AppCompatActivity() {

    // =========================
    // ✅ INPUT FIELDS
    // =========================
    private lateinit var etClassName: EditText
    private lateinit var etSection: EditText
    private lateinit var etDepartment: EditText
    private lateinit var etYear: EditText
    private lateinit var etSemester: EditText

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
        etClassName =
            findViewById(R.id.etClassName)

        etSection =
            findViewById(R.id.etSection)

        etDepartment =
            findViewById(R.id.etDepartment)

        etYear =
            findViewById(R.id.etYear)

        etSemester =
            findViewById(R.id.etSemester)

        btnCreate =
            findViewById(R.id.btnCreate)

        // =========================
        // ✅ CREATE BUTTON
        // =========================
        btnCreate.setOnClickListener {

            createClass()
        }
    }

    // =========================
    // 🏫 CREATE CLASS
    // =========================
    private fun createClass() {

        val className =

            etClassName.text
                .toString()
                .trim()

        val section =

            etSection.text
                .toString()
                .trim()

        val department =

            etDepartment.text
                .toString()
                .trim()

        val year =

            etYear.text
                .toString()
                .trim()

        val semester =

            etSemester.text
                .toString()
                .trim()

        // =========================
        // ✅ VALIDATION
        // =========================
        if (

            className.isEmpty() ||

            section.isEmpty() ||

            department.isEmpty() ||

            year.isEmpty() ||

            semester.isEmpty()
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

            class_name = className,

            section = section,

            department = department,

            year = year,

            semester = semester,

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
                                className
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
                            className
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