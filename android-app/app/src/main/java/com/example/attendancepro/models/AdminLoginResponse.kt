package com.example.attendancepro.models

data class AdminLoginResponse(

    val token: String? = null,

    val name: String? = null,

    val error: String? = null,

    val email: String? = null,

    val admin_id: String? = null
)