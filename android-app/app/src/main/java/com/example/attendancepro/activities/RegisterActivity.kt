package com.example.attendancepro.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
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

class RegisterActivity : AppCompatActivity() {

    private lateinit var imageFile: File
    private lateinit var photoUri: Uri

    private lateinit var name: EditText
    private lateinit var roll: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var captureBtn: Button
    private lateinit var registerBtn: Button
    private lateinit var previewImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        name = findViewById(R.id.name)
        roll = findViewById(R.id.roll)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        captureBtn = findViewById(R.id.captureBtn)
        registerBtn = findViewById(R.id.registerBtn)
        previewImage = findViewById(R.id.previewImage)

        captureBtn.setOnClickListener {
            checkCameraPermission()
        }

        registerBtn.setOnClickListener {
            registerUser()
        }
    }

    // =========================
    // 📸 CAMERA PERMISSION
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

    // =========================
    // 📸 OPEN CAMERA
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

        cameraLauncher.launch(intent)
    }

    // =========================
    // 📸 CAMERA RESULT
    // =========================
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                previewImage.setImageURI(photoUri)
                Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show()
            }
        }

    // =========================
    // 🚀 REGISTER API
    // =========================
    private fun registerUser() {

        if (!::imageFile.isInitialized) {
            Toast.makeText(this, "Capture photo first", Toast.LENGTH_SHORT).show()
            return
        }

        val requestFile =
            imageFile.asRequestBody("image/*".toMediaTypeOrNull())

        val filePart =
            MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

        val nameBody = name.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val rollBody = roll.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val passBody = password.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        RetrofitClient.instance.register(nameBody, rollBody, emailBody, passBody, filePart)
            .enqueue(object : Callback<Map<String, String>> {

                override fun onResponse(
                    call: Call<Map<String, String>>,
                    response: Response<Map<String, String>>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterActivity, "Registered Successfully", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}