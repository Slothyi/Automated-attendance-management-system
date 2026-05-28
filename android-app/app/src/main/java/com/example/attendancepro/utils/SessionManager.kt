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

            .putString(
                "token",
                token
            )

            .apply()
    }

    fun getToken(): String? {

        return prefs.getString(
            "token",
            null
        )
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