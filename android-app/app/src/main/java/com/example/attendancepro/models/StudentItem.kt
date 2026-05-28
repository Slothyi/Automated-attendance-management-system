package com.example.attendancepro.models

data class StudentItem(

    val name: String,

    val roll: String,

    val attendance_status: String? = "Absent"
)
