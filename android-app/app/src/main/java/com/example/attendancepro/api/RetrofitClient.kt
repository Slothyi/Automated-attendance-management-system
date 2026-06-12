package com.example.attendancepro.api

import com.example.attendancepro.App
import com.example.attendancepro.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.content.Intent
import com.example.attendancepro.activities.StudentLoginActivity

object RetrofitClient {

    const val BASE_URL = "http://192.168.0.166:8000/"  // ✅ your laptop IP

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .addInterceptor { chain ->

            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            val url = originalRequest.url.toString()

            val token = SessionManager(App.context).getToken()

            // 🚫 Don't attach token for login/register
            if (!token.isNullOrEmpty() &&
                !url.contains("auth/login") &&
                !url.contains("auth/register")
            ) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val response = chain.proceed(requestBuilder.build())

            // 🔐 SINGLE SESSION EXPIRATION INTERCEPTOR
            if (response.code == 401 && !url.contains("auth/login") && !url.contains("auth/register")) {
                SessionManager(App.context).clearSession()

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        App.context,
                        "Session expired. Another device has logged in.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }

                val intent = Intent(App.context, StudentLoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                App.context.startActivity(intent)
            }

            response
        }
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}