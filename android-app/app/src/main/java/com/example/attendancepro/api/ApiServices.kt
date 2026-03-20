package com.example.attendancepro.api

import com.example.attendancepro.models.LoginRequest
import com.example.attendancepro.models.LoginResponse
import com.example.attendancepro.models.AttendanceResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ✅ LOGIN
    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // ✅ GET STATUS
    @GET("api/attendance/status")
    fun getStatus(): Call<AttendanceResponse>

    // ✅ MARK ATTENDANCE
    @Multipart
    @POST("api/attendance/mark")
    fun markAttendance(
        @Part file: MultipartBody.Part,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody
    ): Call<AttendanceResponse>

    // ✅ UNMARK (FIXED — NO TOKEN FIELD)
    @POST("api/attendance/unmark")
    fun unmarkAttendance(): Call<AttendanceResponse>
}