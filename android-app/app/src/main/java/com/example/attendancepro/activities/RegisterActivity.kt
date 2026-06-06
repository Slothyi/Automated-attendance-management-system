package com.example.attendancepro.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import com.example.attendancepro.utils.ParticleView
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.exifinterface.media.ExifInterface
import com.example.attendancepro.R
import com.example.attendancepro.api.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class RegisterActivity : AppCompatActivity() {

    private lateinit var imageFile: File
    private lateinit var photoUri: Uri

    private lateinit var name: EditText
    private lateinit var roll: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var captureBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var selfiePreview: ImageView
    private lateinit var btnVerifyEmail: TextView
    private lateinit var ivVerifiedTick: ImageView

    private var isEmailVerified = false
    private val pollingHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pollingRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔥 EDGE TO EDGE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false

        setContentView(R.layout.activity_register)

        // =========================
        // 🔥 PARTICLE BACKGROUND (ADDED)
        // =========================
        val container = findViewById<FrameLayout>(R.id.particleContainer)

        container.addView(
            ParticleView(this),
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // =========================
        // NO PADDING FIX
        // =========================
        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets -> insets }

        // INIT
        name = findViewById(R.id.name)
        roll = findViewById(R.id.roll)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        captureBtn = findViewById(R.id.captureBtn)
        registerBtn = findViewById(R.id.registerBtn)
        selfiePreview = findViewById(R.id.selfiePreview)
        btnVerifyEmail = findViewById(R.id.btnVerifyEmail)
        ivVerifiedTick = findViewById(R.id.ivVerifiedTick)

        btnVerifyEmail.setOnClickListener {
            val emailText = email.text.toString().trim()
            if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendVerificationEmail(emailText)
        }

        captureBtn.setOnClickListener {
            checkCameraPermission()
        }

        registerBtn.setOnClickListener {
            registerUser()
        }
    }

    // =========================
    // CAMERA PERMISSION
    // =========================
    private fun checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================
    // OPEN CAMERA
    // =========================
    private fun openCamera() {
        val intent = Intent(this, LivenessDetectionActivity::class.java)
        livenessLauncher.launch(intent)
    }

    private val livenessLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val path = result.data?.getStringExtra("photo_path")
                if (!path.isNullOrEmpty()) {
                    imageFile = File(path)
                    val fixedBitmap = fixRotation(imageFile)
                    overwriteImage(fixedBitmap, imageFile)
                    selfiePreview.setImageBitmap(fixedBitmap)
                    Toast.makeText(this, "Liveness verified & photo captured", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // =========================
    // ROTATION FIX
    // =========================
    private fun fixRotation(file: File): Bitmap {

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        val exif = ExifInterface(file.absolutePath)

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun overwriteImage(bitmap: Bitmap, file: File) {
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
    }

    // =========================
    // REGISTER API
    // =========================
    private fun registerUser() {

        if (!isEmailVerified) {
            Toast.makeText(this, "Please verify your email first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!::imageFile.isInitialized) {
            Toast.makeText(this, "Capture photo first", Toast.LENGTH_SHORT).show()
            return
        }

        val emailInput = email.text.toString().trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())

        val filePart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

        val nameBody = name.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val rollBody = roll.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = emailInput.toRequestBody("text/plain".toMediaTypeOrNull())
        val passBody = password.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        registerBtn.isEnabled = false
        registerBtn.text = "Registering..."

        RetrofitClient.instance.register(nameBody, rollBody, emailBody, passBody, filePart)
            .enqueue(object : Callback<Map<String, Any>> {

                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {

                    registerBtn.isEnabled = true
                    registerBtn.text = "Register"

                    val res = response.body()

                    if (response.isSuccessful && res != null) {

                        val success = res["success"]
                        val message = res["message"]?.toString() ?: "Unknown error"

                        if (success == true) {
                            Toast.makeText(this@RegisterActivity, "Registration Successful ✅", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_LONG).show()
                        }

                    } else {
                        Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    registerBtn.isEnabled = true
                    registerBtn.text = "Register"
                    Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun sendVerificationEmail(emailText: String) {
        btnVerifyEmail.isEnabled = false

        RetrofitClient.instance.sendVerificationEmail(emailText)
            .enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(
                    call: Call<Map<String, Any>>,
                    response: Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        val success = response.body()!!["success"] as? Boolean ?: false
                        val message = response.body()!!["message"]?.toString() ?: "Sent"
                        if (success) {
                            Toast.makeText(this@RegisterActivity, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                            btnVerifyEmail.animate().alpha(0f).setDuration(150).withEndAction {
                                btnVerifyEmail.text = "Sent"
                                btnVerifyEmail.animate().alpha(1f).setDuration(150).start()
                            }.start()
                            startVerificationPolling(emailText)
                        } else {
                            btnVerifyEmail.isEnabled = true
                            btnVerifyEmail.text = "Verify"
                            Toast.makeText(this@RegisterActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        btnVerifyEmail.isEnabled = true
                        btnVerifyEmail.text = "Verify"
                        Toast.makeText(this@RegisterActivity, "Failed to send verification email", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    btnVerifyEmail.isEnabled = true
                    btnVerifyEmail.text = "Verify"
                    Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun startVerificationPolling(emailText: String) {
        stopVerificationPolling()

        pollingRunnable = object : Runnable {
            override fun run() {
                RetrofitClient.instance.checkVerificationStatus(emailText)
                    .enqueue(object : Callback<Map<String, Any>> {
                        override fun onResponse(
                            call: Call<Map<String, Any>>,
                            response: Response<Map<String, Any>>
                        ) {
                            if (response.isSuccessful && response.body() != null) {
                                val verified = response.body()!!["verified"] as? Boolean ?: false
                                if (verified) {
                                    isEmailVerified = true
                                    btnVerifyEmail.visibility = View.GONE
                                    ivVerifiedTick.visibility = View.VISIBLE
                                    Toast.makeText(this@RegisterActivity, "Email is verified", Toast.LENGTH_SHORT).show()
                                    email.isEnabled = false
                                    stopVerificationPolling()
                                } else {
                                    pollingRunnable?.let { pollingHandler.postDelayed(it, 2000) }
                                }
                            } else {
                                pollingRunnable?.let { pollingHandler.postDelayed(it, 2000) }
                            }
                        }

                        override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                            pollingRunnable?.let { pollingHandler.postDelayed(it, 2000) }
                        }
                    })
            }
        }
        pollingHandler.post(pollingRunnable!!)
    }

    private fun stopVerificationPolling() {
        pollingRunnable?.let {
            pollingHandler.removeCallbacks(it)
        }
        pollingRunnable = null
    }

    override fun onDestroy() {
        stopVerificationPolling()
        super.onDestroy()
    }
}