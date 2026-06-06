package com.example.attendancepro.activities

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendancepro.R
import com.example.attendancepro.adapters.StudentClassesAdapter
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.StudentClassesResponse
import com.google.android.material.appbar.MaterialToolbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StudentClassesActivity : AppCompatActivity() {

    private lateinit var rvClasses: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: StudentClassesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_student_classes)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        rvClasses = findViewById(R.id.rvClasses)
        
        // Fix for recycler view overlap with system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(rvClasses) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom + (20 * resources.displayMetrics.density).toInt())
            insets
        }
        progressBar = findViewById(R.id.progressBar)

        rvClasses.layoutManager = LinearLayoutManager(this)
        adapter = StudentClassesAdapter(emptyList())
        rvClasses.adapter = adapter

        fetchClasses()
    }

    private fun fetchClasses() {
        progressBar.visibility = View.VISIBLE
        RetrofitClient.instance.getStudentClasses().enqueue(object : Callback<StudentClassesResponse> {
            override fun onResponse(
                call: Call<StudentClassesResponse>,
                response: Response<StudentClassesResponse>
            ) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val classes = response.body()!!.classes
                    if (classes.isEmpty()) {
                        Toast.makeText(this@StudentClassesActivity, "No classes attended yet", Toast.LENGTH_SHORT).show()
                    } else {
                        adapter.updateData(classes)
                    }
                } else {
                    Toast.makeText(this@StudentClassesActivity, "Failed to load classes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<StudentClassesResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@StudentClassesActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
