package com.example.attendancepro.activities

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
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
import java.util.UUID

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
    private var expectedClassroomBeacon = ""
    private var expectedOtpCode = ""
    private var activeSessionLoaded = false

    // =========================
    // IMAGE
    // =========================
    private var imageUri: Uri? = null

    // =========================
    // LOCATION
    // =========================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // =========================
    // SESSION MANAGER
    // =========================
    private lateinit var sessionManager: SessionManager

    // =========================
    // LOCATION DATA
    // =========================
    private var currentLat = 0.0
    private var currentLng = 0.0

    // =========================
    // PENDING BLE CALLBACK
    // Survives the "enable Bluetooth" round-trip
    // =========================
    private var pendingBleCallback: ((Boolean) -> Unit)? = null

    // =========================
    // ENABLE BLUETOOTH LAUNCHER (Android 12+)
    // =========================
    private val enableBluetoothLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val callback = pendingBleCallback
            pendingBleCallback = null
            if (result.resultCode == RESULT_OK) {
                // User enabled BT — wait briefly for radio initialization before retrying the scan
                Handler(Looper.getMainLooper()).postDelayed({
                    callback?.let { verifyTeacherBluetooth(it) }
                }, 1500)
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth is required to mark attendance",
                    Toast.LENGTH_LONG
                ).show()
                callback?.invoke(false)
            }
        }

    // =========================
    // LIVENESS LAUNCHER
    // =========================
    private val livenessLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val photoPath = result.data?.getStringExtra("photo_path")
                if (!photoPath.isNullOrEmpty()) {
                    val file = File(photoPath)
                    val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                    uploadAttendance(uri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Transparent status bar
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowInsetsControllerCompat(
            window, window.decorView
        ).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_attendance)

        // =========================
        // VIEWS
        // =========================
        captureBtn      = findViewById(R.id.captureBtn)
        statusText      = findViewById(R.id.statusText)
        progressBar     = findViewById(R.id.progressBar)
        sessionInfoText = findViewById(R.id.sessionInfoText)
        successCard     = findViewById(R.id.successCard)
        lottieSuccess   = findViewById(R.id.lottieSuccess)

        successCard.visibility = View.GONE

        // =========================
        // SESSION
        // =========================
        sessionManager = SessionManager(this)

        // =========================
        // LOCATION
        // =========================
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        // =========================
        // BUTTON
        // =========================
        captureBtn.setOnClickListener {
            loadActiveSession { success ->
                if (success) {
                    showVerificationDialog()
                }
            }
        }

        fetchAttendanceStatus()
    }


    // All available sessions (when multiple teachers run sessions simultaneously)
    private var allSessions: List<com.example.attendancepro.models.SessionItem> = emptyList()

    private fun loadActiveSession(onLoaded: (Boolean) -> Unit) {

        val classId = sessionManager.getLatestClassId()

        if (classId.isNullOrEmpty()) {
            Toast.makeText(this, "Class not found", Toast.LENGTH_LONG).show()
            onLoaded(false)
            return
        }

        RetrofitClient.instance
            .getActiveSession(classId)
            .enqueue(object : Callback<SessionResponse> {

                override fun onResponse(
                    call: Call<SessionResponse>,
                    response: Response<SessionResponse>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.success) {

                        detectedSessionCode = body.session_code ?: ""
                        expectedSessionUuid = body.session_uuid ?: ""
                        expectedClassroomBeacon = body.classroom_beacon ?: ""
                        expectedOtpCode = body.otp_code ?: ""

                        // Store all sessions for BLE-based matching
                        allSessions = body.all_sessions ?: listOf(
                            com.example.attendancepro.models.SessionItem(
                                session_code = body.session_code,
                                session_uuid = body.session_uuid,
                                bluetooth_name = body.bluetooth_name,
                                classroom_beacon = body.classroom_beacon,
                                otp_code = body.otp_code,
                                session_class_id = body.session_class_id
                            )
                        )

                        sessionInfoText.text =
                            "Session Code: $detectedSessionCode\n\nClassroom Beacon: $expectedClassroomBeacon"

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

                override fun onFailure(call: Call<SessionResponse>, t: Throwable) {
                    Toast.makeText(this@AttendanceActivity, t.message, Toast.LENGTH_LONG).show()
                    onLoaded(false)
                }
            })
    }

    // =========================
    // CHECK PERMISSIONS
    // =========================
    private fun checkPermissionsAndCapture() {

        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val denied = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (denied) {
            requestPermissions(permissions, 100)
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            getLocationAndVerify()
        } else {
            Toast.makeText(
                this,
                "All permissions are required to mark attendance",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================
    // GET LOCATION
    // =========================
    private fun getLocationAndVerify() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->

            if (location == null) {
                Toast.makeText(this, "Location unavailable", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            if (location.isFromMockProvider) {
                Toast.makeText(this, "Mock location detected", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            currentLat = location.latitude
            currentLng = location.longitude
            statusText.text = "📍 GPS Verified"

            // =========================
            // BLE VERIFY
            // =========================
            verifyTeacherBluetooth { detected ->
                if (detected) {
                    statusText.text = "📷 Opening Camera..."
                    openCamera()
                } else {
                    statusText.text = "❌ Classroom Verification Failed"
                }
            }
        }
    }

    // =========================
    // BLE SCAN — Android 14+ compatible
    // =========================
    private fun verifyTeacherBluetooth(callback: (Boolean) -> Unit) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                callback(false)
                return
            }
        }

        try {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            // ====================================
            // AUTO-ENABLE BLUETOOTH (Android 12+)
            // ====================================
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                statusText.text = "🔵 Enabling Bluetooth..."
                pendingBleCallback = callback
                try {
                    val enableIntent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableIntent)
                } catch (e: SecurityException) {
                    pendingBleCallback = null
                    Toast.makeText(this, "Cannot enable Bluetooth automatically", Toast.LENGTH_LONG).show()
                    callback(false)
                }
                return
            }

            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                callback(false)
                return
            }

            statusText.text = "📡 Scanning for classroom beacons..."

            val handler = Handler(Looper.getMainLooper())
            var finished = false
            val detectedBeacons = mutableMapOf<String, Int>()
            var scanCallback: ScanCallback? = null

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanCallback = object : ScanCallback() {

                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    try {
                        val beaconUuid = ParcelUuid(java.util.UUID.fromString("0000BEEF-0000-1000-8000-00805f9b34fb"))
                        val serviceDataBytes = result.scanRecord?.getServiceData(beaconUuid)

                        if (serviceDataBytes != null) {
                            val decodedBeacon = String(serviceDataBytes, Charsets.UTF_8)
                            if (decodedBeacon.startsWith("CLASS_")) {
                                val rssi = result.rssi
                                val existingMax = detectedBeacons[decodedBeacon] ?: -999
                                if (rssi > existingMax) {
                                    detectedBeacons[decodedBeacon] = rssi
                                    println("Detected Beacon: $decodedBeacon, RSSI: $rssi")

                                    // Early exit if we found a match
                                    val matchedSession = allSessions.find { it.classroom_beacon.equals(decodedBeacon, ignoreCase = true) }
                                    if (matchedSession != null && !finished) {
                                        finished = true
                                        try { scanner.stopScan(this) } catch (_: Exception) {}

                                        detectedSessionCode = matchedSession.session_code ?: ""
                                        expectedSessionUuid = matchedSession.session_uuid ?: ""
                                        expectedClassroomBeacon = matchedSession.classroom_beacon ?: ""
                                        expectedOtpCode = matchedSession.otp_code ?: ""

                                        Handler(Looper.getMainLooper()).post {
                                            statusText.text = "📡 Classroom Beacon Detected ($decodedBeacon)"
                                            callback(true)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    results.forEach { onScanResult(0, it) }
                }

                override fun onScanFailed(errorCode: Int) {
                    if (errorCode == 2 && !finished) {
                        handler.postDelayed({
                            if (!finished) {
                                try {
                                    scanner.startScan(null, scanSettings, this)
                                } catch (_: Exception) {
                                    callback(false)
                                }
                            }
                        }, 1000)
                    } else {
                        Toast.makeText(
                            this@AttendanceActivity,
                            "BLE scan failed: $errorCode",
                            Toast.LENGTH_LONG
                        ).show()
                        callback(false)
                    }
                }
            }

            // Start unfiltered scan to find all nearby beacons
            try {
                scanner.startScan(null, scanSettings, scanCallback)
            } catch (e: SecurityException) {
                e.printStackTrace()
                callback(false)
                return
            }

            // ====================================================
            // SCAN WINDOW — 4 seconds to collect and evaluate RSSI
            // ====================================================
            handler.postDelayed({
                if (finished) return@postDelayed
                finished = true

                try {
                    scanner.stopScan(scanCallback)
                } catch (_: Exception) {}

                if (detectedBeacons.isEmpty()) {
                    Toast.makeText(
                        this@AttendanceActivity,
                        "No classroom beacons detected. Make sure Bluetooth is enabled and you are in the classroom.",
                        Toast.LENGTH_LONG
                    ).show()
                    callback(false)
                    return@postDelayed
                }

                // Try to match detected beacons against ALL sessions the student has access to
                // This handles the case where two teachers are running sessions side by side
                var matched = false
                for (session in allSessions) {
                    val beacon = session.classroom_beacon ?: continue
                    val matchingBeacon = detectedBeacons.keys.firstOrNull { it.equals(beacon, ignoreCase = true) }
                    if (matchingBeacon != null) {
                        val rssi = detectedBeacons[matchingBeacon]
                        println("Matched Session Beacon: $matchingBeacon, RSSI: $rssi, ClassID: ${session.session_class_id}")

                        // Switch to the correct session's credentials
                        detectedSessionCode = session.session_code ?: ""
                        expectedSessionUuid = session.session_uuid ?: ""
                        expectedClassroomBeacon = beacon
                        expectedOtpCode = session.otp_code ?: ""

                        statusText.text = "📡 Classroom Beacon Detected ($matchingBeacon)"
                        matched = true
                        callback(true)
                        break
                    }
                }

                if (!matched) {
                    val nearestBeacon = detectedBeacons.maxByOrNull { it.value }?.key
                    val expectedBeacons = allSessions.mapNotNull { it.classroom_beacon }.joinToString(", ")
                    println("No matching beacon found. Nearest: $nearestBeacon. Expected one of: $expectedBeacons")
                    Toast.makeText(
                        this@AttendanceActivity,
                        "Wrong Classroom! Nearest detected: $nearestBeacon. Expected: $expectedBeacons.",
                        Toast.LENGTH_LONG
                    ).show()
                    statusText.text = "❌ Mismatched Classroom"
                    callback(false)
                }
            }, 12000)

        } catch (e: SecurityException) {
            e.printStackTrace()
            callback(false)
        }
    }


    private fun openCamera() {
        val intent = Intent(this, LivenessDetectionActivity::class.java)
        livenessLauncher.launch(intent)
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

        val classroomBeaconBody =

            expectedClassroomBeacon

                .toRequestBody(
                    "text/plain"
                        .toMediaTypeOrNull()
                )

        val otpBody =

            expectedOtpCode

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

                uuidBody,

                classroomBeaconBody,

                otpBody
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

    private fun showVerificationDialog() {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_verification_code)
        dialog.setCancelable(false)

        // Make the system window background fully transparent so only our CardView shows
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        // Apply a beautiful glassmorphism blur effect
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            dialog.window?.attributes?.blurBehindRadius = 30
        } else {
            dialog.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            dialog.window?.attributes?.dimAmount = 0.7f
        }

        val input = dialog.findViewById<android.widget.EditText>(R.id.etVerificationCode)
        val btnCancel = dialog.findViewById<android.widget.Button>(R.id.btnCancel)
        val btnVerify = dialog.findViewById<android.widget.Button>(R.id.btnVerify)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnVerify.setOnClickListener {
            val enteredCode = input.text.toString().trim()
            if (enteredCode.equals(expectedOtpCode, ignoreCase = true)) {
                dialog.dismiss()
                checkPermissionsAndCapture()
            } else {
                Toast.makeText(this, "Incorrect Verification Code!", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
        val window = dialog.window
        if (window != null) {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
