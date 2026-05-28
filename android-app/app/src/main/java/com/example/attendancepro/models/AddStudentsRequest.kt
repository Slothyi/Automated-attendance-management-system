package com.example.attendancepro.models

data class AddStudentsRequest(

    val class_id: String,

    val students: List<StudentData>
)