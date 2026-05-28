package com.example.attendancepro.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendancepro.R
import com.example.attendancepro.adapters.ClassAdapter
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.ClassItem
import com.example.attendancepro.models.ClassesResponse
import com.example.attendancepro.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity() {

    // =========================
    // BUTTONS
    // =========================
    private lateinit var btnCreateClass: Button
    private lateinit var btnAddStudents: Button
    private lateinit var btnLogout: Button

    // =========================
    // RECYCLER VIEW
    // =========================
    private lateinit var recyclerClasses: RecyclerView

    // =========================
    // SESSION
    // =========================
    private lateinit var sessionManager: SessionManager

    // =========================
    // CLASS LIST
    // =========================
    private var classList: List<ClassItem> = listOf()

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

        setContentView(R.layout.activity_admin_dashboard)

        // SESSION
        sessionManager = SessionManager(this)

        // VIEW BINDING
        btnCreateClass =
            findViewById(R.id.btnCreateClass)

        btnAddStudents =
            findViewById(R.id.btnAddStudents)

        btnLogout =
            findViewById(R.id.btnLogout)

        recyclerClasses =
            findViewById(R.id.recyclerClasses)

        // HORIZONTAL LIST
        recyclerClasses.layoutManager =

            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // LOAD CLASSES
        loadClasses()

        // CREATE CLASS
        btnCreateClass.setOnClickListener {

            startActivity(

                Intent(
                    this,
                    CreateClassActivity::class.java
                )
            )
        }

        // ADD STUDENTS
        btnAddStudents.setOnClickListener {

            val latestClassId =

                sessionManager.getLatestClassId()

            val latestClassName =

                sessionManager.getLatestClassName()

            // NO CLASS CREATED
            if (latestClassId.isNullOrEmpty()) {

                Toast.makeText(
                    this,
                    "Create a class first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // OPEN ADD STUDENTS
            val intent = Intent(

                this,
                AddStudentsActivity::class.java
            )

            intent.putExtra(
                "CLASS_ID",
                latestClassId
            )

            intent.putExtra(
                "CLASS_NAME",
                latestClassName
            )

            startActivity(intent)
        }

        // LOGOUT
        btnLogout.setOnClickListener {

            sessionManager.clearSession()

            startActivity(

                Intent(
                    this,
                    RoleSelectionActivity::class.java
                )
            )

            finishAffinity()
        }
    }

    // LOAD CLASSES
    private fun loadClasses() {

        RetrofitClient.instance

            .getAllClasses()

            .enqueue(object : Callback<ClassesResponse> {

                override fun onResponse(
                    call: Call<ClassesResponse>,
                    response: Response<ClassesResponse>
                ) {

                    // SUCCESS
                    if (
                        response.isSuccessful &&
                        response.body() != null
                    ) {

                        classList =
                            response.body()!!.classes

                        // SAVE LATEST CLASS
                        if (classList.isNotEmpty()) {

                            val latestClass =
                                classList[0]

                            sessionManager
                                .saveLatestClassId(
                                    latestClass.class_id
                                )

                            sessionManager
                                .saveLatestClassName(
                                    latestClass.class_name
                                )
                        }

                        // SET ADAPTER
                        val adapter = ClassAdapter(

                            this@AdminDashboardActivity,

                            classList
                        )

                        recyclerClasses.adapter =
                            adapter
                    }

                    // FAILED
                    else {

                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "Failed to load classes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // API ERROR
                override fun onFailure(
                    call: Call<ClassesResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(
                        this@AdminDashboardActivity,
                        t.message,
                        Toast.LENGTH_LONG
                    ).show()

                    t.printStackTrace()
                }
            })
    }

    // AUTO REFRESH
    override fun onResume() {
        super.onResume()

        loadClasses()
    }
}