package com.example.attendancepro.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
    fun clearToken() {
        prefs.edit().clear().apply()
    }

    fun saveName(name: String) {
        prefs.edit().putString("name", name).apply()
    }

    fun getName(): String? {
        return prefs.getString("name", null)
    }
}