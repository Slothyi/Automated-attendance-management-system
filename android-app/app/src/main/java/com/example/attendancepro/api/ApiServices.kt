package com.example.attendancepro.api

import com.example.attendancepro.models.*

import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    // =========================
    // 🎓 STUDENT LOGIN
    // =========================
    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>


    // =========================
    // 👨‍💼 ADMIN LOGIN
    // =========================
    @POST("api/admin/login")
    fun adminLogin(
        @Body request: AdminLoginRequest
    ): Call<AdminLoginResponse>


    // =========================
    // 🆕 STUDENT REGISTER
    // =========================
    @Multipart
    @POST("api/auth/register")
    fun register(

        @Part("name")
        name: RequestBody,

        @Part("roll")
        roll: RequestBody,

        @Part("email")
        email: RequestBody,

        @Part("password")
        password: RequestBody,

        @Part
        file: MultipartBody.Part

    ): Call<Map<String, String>>


    // =========================
    // 🏫 CREATE CLASS
    // =========================
    @POST("api/class/create")
    fun createClass(
        @Body request: CreateClassRequest
    ): Call<CreateClassResponse>


    // =========================
    // 👨‍🎓 ADD STUDENTS
    // =========================
    @POST("api/class/add-students")
    fun addStudents(
        @Body request: AddStudentsRequest
    ): Call<MessageResponse>


    // =========================
    // 📚 GET ALL CLASSES
    // =========================
    @GET("api/class/all")
    fun getAllClasses(): Call<ClassesResponse>


    // =========================
    // 👨‍🎓 GET CLASS STUDENTS
    // =========================
    @GET("api/class/students/{class_id}")
    fun getClassStudents(

        @Path("class_id")
        classId: String

    ): Call<ClassStudentsResponse>


    // =========================
    // 📊 CLASS ATTENDANCE REPORT
    // =========================
    @GET("api/attendance/class-report/{class_id}")
    fun getClassAttendanceReport(

        @Path("class_id")
        classId: String

    ): Call<AttendanceReportResponse>


    // =========================
    // 📊 ATTENDANCE STATUS
    // =========================
    @GET("api/attendance/status")
    fun getStatus(): Call<AttendanceResponse>


    // =========================
    // 📜 ATTENDANCE HISTORY
    // =========================
    @GET("api/attendance/history")
    fun getHistory(): Call<AttendanceHistoryResponse>


    // =========================
    // 📸 MARK ATTENDANCE
    // =========================
    @Multipart
    @POST("api/attendance/mark")
    fun markAttendance(

        @Part
        file: MultipartBody.Part,

        @Part("lat")
        lat: RequestBody,

        @Part("lng")
        lng: RequestBody

    ): Call<AttendanceResponse>


    // =========================
    // ❌ UNMARK ATTENDANCE
    // =========================
    @POST("api/attendance/unmark")
    fun unmarkAttendance(): Call<AttendanceResponse>
}