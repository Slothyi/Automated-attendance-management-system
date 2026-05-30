package com.example.attendancepro.models

data class AttendanceReportResponse(

    val success: Boolean,

    val class_name: String,

    val present_students: Int,

    val absent_students: Int,

    val na_students: Int,

    val students: List<AttendanceReportItem>
)

data class AttendanceReportItem(

    val name: String,

    val roll: String,

    val attendance_status: String,

    val weekly_attendance: Int,

    val monthly_attendance: Int
)