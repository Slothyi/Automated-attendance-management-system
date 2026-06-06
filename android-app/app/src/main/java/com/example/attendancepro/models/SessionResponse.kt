package com.example.attendancepro.models

data class SessionResponse(

    val success: Boolean,

    val message: String? = null,

    val session_code: String? = null,

    val session_uuid: String? = null,

    val bluetooth_name: String? = null,

    val classroom_beacon: String? = null,

    val otp_code: String? = null
)