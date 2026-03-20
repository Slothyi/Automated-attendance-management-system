package com.example.attendancepro.utils

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }
    fun clearToken() {
        prefs.edit().remove("token").apply()
    }
}