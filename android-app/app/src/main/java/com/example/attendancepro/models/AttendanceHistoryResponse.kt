package com.example.attendancepro.models

data class AttendanceHistoryResponse(
    val history: List<HistoryItem>
)

data class HistoryItem(
    val date: String,
    val status: String,
    val time: String? = null
)