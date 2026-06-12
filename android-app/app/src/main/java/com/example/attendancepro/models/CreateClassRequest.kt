package com.example.attendancepro.models

data class CreateClassRequest(
    val course_name: String,
    val course_code: String,
    val semester: String,
    val section: String,
    val year: String,
    val academic_session: String,
    val department: String,
    val admin_id: String
)