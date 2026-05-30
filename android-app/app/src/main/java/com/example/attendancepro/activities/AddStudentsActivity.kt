package com.example.attendancepro.activities

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import com.example.attendancepro.adapters.GroupSpinnerAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AddStudentsRequest
import com.example.attendancepro.models.MessageResponse
import com.example.attendancepro.models.StudentData
import com.example.attendancepro.models.StudentGroupsResponse
import com.example.attendancepro.models.StudentGroupStudentsResponse
import com.example.attendancepro.utils.SessionManager

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddStudentsActivity : AppCompatActivity() {

    // =========================
    // CLASS INFO
    // =========================
    private lateinit var tvClassName: TextView

    // =========================
    // STUDENT INPUTS
    // =========================
    private lateinit var etStudentName: EditText
    private lateinit var etStudentRoll: EditText

    // =========================
    // BUTTONS
    // =========================
    private lateinit var btnAddStudent: Button
    private lateinit var btnFinish: Button

    // =========================
    // IMPORT GROUP
    // =========================
    private lateinit var spinnerGroups: Spinner
    private lateinit var btnImportGroup: Button

    // =========================
    // CLASS DATA
    // =========================
    private var classId: String = ""
    private var className: String = ""

    private lateinit var sessionManager: SessionManager

    private var groupNames =
        mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {

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
            R.layout.activity_add_students
        )

        // =========================
        // VIEWS
        // =========================
        tvClassName =
            findViewById(R.id.tvClassName)

        etStudentName =
            findViewById(R.id.etStudentName)

        etStudentRoll =
            findViewById(R.id.etStudentRoll)

        btnAddStudent =
            findViewById(R.id.btnAddStudent)

        btnFinish =
            findViewById(R.id.btnFinish)

        spinnerGroups =
            findViewById(R.id.spinnerGroups)

        btnImportGroup =
            findViewById(R.id.btnImportGroup)

        sessionManager =
            SessionManager(this)

        // =========================
        // INTENT DATA
        // =========================
        classId =
            intent.getStringExtra(
                "CLASS_ID"
            )?.trim() ?: ""

        className =
            intent.getStringExtra(
                "CLASS_NAME"
            )?.trim() ?: ""

        if (classId.isNotBlank()) {

            sessionManager.saveLatestClassId(
                classId
            )
        }

        if (className.isNotBlank()) {

            sessionManager.saveLatestClassName(
                className
            )
        }

        tvClassName.text =
            className

        // =========================
        // LOAD GROUPS
        // =========================
        loadStudentGroups()

        // =========================
        // ADD STUDENT
        // =========================
        btnAddStudent.setOnClickListener {

            addStudent()
        }

        // =========================
        // IMPORT GROUP
        // =========================
        btnImportGroup.setOnClickListener {

            if (groupNames.isEmpty()) {

                Toast.makeText(
                    this,
                    "No saved groups found",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val groupName =
                spinnerGroups.selectedItem
                    .toString()

            importGroupStudents(

                groupName

            )
        }

        // =========================
        // FINISH
        // =========================
        btnFinish.setOnClickListener {

            finish()
        }
    }

    // =========================
    // LOAD SAVED GROUPS
    // =========================
    private fun loadStudentGroups() {

        RetrofitClient.instance

            .getStudentGroups()

            .enqueue(
                object :
                    Callback<StudentGroupsResponse> {

                    override fun onResponse(

                        call:
                        Call<StudentGroupsResponse>,

                        response:
                        Response<StudentGroupsResponse>

                    ) {

                        if (
                            response.isSuccessful &&
                            response.body() != null
                        ) {

                            groupNames.clear()

                            response.body()?.groups?.forEach {

                                groupNames.add(
                                    it.group_name
                                )
                            }

                            val adapter = GroupSpinnerAdapter(
                                this@AddStudentsActivity,
                                groupNames
                            )

                            spinnerGroups.adapter = adapter
                        }
                    }

                    override fun onFailure(

                        call:
                        Call<StudentGroupsResponse>,

                        t: Throwable

                    ) {

                    }
                }
            )
    }

    // =========================
    // IMPORT STUDENTS
    // =========================
    private fun importGroupStudents(
        groupName: String
    ) {

        if (!hasSelectedClass()) {

            return
        }

        RetrofitClient.instance

            .getStudentGroup(groupName)

            .enqueue(
                object :
                    Callback<StudentGroupStudentsResponse> {

                    override fun onResponse(

                        call:
                        Call<StudentGroupStudentsResponse>,

                        response:
                        Response<StudentGroupStudentsResponse>

                    ) {

                        if (
                            response.isSuccessful &&
                            response.body() != null
                        ) {

                            val students =
                                response.body()!!
                                    .students

                            if (
                                students.isEmpty()
                            ) {

                                Toast.makeText(
                                    this@AddStudentsActivity,
                                    "No students found",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return
                            }

                            val request =
                                AddStudentsRequest(

                                    class_id =
                                        classId,

                                    students =
                                        students
                                )

                            RetrofitClient.instance

                                .addStudents(
                                    request
                                )

                                .enqueue(
                                    object :
                                        Callback<MessageResponse> {

                                        override fun onResponse(

                                            call:
                                            Call<MessageResponse>,

                                            response:
                                            Response<MessageResponse>

                                        ) {

                                            if (response.isSuccessful) {

                                                val body =
                                                    response.body()

                                                Toast.makeText(

                                                    this@AddStudentsActivity,

                                                    body?.message
                                                        ?: body?.error
                                                        ?: "Import completed",

                                                    Toast.LENGTH_LONG

                                                ).show()

                                            } else {

                                                Toast.makeText(

                                                    this@AddStudentsActivity,

                                                    "Import Failed : ${response.code()}",

                                                    Toast.LENGTH_LONG

                                                ).show()
                                            }
                                        }

                                        override fun onFailure(

                                            call:
                                            Call<MessageResponse>,

                                            t: Throwable

                                        ) {

                                            Toast.makeText(

                                                this@AddStudentsActivity,

                                                t.message,

                                                Toast.LENGTH_LONG

                                            ).show()
                                        }
                                    }
                                )
                        }
                    }

                    override fun onFailure(

                        call:
                        Call<StudentGroupStudentsResponse>,

                        t: Throwable

                    ) {

                        Toast.makeText(

                            this@AddStudentsActivity,

                            t.message,

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }
            )
    }

    // =========================
    // ADD SINGLE STUDENT
    // =========================
    private fun addStudent() {

        if (!hasSelectedClass()) {

            return
        }

        val studentName =

            etStudentName.text
                .toString()
                .trim()

        val studentRoll =

            etStudentRoll.text
                .toString()
                .trim()

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

        val student = StudentData(

            name = studentName,

            roll = studentRoll
        )

        val request =
            AddStudentsRequest(

                class_id = classId,

                students = listOf(student)
            )

        RetrofitClient.instance

            .addStudents(request)

            .enqueue(
                object :
                    Callback<MessageResponse> {

                    override fun onResponse(

                        call:
                        Call<MessageResponse>,

                        response:
                        Response<MessageResponse>

                    ) {

                        if (response.isSuccessful) {

                            val body =
                                response.body()

                            Toast.makeText(
                                this@AddStudentsActivity,
                                body?.message
                                    ?: body?.error
                                    ?: "Done",
                                Toast.LENGTH_LONG
                            ).show()

                            etStudentName.text.clear()
                            etStudentRoll.text.clear()
                            etStudentName.requestFocus()

                        } else {

                            Toast.makeText(
                                this@AddStudentsActivity,
                                "Error: ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(

                        call:
                        Call<MessageResponse>,

                        t: Throwable

                    ) {

                        Toast.makeText(

                            this@AddStudentsActivity,

                            t.message,

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }
            )
    }

    private fun hasSelectedClass(): Boolean {

        if (classId.isNotBlank()) {

            return true
        }

        Toast.makeText(
            this,
            "Select a class first",
            Toast.LENGTH_LONG
        ).show()

        return false
    }
}
