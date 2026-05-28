package com.example.attendancepro.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceResponse
import com.example.attendancepro.utils.SessionManager
import com.google.android.gms.location.*

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.io.File
import java.io.FileOutputStream

class AttendanceActivity : AppCompatActivity() {

    // =========================
    // ✅ UI
    // =========================
    private lateinit var captureBtn: Button

    private lateinit var statusText: TextView

    private lateinit var progressBar: ProgressBar

    // =========================
    // ✅ LOCATION
    // =========================
    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    // =========================
    // ✅ SESSION
    // =========================
    private lateinit var sessionManager:
            SessionManager

    // =========================
    // ✅ LOCATION VALUES
    // =========================
    private var currentLat = 0.0

    private var currentLng = 0.0

    // =========================
    // 📸 IMAGE URI
    // =========================
    private var imageUri: Uri? = null

    // =========================
    // 📸 CAMERA RESULT
    // =========================
    private val cameraLauncher =

        registerForActivityResult(

            ActivityResultContracts.TakePicture()

        ) { success ->

            if (success) {

                imageUri?.let {

                    uploadAttendance(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_attendance
        )

        // =========================
        // ✅ VIEW BINDING
        // =========================
        captureBtn =
            findViewById(R.id.captureBtn)

        statusText =
            findViewById(R.id.statusText)

        progressBar =
            findViewById(R.id.progressBar)

        // =========================
        // ✅ SESSION
        // =========================
        sessionManager = SessionManager(this)

        // =========================
        // ✅ LOCATION CLIENT
        // =========================
        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(
                    this
                )

        // =========================
        // ✅ CAPTURE CLICK
        // =========================
        captureBtn.setOnClickListener {

            checkPermissionsAndCapture()
        }

        // =========================
        // ✅ FETCH STATUS
        // =========================
        fetchAttendanceStatus()
    }

    // =========================
    // 📍 FETCH LOCATION
    // =========================
    private fun getLocation(
        callback: () -> Unit
    ) {

        if (

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission
                    .ACCESS_FINE_LOCATION

            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Toast.makeText(

                this,

                "Location permission denied",

                Toast.LENGTH_SHORT

            ).show()

            return
        }

        fusedLocationClient.lastLocation

            .addOnSuccessListener { location: Location? ->

                if (location != null) {

                    currentLat = location.latitude

                    currentLng = location.longitude

                    callback()

                } else {

                    Toast.makeText(

                        this,

                        "Unable to fetch location",

                        Toast.LENGTH_SHORT

                    ).show()
                }
            }
    }

    // =========================
    // 📸 CHECK PERMISSIONS
    // =========================
    private fun checkPermissionsAndCapture() {

        if (

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission.CAMERA

            ) != PackageManager.PERMISSION_GRANTED ||

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission
                    .ACCESS_FINE_LOCATION

            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(

                arrayOf(

                    Manifest.permission.CAMERA,

                    Manifest.permission
                        .ACCESS_FINE_LOCATION
                ),

                101
            )

            return
        }

        getLocation {

            openCamera()
        }
    }

    // =========================
    // 📸 OPEN CAMERA
    // =========================
    private fun openCamera() {

        val file = File(

            cacheDir,

            "attendance.jpg"
        )

        imageUri = androidx.core.content
            .FileProvider.getUriForFile(

                this,

                "${packageName}.provider",

                file
            )

        // ✅ SAFE NULL CHECK
        imageUri?.let {

            cameraLauncher.launch(it)
        }
    }

    // =========================
    // 📤 UPLOAD ATTENDANCE
    // =========================
    private fun uploadAttendance(
        uri: Uri
    ) {

        setLoading(true)

        try {

            val inputStream =
                contentResolver
                    .openInputStream(uri)

            val file = File(

                cacheDir,

                "upload.jpg"
            )

            val outputStream =
                FileOutputStream(file)

            inputStream?.copyTo(
                outputStream
            )

            outputStream.close()

            inputStream?.close()

            // =========================
            // ✅ IMAGE PART
            // =========================
            val requestFile = file
                .asRequestBody(

                    "image/*"
                        .toMediaTypeOrNull()
                )

            val imagePart =
                MultipartBody.Part
                    .createFormData(

                        "file",

                        file.name,

                        requestFile
                    )

            // =========================
            // ✅ LAT LNG
            // =========================
            val latBody = currentLat
                .toString()
                .toRequestBody(

                    "text/plain"
                        .toMediaTypeOrNull()
                )

            val lngBody = currentLng
                .toString()
                .toRequestBody(

                    "text/plain"
                        .toMediaTypeOrNull()
                )

            // =========================
            // ✅ API CALL
            // =========================
            RetrofitClient.instance

                .markAttendance(

                    imagePart,

                    latBody,

                    lngBody
                )

                .enqueue(object :
                    Callback<AttendanceResponse> {

                    override fun onResponse(
                        call: Call<AttendanceResponse>,
                        response: Response<AttendanceResponse>
                    ) {

                        setLoading(false)

                        Log.d(
                            "RAW_RESPONSE",
                            response.toString()
                        )

                        if (!response.isSuccessful) {

                            statusText.text =
                                "Server Error ❌"

                            Toast.makeText(

                                this@AttendanceActivity,

                                "HTTP ${response.code()}",

                                Toast.LENGTH_LONG

                            ).show()

                            return
                        }

                        val res = response.body()

                        // ✅ DEBUG
                        Log.d(
                            "ATTENDANCE_RESPONSE",
                            res.toString()
                        )

                        // =========================
                        // ✅ SUCCESS
                        // =========================
                        if (

                            res != null &&

                            res.status != null &&

                            res.status.equals(
                                "Present",
                                ignoreCase = true
                            )
                        ) {

                            statusText.text =
                                "Attendance Marked ✅"

                            captureBtn.text =
                                "Already Marked"

                            captureBtn.isEnabled =
                                false

                            Toast.makeText(

                                this@AttendanceActivity,

                                res.message
                                    ?: "Attendance Marked Successfully",

                                Toast.LENGTH_LONG

                            ).show()

                            AlertDialog.Builder(
                                this@AttendanceActivity
                            )

                                .setTitle(
                                    "Success"
                                )

                                .setMessage(
                                    "You are marked present ✅"
                                )

                                .setCancelable(false)

                                .setPositiveButton(
                                    "Go to Dashboard"
                                ) { _, _ ->

                                    val intent = Intent(

                                        this@AttendanceActivity,

                                        DashboardActivity::class.java
                                    )

                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP

                                    startActivity(
                                        intent
                                    )

                                    finish()
                                }

                                .show()

                        } else {

                            // =========================
                            // ❌ ERROR
                            // =========================
                            val errorMsg =

                                res?.error
                                    ?: res?.message
                                    ?: "Unknown error"

                            statusText.text =
                                errorMsg

                            Toast.makeText(

                                this@AttendanceActivity,

                                errorMsg,

                                Toast.LENGTH_LONG

                            ).show()

                            Log.e(
                                "ATTENDANCE_ERROR",
                                errorMsg
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<AttendanceResponse>,
                        t: Throwable
                    ) {

                        setLoading(false)

                        statusText.text =
                            "Network Error ❌"

                        Toast.makeText(

                            this@AttendanceActivity,

                            t.message,

                            Toast.LENGTH_LONG

                        ).show()

                        Log.e(
                            "NETWORK_ERROR",
                            t.message.toString()
                        )
                    }
                })

        } catch (e: Exception) {

            setLoading(false)

            Toast.makeText(

                this,

                e.message,

                Toast.LENGTH_LONG

            ).show()
        }
    }

    // =========================
    // 📊 FETCH STATUS
    // =========================
    private fun fetchAttendanceStatus() {

        RetrofitClient.instance

            .getStatus()

            .enqueue(object :
                Callback<AttendanceResponse> {

                override fun onResponse(
                    call: Call<AttendanceResponse>,
                    response: Response<AttendanceResponse>
                ) {

                    if (

                        response.isSuccessful &&

                        response.body() != null
                    ) {

                        val data =
                            response.body()!!

                        if (

                            data.status.equals(
                                "Present",
                                ignoreCase = true
                            )
                        ) {

                            statusText.text =
                                "Attendance Already Marked ✅"

                            captureBtn.text =
                                "Already Marked"

                            captureBtn.isEnabled =
                                false
                        }
                    }
                }

                override fun onFailure(
                    call: Call<AttendanceResponse>,
                    t: Throwable
                ) {

                }
            })
    }

    // =========================
    // ⏳ LOADING
    // =========================
    private fun setLoading(
        loading: Boolean
    ) {

        progressBar.visibility =

            if (loading)
                View.VISIBLE

            else
                View.GONE

        captureBtn.isEnabled =
            !loading
    }
}