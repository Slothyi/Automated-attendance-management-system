package com.example.attendancepro.models

data class AttendanceResponse(
    val message: String? = null,

    val status: String? = null,

    val error: String? = null,

    val remaining_minutes: Int? = null
)