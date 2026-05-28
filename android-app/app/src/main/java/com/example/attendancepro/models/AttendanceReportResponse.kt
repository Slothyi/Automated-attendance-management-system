package com.example.attendancepro.models

data class AttendanceReportResponse(

    val class_name: String,

    val report: List<AttendanceReportItem>
)

data class AttendanceReportItem(

    val name: String,

    val roll: String,

    val weekly_percentage: Int,

    val monthly_percentage: Int
)