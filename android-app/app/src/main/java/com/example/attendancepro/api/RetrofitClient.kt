package com.example.attendancepro.api

import com.example.attendancepro.App
import com.example.attendancepro.utils.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "http://10.143.13.169:8000/"  // ✅ your laptop IP

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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

            chain.proceed(requestBuilder.build())
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