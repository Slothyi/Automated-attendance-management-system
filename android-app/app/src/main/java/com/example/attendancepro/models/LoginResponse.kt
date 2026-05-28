package com.example.attendancepro.models

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val error: String?,
    val name: String?,
    val message: String?
)