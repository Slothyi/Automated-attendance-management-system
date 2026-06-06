package com.example.attendancepro.models

data class ResetPasswordRequest(
    val email: String,
    val new_password: String
)
