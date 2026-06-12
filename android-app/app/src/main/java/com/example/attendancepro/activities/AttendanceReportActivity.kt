package com.example.attendancepro.activities

import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendancepro.R
import com.example.attendancepro.adapters.AttendanceReportAdapter
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceReportItem
import com.example.attendancepro.models.AttendanceReportResponse
import com.google.android.material.datepicker.MaterialDatePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AttendanceReportActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tvClassName: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvSection: TextView
    private lateinit var recyclerReport: RecyclerView
    private lateinit var btnReportMenu: ImageView

    private lateinit var tvSelectedDate: TextView
    private lateinit var btnDatePrev: ImageView
    private lateinit var btnDateNext: ImageView

    // Drawer views
    private lateinit var etSearchName: EditText
    private lateinit var etSearchRoll: EditText
    private lateinit var etRollFrom: EditText
    private lateinit var etRollTo: EditText
    private lateinit var btnApplyFilter: TextView
    private lateinit var btnClearFilters: TextView
    private lateinit var btnDownload: LinearLayout
    private lateinit var btnDateRange: LinearLayout
    private lateinit var btnClearAllFilters: LinearLayout

    private var classId = ""
    private var allStudents: List<AttendanceReportItem> = emptyList()
    private var totalClasses: Int = 0

    // Date navigation
    private var availableDates: List<String> = emptyList()
    private var currentDateIndex: Int = 0
    private var currentDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_attendance_report)

        drawerLayout = findViewById(R.id.drawerLayout)
        tvClassName = findViewById(R.id.tvClassName)
        tvDepartment = findViewById(R.id.tvDepartment)
        tvSection = findViewById(R.id.tvSection)
        recyclerReport = findViewById(R.id.recyclerReport)
        btnReportMenu = findViewById(R.id.btnReportMenu)

        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        btnDatePrev = findViewById(R.id.btnDatePrev)
        btnDateNext = findViewById(R.id.btnDateNext)

        etSearchName = findViewById(R.id.etSearchName)
        etSearchRoll = findViewById(R.id.etSearchRoll)
        etRollFrom = findViewById(R.id.etRollFrom)
        etRollTo = findViewById(R.id.etRollTo)
        btnApplyFilter = findViewById(R.id.btnApplyFilter)
        btnClearFilters = findViewById(R.id.btnClearFilters)
        btnDownload = findViewById(R.id.btnDownload)
        btnDateRange = findViewById(R.id.btnDateRange)
        btnClearAllFilters = findViewById(R.id.btnClearAllFilters)

        recyclerReport.layoutManager = LinearLayoutManager(this)

        classId = intent.getStringExtra("CLASS_ID") ?: ""

        btnReportMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Date navigation arrows
        btnDatePrev.setOnClickListener {
            if (availableDates.isNotEmpty() && currentDateIndex < availableDates.size - 1) {
                currentDateIndex++
                currentDate = availableDates[currentDateIndex]
                loadReport(currentDate)
            }
        }

        btnDateNext.setOnClickListener {
            if (availableDates.isNotEmpty() && currentDateIndex > 0) {
                currentDateIndex--
                currentDate = availableDates[currentDateIndex]
                loadReport(currentDate)
            }
        }

        // Tap on the date text to open date picker
        tvSelectedDate.setOnClickListener {
            showSingleDatePicker()
        }

        setupDrawerClicks()
        setupFilterValidation()

        // Load report for today (default)
        loadReport(null)
    }

    private fun loadReport(date: String?) {
        RetrofitClient.instance.getClassAttendanceReport(classId, date)
            .enqueue(object : Callback<AttendanceReportResponse> {
                override fun onResponse(
                    call: Call<AttendanceReportResponse>,
                    response: Response<AttendanceReportResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!

                        // === Build header: Department (Session) | Section | ClassName (Code) ===
                        val dept = data.department?.takeIf { it.isNotBlank() } ?: "Attendance"
                        val session = data.academic_session?.takeIf { it.isNotBlank() }
                        val deptLine = if (session != null) "$dept  ($session)" else dept
                        tvDepartment.text = deptLine

                        val sectionText = data.section?.takeIf { it.isNotBlank() } ?: "—"
                        tvSection.text = "Section $sectionText"

                        val code = data.course_code?.takeIf { it.isNotBlank() }
                        tvClassName.text = if (code != null) "${data.class_name}  ($code)" else data.class_name

                        allStudents = data.students
                        totalClasses = data.total_monthly_classes

                        // Update available dates
                        data.available_dates?.let {
                            availableDates = it
                        }

                        // Update current date display
                        val reportDate = data.report_date ?: ""
                        currentDate = reportDate
                        currentDateIndex = availableDates.indexOf(reportDate).coerceAtLeast(0)

                        // Format the date display
                        val displayDate = formatDateDisplay(reportDate)
                        tvSelectedDate.text = displayDate

                        // Update arrow visibility
                        btnDatePrev.alpha = if (currentDateIndex < availableDates.size - 1) 1.0f else 0.3f
                        btnDateNext.alpha = if (currentDateIndex > 0) 1.0f else 0.3f

                        updateAdapter(allStudents)
                    } else {
                        Toast.makeText(this@AttendanceReportActivity, "Failed To Load Report", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AttendanceReportResponse>, t: Throwable) {
                    Toast.makeText(this@AttendanceReportActivity, t.message, Toast.LENGTH_LONG).show()
                }
            })
    }

    // ============================================================
    // FILTER LOGIC
    // ============================================================

    /**
     * Extracts numeric portion from a roll string (e.g. "CSE3135" → 3135, "3135" → 3135).
     * Returns null if no number found.
     */
    private fun extractRollNumber(roll: String): Int? {
        return Regex("\\d+").find(roll)?.value?.toIntOrNull()
    }

    private fun applyFilters() {
        val nameQuery = etSearchName.text.toString().trim()
        val rollQuery = etSearchRoll.text.toString().trim()
        val rollFromStr = etRollFrom.text.toString().trim()
        val rollToStr = etRollTo.text.toString().trim()

        val nameTerms = if (nameQuery.isNotEmpty())
            nameQuery.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        else emptyList()

        val rollTerms = if (rollQuery.isNotEmpty())
            rollQuery.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        else emptyList()

        val rollFrom = rollFromStr.toIntOrNull()
        val rollTo = rollToStr.toIntOrNull()
        val hasRange = rollFrom != null && rollTo != null

        // If all fields empty → show all
        if (nameTerms.isEmpty() && rollTerms.isEmpty() && !hasRange) {
            updateAdapter(allStudents)
            drawerLayout.closeDrawer(GravityCompat.END)
            Toast.makeText(this, "No filter applied — showing all", Toast.LENGTH_SHORT).show()
            return
        }

        val filtered = allStudents.filter { student ->
            val rollNum = extractRollNumber(student.roll)

            // Name match: student name contains ANY of the terms
            val nameMatch = nameTerms.isEmpty() || nameTerms.any { term ->
                student.name.lowercase().contains(term)
            }

            // Roll exact/partial match
            val rollMatch = rollTerms.isEmpty() || rollTerms.any { term ->
                student.roll.lowercase().contains(term)
            }

            // Roll range match (numeric portion)
            val rangeMatch = !hasRange || (rollNum != null && rollNum in rollFrom!!..rollTo!!)

            // Combine: name filter OR roll filter OR range filter
            // If multiple are filled, ANY match includes the student
            val anyNameFilter = nameTerms.isNotEmpty()
            val anyRollFilter = rollTerms.isNotEmpty()
            val anyRangeFilter = hasRange

            when {
                anyNameFilter && anyRollFilter && anyRangeFilter ->
                    nameMatch || rollMatch || rangeMatch
                anyNameFilter && anyRollFilter ->
                    nameMatch || rollMatch
                anyNameFilter && anyRangeFilter ->
                    nameMatch || rangeMatch
                anyRollFilter && anyRangeFilter ->
                    rollMatch || rangeMatch
                anyNameFilter -> nameMatch
                anyRollFilter -> rollMatch
                anyRangeFilter -> rangeMatch
                else -> true
            }
        }

        updateAdapter(filtered)
        drawerLayout.closeDrawer(GravityCompat.END)

        val msg = if (filtered.isEmpty()) "No students match your filter"
        else "${filtered.size} student(s) found"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun clearFilters() {
        etSearchName.setText("")
        etSearchRoll.setText("")
        etRollFrom.setText("")
        etRollTo.setText("")
        updateAdapter(allStudents)
        drawerLayout.closeDrawer(GravityCompat.END)
        Toast.makeText(this, "Filters cleared", Toast.LENGTH_SHORT).show()
    }

    // ============================================================

    private fun formatDateDisplay(dateStr: String): String {
        return try {
            if (dateStr.contains(" ")) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy (EEE) hh:mm a", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                if (date != null) outputFormat.format(date) else dateStr
            } else {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy (EEE)", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                if (date != null) outputFormat.format(date) else dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun showSingleDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Report Date")
            .setTheme(R.style.AcrylicDatePickerTheme)
            .build()

        datePicker.addOnPositiveButtonClickListener { millis ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val selectedDate = sdf.format(Date(millis))
            loadReport(selectedDate)
        }

        datePicker.show(supportFragmentManager, "SINGLE_DATE_PICKER")
    }

    private fun updateAdapter(list: List<AttendanceReportItem>) {
        recyclerReport.adapter = AttendanceReportAdapter(this, list)
    }

    private fun setupFilterValidation() {
        btnApplyFilter.isEnabled = false // Initially disabled
        
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateFilterInputs()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        
        etSearchName.addTextChangedListener(watcher)
        etSearchRoll.addTextChangedListener(watcher)
        etRollFrom.addTextChangedListener(watcher)
        etRollTo.addTextChangedListener(watcher)
    }

    private fun validateFilterInputs() {
        val hasInput = etSearchName.text.toString().trim().isNotEmpty() ||
                       etSearchRoll.text.toString().trim().isNotEmpty() ||
                       etRollFrom.text.toString().trim().isNotEmpty() ||
                       etRollTo.text.toString().trim().isNotEmpty()
        
        btnApplyFilter.isEnabled = hasInput
    }

    private fun setupDrawerClicks() {
        btnApplyFilter.setOnClickListener { applyFilters() }

        btnClearFilters.setOnClickListener { clearFilters() }
        btnClearAllFilters.setOnClickListener { clearFilters() }

        btnDownload.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            downloadExcelReport()
        }

        btnDateRange.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            downloadExcelReport()
        }
    }

    private fun downloadExcelReport() {
        val token = com.example.attendancepro.utils.SessionManager(this).getToken()
        val url = "${RetrofitClient.BASE_URL}api/attendance/export_excel/$classId?token=$token"

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Attendance_Report.xlsx")
            .setDescription("Downloading Excel report")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Attendance_Report_${System.currentTimeMillis()}.xlsx")

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(this, "Downloading Report...", Toast.LENGTH_SHORT).show()
    }
}