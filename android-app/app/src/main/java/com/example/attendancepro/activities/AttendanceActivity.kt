package com.example.attendancepro.activities

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri

import android.os.Bundle
import android.os.Handler
import android.os.Looper

import android.view.View

import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.airbnb.lottie.LottieAnimationView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.attendancepro.models.SessionResponse

import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

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

class AttendanceActivity : AppCompatActivity() {

    // =========================
    // UI
    // =========================
    private lateinit var captureBtn: View

    private lateinit var statusText: TextView

    private lateinit var progressBar: ProgressBar

    private lateinit var sessionInfoText: TextView

    private lateinit var successCard: View
    private lateinit var lottieSuccess: LottieAnimationView

    // =========================
    // SESSION
    // =========================
    private var detectedSessionCode = ""

    private var expectedSessionUuid = ""

    private var activeSessionLoaded = false

    // =========================
    // IMAGE
    // =========================
    private var imageUri: Uri? = null

    // =========================
    // LOCATION
    // =========================
    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    // =========================
    // SESSION MANAGER
    // =========================
    private lateinit var sessionManager:
            SessionManager

    // =========================
    // LOCATION DATA
    // =========================
    private var currentLat = 0.0

    private var currentLng = 0.0

    // =========================
    // CAMERA
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        // Transparent status bar — blends with dark bg
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowInsetsControllerCompat(
            window, window.decorView
        ).isAppearanceLightStatusBars = false

        setContentView(
            R.layout.activity_attendance
        )

        // =========================
        // VIEWS
        // =========================
        captureBtn =
            findViewById(R.id.captureBtn)

        statusText =
            findViewById(R.id.statusText)

        progressBar =
            findViewById(R.id.progressBar)

        sessionInfoText =
            findViewById(R.id.sessionInfoText)

        successCard =
            findViewById(R.id.successCard)

        lottieSuccess =
            findViewById(R.id.lottieSuccess)

        successCard.visibility = View.GONE

        // =========================
        // SESSION
        // =========================
        sessionManager =
            SessionManager(this)

        // =========================
        // LOCATION
        // =========================
        fusedLocationClient =

            LocationServices
                .getFusedLocationProviderClient(
                    this
                )

        // =========================
        // BUTTON
        // =========================
        captureBtn.setOnClickListener {

            loadActiveSession { success ->

                if (success) {

                    checkPermissionsAndCapture()
                }
            }
        }

