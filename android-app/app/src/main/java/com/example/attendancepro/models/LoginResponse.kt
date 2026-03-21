package com.example.attendancepro.models

data class LoginResponse(
    val token: String?,
    val error: String?,
    val name: String?
)