package com.example.attendancepro.models

data class AttendanceReportResponse(

    val success: Boolean,

    val class_name: String,

    val department: String? = null,

    val section: String? = null,

    val academic_session: String? = null,

    val course_code: String? = null,

    val present_students: Int,

    val absent_students: Int,

    val na_students: Int,

    val total_monthly_classes: Int,

    val report_date: String? = null,

    val available_dates: List<String>? = null,

    val students: List<AttendanceReportItem>
)

data class AttendanceReportItem(

    val name: String,
    val roll: String,
    val attendance_status: String,
    val time: String
)