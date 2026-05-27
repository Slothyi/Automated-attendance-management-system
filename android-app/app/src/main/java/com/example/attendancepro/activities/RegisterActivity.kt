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

        imageFile = File(getExternalFilesDir(null), "register_${System.currentTimeMillis()}.jpg")

        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        cameraLauncher.launch(intent)
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK && imageFile.exists()) {

                val fixedBitmap = fixRotation(imageFile)
                overwriteImage(fixedBitmap, imageFile)

                selfiePreview.setImageBitmap(fixedBitmap)

                Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show()
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

        if (!::imageFile.isInitialized) {
            Toast.makeText(this, "Capture photo first", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())

        val filePart = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

        val nameBody = name.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val rollBody = roll.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val passBody = password.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        registerBtn.isEnabled = false
        registerBtn.text = "Registering..."

        RetrofitClient.instance.register(nameBody, rollBody, emailBody, passBody, filePart)
            .enqueue(object : Callback<Map<String, String>> {

                override fun onResponse(
                    call: Call<Map<String, String>>,
                    response: Response<Map<String, String>>
                ) {

                    registerBtn.isEnabled = true
                    registerBtn.text = "Register"

                    val res = response.body()

                    if (response.isSuccessful && res != null) {

                        val error = res["error"]
                        val message = res["message"]

                        if (error != null) {
                            Toast.makeText(this@RegisterActivity, error, Toast.LENGTH_LONG).show()
                        } else if (message != null) {
                            Toast.makeText(this@RegisterActivity, "Registration Successful ✅", Toast.LENGTH_LONG).show()
                            finish()
                        }

                    } else {
                        Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    registerBtn.isEnabled = true
                    registerBtn.text = "Register"
                    Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}