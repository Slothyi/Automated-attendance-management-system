package com.example.attendancepro.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    // ✅ SHARED PREFERENCES
    private val prefs: SharedPreferences =

        context.getSharedPreferences(
            "auth",
            Context.MODE_PRIVATE
        )

    // =========================
    // 🔐 TOKEN
    // =========================
    fun saveToken(token: String) {
        prefs.edit()
            .putString("token", token)
            .putLong("login_timestamp", System.currentTimeMillis())
            .apply()
    }

    fun getToken(): String? {
        return prefs.getString(
            "token",
            null
        )
    }

    fun isSessionValid(): Boolean {
        val token = getToken() ?: return false
        var loginTimestamp = prefs.getLong("login_timestamp", 0L)
        if (loginTimestamp == 0L) {
            // For legacy sessions, save current time as fallback so they stay logged in
            loginTimestamp = System.currentTimeMillis()
            prefs.edit().putLong("login_timestamp", loginTimestamp).apply()
        }
        
        val expirationTime = 5L * 60 * 60 * 1000L // 5 hours session persistence
        val currentTime = System.currentTimeMillis()
        
        return (currentTime - loginTimestamp) < expirationTime
    }

    // =========================
    // 👤 USER NAME
    // =========================
    fun saveName(name: String) {

        prefs.edit()

            .putString(
                "name",
                name
            )

            .apply()
    }

    fun getName(): String? {

        return prefs.getString(
            "name",
            null
        )
    }

    // =========================
    // 📧 USER EMAIL
    // =========================
    fun saveEmail(email: String) {
        prefs.edit()
            .putString("email", email)
            .apply()
    }

    fun getEmail(): String? {
        return prefs.getString("email", null)
    }

    // =========================
    // 🏫 LATEST CLASS ID
    // =========================
    fun saveLatestClassId(id: String) {

        prefs.edit()

            .putString(
                "LATEST_CLASS_ID",
                id
            )

            .apply()
    }

    fun getLatestClassId(): String? {

        return prefs.getString(
            "LATEST_CLASS_ID",
            null
        )
    }

    // =========================
    // 📚 LATEST CLASS NAME
    // =========================
    fun saveLatestClassName(name: String) {

        prefs.edit()

            .putString(
                "LATEST_CLASS_NAME",
                name
            )

            .apply()
    }

    fun getLatestClassName(): String? {

        return prefs.getString(
            "LATEST_CLASS_NAME",
            null
        )
    }

    // =========================
    // 🗄️ ADMIN ID
    // =========================
    fun saveAdminId(id: String) {

        prefs.edit()

            .putString(
                "admin_id",
                id
            )

            .apply()
    }

    fun getAdminId(): String? {

        return prefs.getString(
            "admin_id",
            null
        )
    }

    // =========================
    // 🚪 CLEAR SESSION
    // =========================
    fun clearSession() {

        prefs.edit()

            .clear()

            .apply()
    }

    // =========================
    // ❌ CLEAR TOKEN ONLY
    // =========================
    fun clearToken() {

        prefs.edit()

            .remove("token")

            .apply()
    }
}