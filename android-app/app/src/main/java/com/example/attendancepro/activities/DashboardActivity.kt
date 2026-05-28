package com.example.attendancepro.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout

import com.airbnb.lottie.LottieAnimationView
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.example.attendancepro.models.AttendanceHistoryResponse
import com.example.attendancepro.models.AttendanceResponse
import com.example.attendancepro.utils.SessionManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var markBtn: MaterialButton
    private lateinit var nextTime: TextView
    private lateinit var historyText: TextView
    private lateinit var totalAttendance: TextView
    private lateinit var successAnim: LottieAnimationView

    private lateinit var imageFile: File
    private lateinit var imageUri: Uri

    private val CAMERA_REQUEST = 1001

    private var remainingSeconds = 0

    private val handler =
        Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // 🔥 STATUS BAR
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        window.statusBarColor =
            android.graphics.Color.TRANSPARENT

        window.decorView.systemUiVisibility =

            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_dashboard)

        // =========================
        // 🔥 DRAWER
        // =========================
        val drawerLayout =
            findViewById<DrawerLayout>(
                R.id.drawerLayout
            )

        val toolbar =
            findViewById<MaterialToolbar>(
                R.id.toolbar
            )

        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(

            this,
            drawerLayout,
            toolbar,

            R.string.app_name,
            R.string.app_name
        )

        drawerLayout.addDrawerListener(toggle)

        toggle.syncState()

        // =========================
        // 🔥 PROFILE CLICK
        // =========================
        findViewById<View>(
            R.id.profileBtn
        ).setOnClickListener {

            drawerLayout.closeDrawer(
                GravityCompat.START
            )

            Handler(Looper.getMainLooper())
                .postDelayed({

                    startActivity(

                        Intent(
                            this,
                            ProfileActivity::class.java
                        )
                    )

                }, 250)
        }

        // =========================
        // 🔥 BACK HANDLER
        // =========================
        onBackPressedDispatcher.addCallback(

            this,

            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    if (
                        drawerLayout.isDrawerOpen(
                            GravityCompat.START
                        )
                    ) {

                        drawerLayout.closeDrawer(
                            GravityCompat.START
                        )

                    } else {

                        finish()
                    }
                }
            })

        val session =
            SessionManager(this)

        val userName =
            findViewById<TextView>(
                R.id.userName
            )

        val profileName =
            findViewById<TextView>(
                R.id.profileName
            )

        markBtn =
            findViewById(R.id.markBtn)

        nextTime =
            findViewById(R.id.nextTime)

        historyText =
            findViewById(R.id.historyText)

        totalAttendance =
            findViewById(R.id.totalAttendance)

        successAnim =
            findViewById(R.id.successAnim)

        userName.text =
            "Welcome ${session.getName()} 👋"

        profileName.text =
            session.getName()

        markBtn.setOnClickListener {

            openCamera()
        }

        loadHistory()

        checkCooldownOnStart()
    }

    // =========================
    // 📸 CAMERA
    // =========================
    private fun openCamera() {

        val intent =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        imageFile = createImageFile()

        imageUri = FileProvider.getUriForFile(

            this,

            "${packageName}.provider",

            imageFile
        )

        intent.putExtra(
            MediaStore.EXTRA_OUTPUT,
            imageUri
        )

        startActivityForResult(
            intent,
            CAMERA_REQUEST
        )
    }

    private fun createImageFile(): File {

        val timeStamp =

            SimpleDateFormat(

                "yyyyMMdd_HHmmss",

                Locale.getDefault()

            ).format(Date())

        val storageDir =
            getExternalFilesDir("Pictures")

        return File.createTempFile(

            "JPEG_${timeStamp}_",

            ".jpg",

            storageDir
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (

            requestCode == CAMERA_REQUEST &&

            resultCode == Activity.RESULT_OK
        ) {

            getLocationAndMark()
        }
    }

    // =========================
    // 📍 LOCATION
    // =========================
    private fun getLocationAndMark() {

        val fusedLocationClient =

            LocationServices
                .getFusedLocationProviderClient(this)

        if (

            ActivityCompat.checkSelfPermission(

                this,

                Manifest.permission.ACCESS_FINE_LOCATION

            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(

                this,

                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),

                101
            )

            return
        }

        markBtn.text = "Processing..."

        markBtn.isEnabled = false

        fusedLocationClient.lastLocation

            .addOnSuccessListener { location ->

                if (location != null) {

                    val lat =

                        location.latitude
                            .toString()
                            .toRequestBody()

                    val lng =

                        location.longitude
                            .toString()
                            .toRequestBody()

                    callMarkAPI(lat, lng)

                } else {

                    Toast.makeText(

                        this,

                        "Location error",

                        Toast.LENGTH_SHORT

                    ).show()

                    resetButton()
                }
            }
    }

    // =========================
    // 🚀 MARK API
    // =========================
    private fun callMarkAPI(
        lat: RequestBody,
        lng: RequestBody
    ) {

        val filePart =

            MultipartBody.Part
                .createFormData(

                    "file",

                    imageFile.name,

                    imageFile.asRequestBody(
                        "image/*"
                            .toMediaTypeOrNull()
                    )
                )

        RetrofitClient.instance

            .markAttendance(
                filePart,
                lat,
                lng
            )

            .enqueue(object :
                Callback<AttendanceResponse> {

                override fun onResponse(
                    call: Call<AttendanceResponse>,
                    response: Response<AttendanceResponse>
                ) {

                    Log.d(
                        "RAW_RESPONSE",
                        response.toString()
                    )

                    if (!response.isSuccessful) {

                        Toast.makeText(

                            this@DashboardActivity,

                            "Server Error ${response.code()}",

                            Toast.LENGTH_LONG

                        ).show()

                        resetButton()

                        return
                    }

                    val res = response.body()

                    Log.d(
                        "ATTENDANCE_RESPONSE",
                        res.toString()
                    )

                    // ✅ SUCCESS
                    if (

                        res != null &&

                        res.status != null &&

                        res.status.equals(
                            "Present",
                            ignoreCase = true
                        )
                    ) {

                        showSuccessAnimation()

                        Toast.makeText(

                            this@DashboardActivity,

                            res.message
                                ?: "Attendance Marked Successfully",

                            Toast.LENGTH_LONG

                        ).show()

                        markBtn.text =
                            "Already Marked"

                        markBtn.isEnabled = false

                        startTimer(3600)

                        loadHistory()

                    }

                    // ✅ COOLDOWN
                    else if (

                        res?.error?.contains(
                            "Wait",
                            ignoreCase = true
                        ) == true
                    ) {

                        Toast.makeText(

                            this@DashboardActivity,

                            res.error,

                            Toast.LENGTH_LONG

                        ).show()

                        startTimer(3600)

                        markBtn.text =
                            "Already Marked"

                        markBtn.isEnabled = false
                    }

                    // ❌ ERROR
                    else {

                        Toast.makeText(

                            this@DashboardActivity,

                            res?.error
                                ?: "Error",

                            Toast.LENGTH_LONG

                        ).show()

                        resetButton()
                    }
                }

                override fun onFailure(
                    call: Call<AttendanceResponse>,
                    t: Throwable
                ) {

                    Toast.makeText(

                        this@DashboardActivity,

                        t.message,

                        Toast.LENGTH_LONG

                    ).show()

                    resetButton()
                }
            })
    }

    // =========================
    // 🎉 LOTTIE
    // =========================
    private fun showSuccessAnimation() {

        successAnim.cancelAnimation()

        successAnim.visibility =
            View.VISIBLE

        successAnim.alpha = 1f

        successAnim.progress = 0f

        successAnim.playAnimation()

        handler.postDelayed({

            successAnim.animate()

                .alpha(0f)

                .setDuration(300)

                .withEndAction {

                    successAnim.cancelAnimation()

                    successAnim.visibility =
                        View.GONE

                    successAnim.alpha = 1f
                }

        }, 1500)
    }

    // =========================
    // 📜 HISTORY
    // =========================
    private fun loadHistory() {

        RetrofitClient.instance

            .getHistory()

            .enqueue(object :
                Callback<AttendanceHistoryResponse> {

                override fun onResponse(
                    call: Call<AttendanceHistoryResponse>,
                    response: Response<AttendanceHistoryResponse>
                ) {

                    val res = response.body()

                    if (

                        res != null &&

                        res.history.isNotEmpty()
                    ) {

                        var present = 0

                        val builder =
                            StringBuilder()

                        for (i in res.history) {

                            builder.append(

                                "${i.date} → ${i.status}\n"
                            )

                            if (

                                i.status.equals(
                                    "Present",
                                    ignoreCase = true
                                )
                            ) {

                                present++
                            }
                        }

                        historyText.text =
                            builder.toString()

                        totalAttendance.text =

                            "Total Attendance: ${
                                (present * 100) /
                                        res.history.size
                            }%"
                    }
                }

                override fun onFailure(
                    call: Call<AttendanceHistoryResponse>,
                    t: Throwable
                ) {

                    historyText.text = "Error"
                }
            })
    }

    // =========================
    // ⏳ TIMER
    // =========================
    private fun startTimer(
        seconds: Int
    ) {

        remainingSeconds = seconds

        handler.post(object : Runnable {

            override fun run() {

                val mins =
                    remainingSeconds / 60

                nextTime.text =
                    "Next mark in: $mins min"

                if (remainingSeconds > 0) {

                    remainingSeconds--

                    handler.postDelayed(
                        this,
                        1000
                    )

                } else {

                    resetButton()
                }
            }
        })
    }

    // =========================
    // 🔄 RESET
    // =========================
    private fun resetButton() {

        markBtn.text =
            "Mark Attendance"

        markBtn.isEnabled = true
    }

    // =========================
    // 🧠 CHECK COOLDOWN
    // =========================
    private fun checkCooldownOnStart() {

        RetrofitClient.instance

            .getStatus()

            .enqueue(object :
                Callback<AttendanceResponse> {

                override fun onResponse(
                    call: Call<AttendanceResponse>,
                    response: Response<AttendanceResponse>
                ) {

                    val res = response.body()

                    if (

                        res?.status.equals(
                            "Present",
                            ignoreCase = true
                        )
                    ) {

                        startTimer(3600)

                        markBtn.text =
                            "Already Marked"

                        markBtn.isEnabled =
                            false
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