package com.example.attendancepro.models

data class StudentClassesResponse(
    val success: Boolean,
    val classes: List<StudentClassItem>
)

data class StudentClassItem(
    val class_name: String,
    val attended_count: Int
)
