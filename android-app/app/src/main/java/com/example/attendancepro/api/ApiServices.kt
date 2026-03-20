package com.example.attendancepro.api

import com.example.attendancepro.models.AttendanceResponse
import com.example.attendancepro.models.LoginRequest
import com.example.attendancepro.models.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/attendance/status")
    fun getStatus(): Call<AttendanceResponse>

    @Multipart
    @POST("api/attendance/mark")
    fun markAttendance(
        @Part file: MultipartBody.Part,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody
    ): Call<AttendanceResponse>

    @POST("api/auth/register")
    fun register(
        @Part("name") name: RequestBody,
        @Part("roll") roll: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part file: MultipartBody.Part
    ): Call<Map<String, String>>
    @POST("api/attendance/unmark")
    fun unmarkAttendance(): Call<AttendanceResponse>
}