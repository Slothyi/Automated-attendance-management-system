package com.example.attendancepro.models

data class ManualAttendanceRequest(
    val class_id: String,
    val students: List<StudentItem>
)
