package com.example.attendancepro.models

data class ClassItem(

    val class_id: String,

    val class_name: String,

    val section: String,

    val department: String,

    val year: String,

    val semester: String,

    val student_count: Int,

    val present_count: Int
)