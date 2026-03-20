package com.example.attendancepro.activities

import android.Manifest
import com.example.attendancepro.models.AttendanceResponse
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import com.google.android.gms.location.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class AttendanceActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var captureBtn: Button
    private lateinit var statusText: TextView
    private lateinit var previewImage: ImageView

    private lateinit var photoUri: Uri
    private lateinit var imageFile: File

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        progressBar = findViewById(R.id.progressBar)
        captureBtn = findViewById(R.id.captureBtn)
        statusText = findViewById(R.id.statusText)
        previewImage = findViewById(R.id.previewImage)

        statusText.text = "Ready to mark attendance"

        captureBtn.setOnClickListener {
            setLoading(true)
            statusText.text = "Opening Camera..."
            checkPermissions()
        }
    }

    // 🔄 Loading UI
    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        captureBtn.isEnabled = !isLoading
        captureBtn.text = if (isLoading) "Processing..." else "Mark Attendance"
    }

    // 🔐 Permission check
    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permissions.all {
                ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 100)
        }
    }

    // 📸 Open Camera
    private fun openCamera() {

        val file = File(getExternalFilesDir(null), "selfie_${System.currentTimeMillis()}.jpg")
        imageFile = file

        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        cameraLauncher.launch(intent)
    }

    // 📸 Camera result
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                if (imageFile.exists()) {

                    previewImage.setImageURI(photoUri)

                    AlertDialog.Builder(this)
                        .setTitle("Confirm Photo")
                        .setMessage("Use this photo?")
                        .setPositiveButton("Yes") { _, _ ->
                            statusText.text = "Getting location..."
                            getLocation()
                        }
                        .setNegativeButton("Retake") { _, _ ->
                            openCamera()
                        }
                        .show()

                } else {
                    setLoading(false)
                    Toast.makeText(this, "Image capture failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                setLoading(false)
            }
        }

    // 📍 Setup GPS
    private fun setupLocationRequest() {

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation

                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude

                    fusedLocationClient.removeLocationUpdates(this)

                    statusText.text = "Uploading..."
                    sendAttendance()
                }
            }
        }
    }

    // 📍 Get Location
    private fun getLocation() {

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            setLoading(false)
            statusText.text = "Enable GPS"
            Toast.makeText(this, "Enable GPS first", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            setLoading(false)
            return
        }

        setupLocationRequest()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // 🌐 API CALL
    private fun sendAttendance() {

        Log.d("FILE_CHECK", "Size: ${imageFile.length()}")

        val requestFile =
            imageFile.asRequestBody("image/*".toMediaTypeOrNull())

        val body =
            MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

        val latBody =
            latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val lngBody =
            longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        RetrofitClient.instance.markAttendance(body, latBody, lngBody)
            .enqueue(object : Callback<AttendanceResponse> {

                override fun onResponse(
                    call: Call<AttendanceResponse>,
                    response: Response<AttendanceResponse>
                ) {
                    setLoading(false)

                    if (!response.isSuccessful) {
                        statusText.text = "Server Error ❌"
                        Toast.makeText(this@AttendanceActivity, "HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                        return
                    }

                    val res = response.body()
                    Log.d("API_RESPONSE", res.toString())

                    if (res?.status == "present") {

                        statusText.text = "Attendance Marked ✅"

                        captureBtn.text = "Already Marked"
                        captureBtn.isEnabled = false

                        AlertDialog.Builder(this@AttendanceActivity)
                            .setTitle("Success")
                            .setMessage("You are marked present ✅")
                            .setPositiveButton("Go to Dashboard") { _, _ ->
                                val intent = Intent(this@AttendanceActivity, DashboardActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                                finish()
                            }
                            .show()

                    } else {

                        val errorMsg = res?.error ?: "Unknown error"

                        statusText.text = errorMsg

                        Toast.makeText(
                            this@AttendanceActivity,
                            errorMsg,
                            Toast.LENGTH_LONG
                        ).show()

                        if (errorMsg.contains("Already")) {
                            captureBtn.text = "Already Marked"
                            captureBtn.isEnabled = false
                        }
                    }
                }

                override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                    setLoading(false)

                    Log.e("API_ERROR", "Error: ${t.message}", t)

                    statusText.text = "Error ❌"

                    Toast.makeText(
                        this@AttendanceActivity,
                        "Network Error: ${t.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}