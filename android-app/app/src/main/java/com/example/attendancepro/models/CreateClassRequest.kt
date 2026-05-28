package com.example.attendancepro.models

data class CreateClassRequest(

    val class_name: String,

    val section: String,

    val department: String,

    val year: String,

    val semester: String,

    val admin_id: String
)