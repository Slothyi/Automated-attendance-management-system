package com.example.attendancepro.models

data class ClassStudentsResponse(

    val class_name: String,

    val class_id: String,

    val present_students: Int,

    val absent_students: Int,

    val na_students: Int,

    val students: List<StudentItem>
)