        fetchAttendanceStatus()
    }


    private fun loadActiveSession(
        onLoaded: (Boolean) -> Unit
    ) {

        val classId =
            sessionManager.getLatestClassId()

        if (classId.isNullOrEmpty()) {

            Toast.makeText(
                this,
                "Class not found",
                Toast.LENGTH_LONG
            ).show()

            onLoaded(false)
            return
        }

        RetrofitClient.instance
            .getActiveSession(classId)
            .enqueue(object :
                Callback<SessionResponse> {

                override fun onResponse(
                    call: Call<SessionResponse>,
                    response: Response<SessionResponse>
                ) {

                    val body = response.body()

                    if (
                        response.isSuccessful &&
                        body != null &&
                        body.success
                    ) {

                        detectedSessionCode =
                            body.session_code ?: ""

                        expectedSessionUuid =
                            body.session_uuid ?: ""

                        sessionInfoText.text =

                            "Session Code: $detectedSessionCode\n\n" +

                                    "BLE UUID Ready"

                        activeSessionLoaded = true

                        onLoaded(true)

                    } else {

                        Toast.makeText(
                            this@AttendanceActivity,
                            "No active attendance session",
                            Toast.LENGTH_LONG
                        ).show()

                        onLoaded(false)
                    }
                }

                override fun onFailure(
                    call: Call<SessionResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(
                        this@AttendanceActivity,
                        t.message,
                        Toast.LENGTH_LONG
                    ).show()

                    onLoaded(false)
                }
            })
    }

    // =========================
    // CHECK PERMISSIONS
    // =========================
    private fun checkPermissionsAndCapture() {

        val permissions = arrayOf(

            Manifest.permission
                .ACCESS_FINE_LOCATION,

            Manifest.permission
                .BLUETOOTH_SCAN,

            Manifest.permission
                .BLUETOOTH_CONNECT
        )

        val denied = permissions.any {

            ContextCompat.checkSelfPermission(

                this,

                it

            ) != PackageManager.PERMISSION_GRANTED
        }

        if (denied) {

            requestPermissions(

                permissions,

                100
            )

            return
        }

        getLocationAndVerify()
    }

    // =========================
    // PERMISSION RESULT
    // =========================
    override fun onRequestPermissionsResult(

        requestCode: Int,

        permissions: Array<out String>,

        grantResults: IntArray

    ) {

        super.onRequestPermissionsResult(

            requestCode,

            permissions,

            grantResults
        )

        if (

            requestCode == 100 &&

            grantResults.isNotEmpty() &&

            grantResults.all {

                it == PackageManager.PERMISSION_GRANTED
            }
        ) {

            getLocationAndVerify()
        }
    }

    // =========================
    // GET LOCATION
    // =========================
    private fun getLocationAndVerify() {

        if (

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission
                    .ACCESS_FINE_LOCATION

            ) != PackageManager.PERMISSION_GRANTED

        ) return

        fusedLocationClient.lastLocation

            .addOnSuccessListener {

                    location ->

                if (location == null) {

                    Toast.makeText(

                        this,

                        "Location unavailable",

                        Toast.LENGTH_LONG

                    ).show()

                    return@addOnSuccessListener
                }

                // =========================
                // MOCK LOCATION
                // =========================
                if (location.isFromMockProvider) {

                    Toast.makeText(

                        this,

                        "Mock location detected",

                        Toast.LENGTH_LONG

                    ).show()

                    return@addOnSuccessListener
                }

                currentLat =
                    location.latitude

                currentLng =
                    location.longitude

                statusText.text =
                    "📍 GPS Verified"

                // =========================
                // BLE VERIFY
                // =========================
                verifyTeacherBluetooth {

                        detected ->

                    if (detected) {

                        statusText.text =
                            "📷 Opening Camera..."

                        openCamera()

                    } else {

                        Toast.makeText(

                            this,

                            "Teacher device not nearby",

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }
            }
    }

// =========================
// REAL BLE SCAN
// =========================
    private fun verifyTeacherBluetooth(
        callback: (Boolean) -> Unit
    ) {

        // =========================
        // PERMISSION CHECK
        // =========================
        if (

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission
                    .BLUETOOTH_SCAN

            ) != PackageManager.PERMISSION_GRANTED

        ) {

            callback(false)
            return
        }

        try {

            val bluetoothManager =

                getSystemService(
                    BLUETOOTH_SERVICE
                ) as BluetoothManager

            val bluetoothAdapter =
                bluetoothManager.adapter

            // =========================
            // BLUETOOTH ENABLED?
            // =========================
            if (!bluetoothAdapter.isEnabled) {

                Toast.makeText(

                    this,

                    "Enable Bluetooth",

                    Toast.LENGTH_LONG

                ).show()

                callback(false)

                return
            }

            // =========================
            // BLE SCANNER
            // =========================
            val scanner =
                bluetoothAdapter.bluetoothLeScanner

            if (scanner == null) {

                callback(false)
                return
            }

            val handler =
                Handler(Looper.getMainLooper())

            var found = false

            val scanCallback =

                object : ScanCallback() {

                    override fun onScanResult(

                        callbackType: Int,

                        result: ScanResult
                    ) {

                        val uuids =

                            result.scanRecord
                                ?.serviceUuids

                        uuids?.forEach {

                            val detectedUuid =
                                it.uuid.toString()

                            println(
                                "BLE UUID: $detectedUuid"
                            )

                            if (
                                detectedUuid.equals(
                                    expectedSessionUuid,
                                    ignoreCase = true
                                )
                            ) {

                                statusText.text =
                                    "📡 Teacher BLE Detected"

                                found = true

                                try {

                                    scanner.stopScan(this)

                                } catch (_: Exception) {
                                }

                                callback(true)

                                return
                            }
                        }
                    }

                    override fun onScanFailed(
                        errorCode: Int
                    ) {

                        Toast.makeText(

                            this@AttendanceActivity,

                            "BLE Failed: $errorCode",

                            Toast.LENGTH_LONG

                        ).show()

                        callback(false)
                    }
                }

            // =========================
            // START SCAN
            // =========================
            try {

                scanner.startScan(
                    scanCallback
                )

            } catch (e: SecurityException) {

                e.printStackTrace()

                callback(false)

                return
            }

            // =========================
            // AUTO STOP
            // =========================
            handler.postDelayed({

                if (!found) {

                    try {

                        scanner.stopScan(
                            scanCallback
                        )

                    } catch (e: Exception) {

                        e.printStackTrace()
                    }

                    callback(false)
                }

            }, 5000)

        } catch (e: SecurityException) {

            e.printStackTrace()

            callback(false)
        }
    }

    // =========================
    // OPEN CAMERA
    // =========================
    private fun openCamera() {

        val file = File(

            cacheDir,

            "attendance.jpg"
        )

        imageUri = FileProvider.getUriForFile(

            this,

            "$packageName.provider",

            file
        )

        imageUri?.let {

            cameraLauncher.launch(it)
        }
    }

    // =========================
    // UPLOAD ATTENDANCE
    // =========================
    private fun uploadAttendance(
        uri: Uri
    ) {

        progressBar.visibility =
            View.VISIBLE

        val file = File(

            cacheDir,

            "upload.jpg"
        )

        contentResolver.openInputStream(uri)
            ?.use { input ->

                file.outputStream()
                    .use { output ->

                        input.copyTo(output)
                    }
            }

        val requestFile =

            file.asRequestBody(
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

        val latBody =

            currentLat.toString()

                .toRequestBody(
                    "text/plain"
                        .toMediaTypeOrNull()
                )

        val lngBody =

            currentLng.toString()

                .toRequestBody(
                    "text/plain"
                        .toMediaTypeOrNull()
                )

        val sessionBody =

            detectedSessionCode

                .toRequestBody(
                    "text/plain"
                        .toMediaTypeOrNull()
                )

        val uuidBody =

            expectedSessionUuid

                .toRequestBody(
                    "text/plain"
                        .toMediaTypeOrNull()
                )

        RetrofitClient.instance

            .markAttendance(

                imagePart,

                latBody,

                lngBody,

                sessionBody,

                uuidBody
            )

            .enqueue(object :
                Callback<AttendanceResponse> {

                override fun onResponse(
                    call: Call<AttendanceResponse>,
                    response: Response<AttendanceResponse>
                ) {
                    progressBar.visibility = View.GONE
                    
                    try {
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.status == "Success") {
                                successCard.visibility = View.VISIBLE
                                lottieSuccess.playAnimation()
                                statusText.text = "✅ Attendance Marked"
                                Toast.makeText(
                                    this@AttendanceActivity,
                                    body.message ?: "Attendance Marked",
                                    Toast.LENGTH_LONG
                                ).show()
                                
                                // Gracefully close AttendanceActivity after 2.5 seconds
                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (!isFinishing) {
                                        finish()
                                    }
                                }, 2500)
                                
                            } else {
                                Toast.makeText(
                                    this@AttendanceActivity,
                                    body.error ?: "Attendance Failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(
                                this@AttendanceActivity,
                                "Attendance Failed",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@AttendanceActivity, "Error parsing response", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(

                    call: Call<AttendanceResponse>,

                    t: Throwable

                ) {

                    progressBar.visibility =
                        View.GONE

                    Toast.makeText(

                        this@AttendanceActivity,

                        t.message,

                        Toast.LENGTH_LONG

                    ).show()
                }
            })
    }

    // =========================
    // STATUS
    // =========================
    private fun fetchAttendanceStatus() {

        RetrofitClient.instance

            .getStatus()

            .enqueue(object :
                Callback<AttendanceResponse> {

                override fun onResponse(

                    call: Call<AttendanceResponse>,

                    response:
                    Response<AttendanceResponse>

                ) {

                    if (

                        response.isSuccessful &&

                        response.body() != null
                    ) {

                        statusText.text =

                            response.body()!!
                                .status
                    }
                }

                override fun onFailure(

                    call: Call<AttendanceResponse>,

                    t: Throwable

                ) {

                }
            })
    }
}
