package com.example.attendancepro.activities

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.BluetoothAdapter
import com.example.attendancepro.models.MessageResponse
import androidx.activity.result.contract.ActivityResultContracts

import android.content.Intent
import android.content.pm.PackageManager

import android.graphics.Color

import android.os.Bundle
import android.os.CountDownTimer
import android.os.ParcelUuid

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.drawerlayout.widget.DrawerLayout

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.attendancepro.R
import com.example.attendancepro.adapters.ClassAdapter
import com.example.attendancepro.api.RetrofitClient

import com.example.attendancepro.models.ClassItem
import com.example.attendancepro.models.ClassesResponse
import com.example.attendancepro.models.SessionResponse

import com.example.attendancepro.utils.SessionManager

import java.util.UUID

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity() {

    // =========================
    // BUTTONS
    // =========================
    private lateinit var btnCreateClass: Button

    private lateinit var btnAddStudents: Button

    private lateinit var btnLogout: Button

    private lateinit var btnSession: Button

    // =========================
    // DRAWER
    // =========================
    private lateinit var drawerLayout:
            DrawerLayout

    private lateinit var btnMenu:
            ImageView

    private lateinit var btnSessionToggle:
            Button

    private lateinit var btnCalendar:
            Button

    // =========================
    // BLE
    // =========================
    private var bluetoothLeAdvertiser:
            BluetoothLeAdvertiser? = null

    private var advertiseCallback:
            AdvertiseCallback? = null

    // Survive the "enable BT" round-trip
    private var pendingSessionCode: String? = null
    private var pendingSessionUuid: String? = null
    private var pendingClassroomBeacon: String? = null
    private var originalBluetoothName: String? = null

    // =========================
    // ENABLE BLUETOOTH LAUNCHER (Android 12+)
    // =========================
    private val enableBluetoothLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val code = pendingSessionCode
            val uuid = pendingSessionUuid
            val beacon = pendingClassroomBeacon
            pendingSessionCode = null
            pendingSessionUuid = null
            pendingClassroomBeacon = null
            if (result.resultCode == RESULT_OK && code != null && uuid != null && beacon != null) {
                startBleBroadcast(code, uuid, beacon)
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth is required to broadcast the session",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // =========================
    // SESSION INFO
    // =========================
    private lateinit var tvSessionStatus:
            TextView

    private lateinit var tvBluetoothName:
            TextView

    private lateinit var tvSessionTimer:
            TextView

    private var activeSession = false

    private var countdownTimer:
            CountDownTimer? = null

    // =========================
    // RECYCLER
    // =========================
    private lateinit var recyclerClasses:
            RecyclerView

    // =========================
    // SESSION
    // =========================
    private lateinit var sessionManager:
            SessionManager

    // =========================
    // CLASS LIST
    // =========================
    private var classList:
            List<ClassItem> = listOf()

    private var selectedClassId: String? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        // =========================
        // EDGE TO EDGE
        // =========================
        WindowCompat.setDecorFitsSystemWindows(

            window,
            false
        )

        window.statusBarColor =
            Color.TRANSPARENT

        val controller =
            WindowInsetsControllerCompat(

                window,
                window.decorView
            )

        controller.isAppearanceLightStatusBars =
            false

        setContentView(
            R.layout.activity_admin_dashboard
        )

        // =========================
        // SESSION
        // =========================
        sessionManager =
            SessionManager(this)

        // =========================
        // VIEWS
        // =========================
        drawerLayout =
            findViewById(R.id.drawerLayout)

        btnMenu =
            findViewById(R.id.btnMenu)

        btnCreateClass =
            findViewById(R.id.btnCreateClass)

        btnAddStudents =
            findViewById(R.id.btnAddStudents)

        btnLogout =
            findViewById(R.id.btnLogout)

        btnSession =
            findViewById(R.id.btnSession)

        btnSessionToggle =
            findViewById(R.id.btnSessionToggle)

        btnCalendar =
            findViewById(R.id.btnCalendar)

        tvSessionStatus =
            findViewById(R.id.tvSessionStatus)

        tvBluetoothName =
            findViewById(R.id.tvBluetoothName)

        tvSessionTimer =
            findViewById(R.id.tvSessionTimer)

        recyclerClasses =
            findViewById(R.id.recyclerClasses)

        recyclerClasses.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // =========================
        // LOAD CLASSES
        // =========================
        loadClasses()

        // =========================
        // MENU
        // =========================
        btnMenu.setOnClickListener {

            drawerLayout.openDrawer(
                GravityCompat.END
            )
        }

        // =========================
        // SESSION BUTTONS
        // =========================
        btnSession.setOnClickListener {

            toggleSession()
        }

        btnSessionToggle.setOnClickListener {

            toggleSession()
        }

        btnCalendar.setOnClickListener {

            startActivity(

                Intent(

                    this,

                    CalendarActivity::class.java
                )
            )
        }

        // =========================
        // CREATE CLASS
        // =========================
        btnCreateClass.setOnClickListener {

            startActivity(

                Intent(

                    this,

                    CreateClassActivity::class.java
                )
            )
        }

        // =========================
        // ADD STUDENTS
        // =========================
        btnAddStudents.setOnClickListener {

            if (selectedClassId.isNullOrBlank()) {

                Toast.makeText(
                    this,
                    "Select a class first",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            val selectedClass = classList.firstOrNull {

                it.class_id == selectedClassId
            }

            if (
                selectedClass == null ||
                selectedClass.class_id.isBlank()
            ) {

                Toast.makeText(
                    this,
                    "Select a class first",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            sessionManager.saveLatestClassId(
                selectedClass.class_id
            )

            sessionManager.saveLatestClassName(
                selectedClass.class_name
            )

            val intent = Intent(
                this,
                AddStudentsActivity::class.java
            )

            intent.putExtra(
                "CLASS_ID",
                selectedClass.class_id
            )

            intent.putExtra(
                "CLASS_NAME",
                selectedClass.class_name
            )

            startActivity(intent)
        }

        // =========================
        // LOGOUT
        // =========================
        btnLogout.setOnClickListener {

            sessionManager.clearSession()

            startActivity(

                Intent(

                    this,

                    AdminLoginActivity::class.java
                )
            )

            finish()
        }
    }

    // =========================
    // LOAD CLASSES
    // =========================
    private fun loadClasses() {

        RetrofitClient.instance

            .getAllClasses(
                adminId = sessionManager.getAdminId() ?: ""
            )

            .enqueue(object :
                Callback<ClassesResponse> {

                override fun onResponse(

                    call: Call<ClassesResponse>,

                    response:
                    Response<ClassesResponse>

                ) {

                    if (

                        response.isSuccessful &&

                        response.body() != null
                    ) {

                        classList =
                            response.body()!!.classes

                        val defaultClass =
                            classList.firstOrNull {
                                it.class_id.isNotBlank()
                            }

                        selectedClassId =
                            defaultClass?.class_id

                        if (defaultClass != null) {

                            sessionManager.saveLatestClassId(
                                defaultClass.class_id
                            )

                            sessionManager.saveLatestClassName(
                                defaultClass.class_name
                            )
                        }

                        recyclerClasses.adapter =

                            ClassAdapter(

                                this@AdminDashboardActivity,

                                classList

                            ) { selectedClass ->

                                selectedClassId =
                                    selectedClass.class_id

                                if (selectedClass.class_id.isNotBlank()) {

                                    sessionManager.saveLatestClassId(
                                        selectedClass.class_id
                                    )

                                    sessionManager.saveLatestClassName(
                                        selectedClass.class_name
                                    )
                                }

                                Toast.makeText(

                                    this@AdminDashboardActivity,

                                    "Selected: ${selectedClass.class_name}",

                                    Toast.LENGTH_SHORT

                                ).show()
                            }
                    }
                }

                override fun onFailure(

                    call: Call<ClassesResponse>,

                    t: Throwable

                ) {

                    Toast.makeText(

                        this@AdminDashboardActivity,

                        t.message,

                        Toast.LENGTH_LONG

                    ).show()
                }
            })
    }

    // =========================
    // TOGGLE SESSION
    // =========================
    private fun toggleSession() {

        if (!activeSession) {

            startSession()

        } else {

            stopSession()
        }
    }

    // =========================
    // START SESSION
    // =========================
    private fun startSession() {

        if (classList.isEmpty()) {

            Toast.makeText(

                this,

                "Create class first",

                Toast.LENGTH_LONG

            ).show()

            return
        }

        val classId =
            selectedClassId

        if (classId == null) {

            Toast.makeText(
                this,
                "Select a class first",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val selectedClass = classList.firstOrNull { it.class_id == classId }
        val classroomBeacon = if (selectedClass != null) {
            "CLASS_${selectedClass.department.uppercase().replace(" ", "_").trim()}_${selectedClass.section.uppercase().replace(" ", "_").trim()}"
        } else {
            "CLASS_CSE_A"
        }

        RetrofitClient.instance

            .startSession(classId, classroomBeacon)

            .enqueue(object :
                Callback<SessionResponse> {

                override fun onResponse(

                    call: Call<SessionResponse>,

                    response: Response<SessionResponse>

                ) {

                    if (

                        response.isSuccessful &&

                        response.body() != null
                    ) {

                        val data =
                            response.body()!!

                        activeSession = true

                        btnSession.text =
                            "End Session"

                        btnSessionToggle.text =
                            "End Session"

                        tvSessionStatus.text =
                            "🟢 ACTIVE"

                        tvSessionStatus.setTextColor(
                            Color.GREEN
                        )

                        tvBluetoothName.text =
                            "BLE Session: ${data.session_code}\n\nVerification Code: ${data.otp_code}"

                        // =========================
                        // START BLE BROADCAST
                        // =========================
                        startBleBroadcast(
                            data.session_code!!,
                            data.session_uuid!!,
                            data.classroom_beacon ?: "CLASS_CSE_A"
                        )

                        // =========================
                        // START TIMER
                        // =========================
                        startCountdown()

                        Toast.makeText(

                            this@AdminDashboardActivity,

                            "Session Started",

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }

                override fun onFailure(

                    call: Call<SessionResponse>,

                    t: Throwable

                ) {

                    Toast.makeText(

                        this@AdminDashboardActivity,

                        t.message,

                        Toast.LENGTH_LONG

                    ).show()
                }
            })
    }

    // =========================
    // STOP SESSION
    // =========================
    private fun stopSession() {

        if (classList.isEmpty()) {

            activeSession = false

            countdownTimer?.cancel()

            stopBleBroadcast()

            return
        }

        val classId =
            selectedClassId ?: return

        RetrofitClient.instance

            .stopSession(classId)

            .enqueue(object :
                Callback<MessageResponse> {

                override fun onResponse(

                    call: Call<MessageResponse>,

                    response: Response<MessageResponse>

                ) {

                    activeSession = false

                    btnSession.text =
                        "Start Session"

                    btnSessionToggle.text =
                        "Start Session"

                    tvSessionStatus.text =
                        "🔴 INACTIVE"

                    tvSessionStatus.setTextColor(
                        Color.RED
                    )

                    tvBluetoothName.text =
                        "Bluetooth:\nN/A"

                    tvSessionTimer.text =
                        "⏱ No Active Session"

                    countdownTimer?.cancel()

                    stopBleBroadcast()

                    Toast.makeText(

                        this@AdminDashboardActivity,

                        response.body()?.message
                            ?: "Session Ended",

                        Toast.LENGTH_LONG

                    ).show()
                }

                override fun onFailure(

                    call: Call<MessageResponse>,

                    t: Throwable

                ) {

                    Toast.makeText(

                        this@AdminDashboardActivity,

                        "Failed to stop session: ${t.message}",

                        Toast.LENGTH_LONG

                    ).show()
                }
            })
    }

    // =========================
    // START BLE
    // =========================
    private fun startBleBroadcast(
        sessionCode: String,
        sessionUuid: String,
        classroomBeacon: String
    ) {

        val advertisePermission =

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission
                    .BLUETOOTH_ADVERTISE
            )

        val connectPermission =

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission
                    .BLUETOOTH_CONNECT
            )

        if (

            advertisePermission !=
            PackageManager.PERMISSION_GRANTED ||

            connectPermission !=
            PackageManager.PERMISSION_GRANTED
        ) {

            // Store values to resume after permissions dialog
            pendingSessionCode = sessionCode
            pendingSessionUuid = sessionUuid
            pendingClassroomBeacon = classroomBeacon

            ActivityCompat.requestPermissions(

                this,

                arrayOf(

                    Manifest.permission
                        .BLUETOOTH_ADVERTISE,

                    Manifest.permission
                        .BLUETOOTH_CONNECT
                ),

                200
            )

            return
        }

        try {

            val bluetoothManager =

                getSystemService(
                    BLUETOOTH_SERVICE
                ) as BluetoothManager

            val bluetoothAdapter =
                bluetoothManager.adapter

            if (bluetoothAdapter == null) {

                Toast.makeText(

                    this,

                    "Bluetooth not supported",

                    Toast.LENGTH_LONG

                ).show()

                return
            }

            try {

                if (!bluetoothAdapter.isEnabled) {

                    // Store values to resume after BT enable dialog
                    pendingSessionCode = sessionCode
                    pendingSessionUuid = sessionUuid
                    pendingClassroomBeacon = classroomBeacon

                    try {
                        enableBluetoothLauncher.launch(
                            Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        )
                    } catch (e: SecurityException) {
                        pendingSessionCode = null
                        pendingSessionUuid = null
                        pendingClassroomBeacon = null
                        Toast.makeText(
                            this,
                            "Cannot enable Bluetooth automatically",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return
                }

            } catch (e: SecurityException) {

                e.printStackTrace()

                return
            }

            bluetoothLeAdvertiser =
                bluetoothAdapter
                    .bluetoothLeAdvertiser

            if (bluetoothLeAdvertiser == null) {

                Toast.makeText(

                    this,

                    "BLE advertising unsupported",

                    Toast.LENGTH_LONG

                ).show()

                return
            }

            // Save original name and change name to simulate beacon
            try {
                if (originalBluetoothName == null) {
                    originalBluetoothName = bluetoothAdapter.name
                }
                bluetoothAdapter.name = classroomBeacon
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            val settings =

                AdvertiseSettings.Builder()

                    .setAdvertiseMode(

                        AdvertiseSettings
                            .ADVERTISE_MODE_LOW_LATENCY
                    )

                    .setConnectable(false)

                    .setTimeout(0)

                    .setTxPowerLevel(

                        AdvertiseSettings
                            .ADVERTISE_TX_POWER_HIGH
                    )

                    .build()

            val data =

                AdvertiseData.Builder()

                    .setIncludeDeviceName(true)

                    .build()

            advertiseCallback =

                object : AdvertiseCallback() {

                    override fun onStartSuccess(
                        settingsInEffect:
                        AdvertiseSettings?
                    ) {

                        Toast.makeText(

                            this@AdminDashboardActivity,

                            "BLE Broadcasting Started",

                            Toast.LENGTH_LONG

                        ).show()
                    }

                    override fun onStartFailure(
                        errorCode: Int
                    ) {

                        Toast.makeText(

                            this@AdminDashboardActivity,

                            "BLE Failed: $errorCode",

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }

            try {

                bluetoothLeAdvertiser
                    ?.startAdvertising(

                        settings,

                        data,

                        advertiseCallback
                    )

            } catch (e: SecurityException) {

                e.printStackTrace()

                Toast.makeText(

                    this,

                    "Bluetooth permission denied",

                    Toast.LENGTH_LONG

                ).show()
            }

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(

                this,

                "BLE error occurred",

                Toast.LENGTH_LONG

            ).show()
        }
    }

// =========================
// STOP BLE
// =========================
    private fun stopBleBroadcast() {

        val connectPermission =

            ContextCompat.checkSelfPermission(

                this,

                Manifest.permission
                    .BLUETOOTH_CONNECT
            )

        if (

            connectPermission !=
            PackageManager.PERMISSION_GRANTED
        ) {

            return
        }

        try {

            bluetoothLeAdvertiser
                ?.stopAdvertising(
                    advertiseCallback
                )

        } catch (e: SecurityException) {

            e.printStackTrace()

        } catch (e: Exception) {

            e.printStackTrace()
        }

        // Restore original system Bluetooth adapter name
        try {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            if (bluetoothAdapter != null && originalBluetoothName != null) {
                bluetoothAdapter.name = originalBluetoothName
                originalBluetoothName = null
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // =========================
    // TIMER
    // =========================
    private fun startCountdown() {

        countdownTimer?.cancel()

        countdownTimer =

            object : CountDownTimer(

                600000,
                1000

            ) {

                override fun onTick(
                    millisUntilFinished: Long
                ) {

                    val minutes =

                        millisUntilFinished /
                                1000 / 60

                    val seconds =

                        millisUntilFinished /
                                1000 % 60

                    tvSessionTimer.text =

                        String.format(

                            "⏱ %02d:%02d remaining",

                            minutes,
                            seconds
                        )
                }

                override fun onFinish() {

                    stopSession()
                }

            }.start()
    }
    override fun onResume() {

        super.onResume()

        loadClasses()
    }

    override fun onDestroy() {
        stopBleBroadcast()
        super.onDestroy()
    }

    // =========================
    // PERMISSION RESULT CALLBACK
    // =========================
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // If permissions granted, start broadcasting using the pending variables
                if (pendingSessionCode != null && pendingSessionUuid != null && pendingClassroomBeacon != null) {
                    startBleBroadcast(pendingSessionCode!!, pendingSessionUuid!!, pendingClassroomBeacon!!)
                } else {
                    Toast.makeText(this, "Please click 'Start Session' again", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bluetooth permissions are required to start a session", Toast.LENGTH_LONG).show()
            }
        }
    }
}
