package com.example.attendancepro.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.attendancepro.R
import com.example.attendancepro.utils.LivenessOverlayView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LivenessDetectionActivity : AppCompatActivity() {

    enum class LivenessStep {
        ALIGN, MOVE, CAPTURE, DONE
    }

    private lateinit var viewFinder: PreviewView
    private lateinit var livenessOverlay: LivenessOverlayView
    private lateinit var btnClose: ImageButton
    private lateinit var btnHelp: ImageButton
    private lateinit var btnCancel: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    
    private lateinit var layoutMoveLeft: LinearLayout
    private lateinit var layoutMoveRight: LinearLayout
    private lateinit var cardInstruction: View
    private lateinit var ivSmiley: ImageView
    private lateinit var tvInstructionMain: TextView
    private lateinit var tvInstructionSub: TextView

    // Stepper views
    private lateinit var step1Circle: View
    private lateinit var step1Check: ImageView
    private lateinit var step1Text: TextView
    private lateinit var tvStep1Label: TextView
    private lateinit var line1_2: View

    private lateinit var step2Circle: View
    private lateinit var step2Check: ImageView
    private lateinit var step2Text: TextView
    private lateinit var tvStep2Label: TextView
    private lateinit var line2_3: View

    private lateinit var step3Circle: View
    private lateinit var step3Check: ImageView
    private lateinit var step3Text: TextView
    private lateinit var tvStep3Label: TextView
    private lateinit var line3_4: View

    private lateinit var step4Circle: View
    private lateinit var step4Check: ImageView
    private lateinit var step4Text: TextView
    private lateinit var tvStep4Label: TextView

    // CameraX and ML Kit
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector
    private var imageCapture: ImageCapture? = null

    // State parameters
    private var currentStep = LivenessStep.ALIGN
    private val moveSequence = ArrayList<LivenessOverlayView.LivenessDirection>()
    private var currentMoveIndex = 0
    private var alignStartTime = 0L
    private var isCapturing = false
    private var eyesClosedDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide status bar and navigation bar for full screen camera feel
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT

        setContentView(R.layout.activity_liveness)

        // Choose move sequence of directions randomly for security, including blink
        val actions = mutableListOf(
            LivenessOverlayView.LivenessDirection.LEFT,
            LivenessOverlayView.LivenessDirection.RIGHT,
            LivenessOverlayView.LivenessDirection.BLINK
        )
        actions.shuffle()
        moveSequence.addAll(actions)

        // Init UI components
        viewFinder = findViewById(R.id.viewFinder)
        livenessOverlay = findViewById(R.id.livenessOverlay)
        btnClose = findViewById(R.id.btnClose)
        btnHelp = findViewById(R.id.btnHelp)
        btnCancel = findViewById(R.id.btnCancel)
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)

        layoutMoveLeft = findViewById(R.id.layoutMoveLeft)
        layoutMoveRight = findViewById(R.id.layoutMoveRight)
        cardInstruction = findViewById(R.id.cardInstruction)
        ivSmiley = findViewById(R.id.ivSmiley)
        tvInstructionMain = findViewById(R.id.tvInstructionMain)
        tvInstructionSub = findViewById(R.id.tvInstructionSub)

        // Stepper
        step1Circle = findViewById(R.id.step1Circle)
        step1Check = findViewById(R.id.step1Check)
        step1Text = findViewById(R.id.step1Text)
        tvStep1Label = findViewById(R.id.tvStep1Label)
        line1_2 = findViewById(R.id.line1_2)

        step2Circle = findViewById(R.id.step2Circle)
        step2Check = findViewById(R.id.step2Check)
        step2Text = findViewById(R.id.step2Text)
        tvStep2Label = findViewById(R.id.tvStep2Label)
        line2_3 = findViewById(R.id.line2_3)

        step3Circle = findViewById(R.id.step3Circle)
        step3Check = findViewById(R.id.step3Check)
        step3Text = findViewById(R.id.step3Text)
        tvStep3Label = findViewById(R.id.tvStep3Label)
        line3_4 = findViewById(R.id.line3_4)

        step4Circle = findViewById(R.id.step4Circle)
        step4Check = findViewById(R.id.step4Check)
        step4Text = findViewById(R.id.step4Text)
        tvStep4Label = findViewById(R.id.tvStep4Label)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Init face detector options (performance optimized)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setMinFaceSize(0.25f)
            .build()
        faceDetector = FaceDetection.getClient(options)

        // Setup click listeners
        btnClose.setOnClickListener { cancelAndFinish() }
        btnCancel.setOnClickListener { cancelAndFinish() }
        btnHelp.setOnClickListener { showHelpDialog() }

        // Start workflow
        updateStepper(LivenessStep.ALIGN)
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required for liveness detection", Toast.LENGTH_LONG).show()
                cancelAndFinish()
            }
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // ImageAnalysis
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer())
                }

            // Select front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to initialize camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }

            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (isCapturing) return@addOnSuccessListener

                    if (faces.isEmpty()) {
                        if (currentStep == LivenessStep.ALIGN) {
                            alignStartTime = 0L
                            runOnUiThread {
                                tvInstructionMain.text = "Align your face in the center"
                                tvInstructionSub.text = "No face detected"
                                tvInstructionSub.setTextColor(Color.parseColor("#FF5252"))
                            }
                        }
                        return@addOnSuccessListener
                    }

                    if (faces.size > 1) {
                        if (currentStep == LivenessStep.ALIGN) {
                            alignStartTime = 0L
                            runOnUiThread {
                                tvInstructionMain.text = "Multiple faces detected"
                                tvInstructionSub.text = "Ensure only one face is visible"
                                tvInstructionSub.setTextColor(Color.parseColor("#FF5252"))
                            }
                        }
                        return@addOnSuccessListener
                    }

                    val face = faces[0]
                    val yaw = face.headEulerAngleY
                    val pitch = face.headEulerAngleX

                    when (currentStep) {
                        LivenessStep.ALIGN -> {
                            val isStraight = Math.abs(yaw) < 8f && Math.abs(pitch) < 10f
                            if (isStraight) {
                                if (alignStartTime == 0L) {
                                    alignStartTime = System.currentTimeMillis()
                                } else if (System.currentTimeMillis() - alignStartTime > 1200) {
                                    currentStep = LivenessStep.MOVE
                                    runOnUiThread { transitionToMoveStep() }
                                } else {
                                    runOnUiThread {
                                        tvInstructionMain.text = "Align your face in the center"
                                        tvInstructionSub.text = "Hold still..."
                                        tvInstructionSub.setTextColor(Color.parseColor("#B0FFFFFF"))
                                    }
                                }
                            } else {
                                alignStartTime = 0L
                                runOnUiThread {
                                    tvInstructionMain.text = "Align your face in the center"
                                    tvInstructionSub.text = "Look straight at the screen"
                                    tvInstructionSub.setTextColor(Color.parseColor("#FFC107"))
                                }
                            }
                        }

                        LivenessStep.MOVE -> {
                            val target = moveSequence[currentMoveIndex]
                            var completed = false
                            if (target == LivenessOverlayView.LivenessDirection.BLINK) {
                                val leftEye = face.leftEyeOpenProbability
                                val rightEye = face.rightEyeOpenProbability
                                if (leftEye != null && rightEye != null) {
                                    if (leftEye < 0.2f && rightEye < 0.2f) {
                                        eyesClosedDetected = true
                                    } else if (eyesClosedDetected && leftEye > 0.8f && rightEye > 0.8f) {
                                        completed = true
                                    }
                                }
                            } else if (target == LivenessOverlayView.LivenessDirection.RIGHT) {
                                // Mirrored camera: Screen right = Physical Left (yaw < -18f)
                                if (yaw < -18f) {
                                    completed = true
                                }
                            } else {
                                // Mirrored camera: Screen left = Physical Right (yaw > 18f)
                                if (yaw > 18f) {
                                    completed = true
                                }
                            }

                            if (completed) {
                                if (currentMoveIndex < moveSequence.size - 1) {
                                    currentMoveIndex++
                                    runOnUiThread { runNextMovement() }
                                } else {
                                    currentStep = LivenessStep.CAPTURE
                                    runOnUiThread { transitionToCaptureStep() }
                                }
                            }
                        }

                        LivenessStep.CAPTURE -> {
                            val isStraight = Math.abs(yaw) < 8f && Math.abs(pitch) < 10f
                            if (isStraight) {
                                isCapturing = true
                                currentStep = LivenessStep.DONE
                                runOnUiThread { autoCapturePhoto() }
                            }
                        }

                        LivenessStep.DONE -> {
                            // Waiting to finish
                        }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun transitionToMoveStep() {
        updateStepper(LivenessStep.MOVE)
        currentMoveIndex = 0
        runNextMovement()
    }

    private fun runNextMovement() {
        val direction = moveSequence[currentMoveIndex]
        livenessOverlay.activeDirection = direction

        if (direction == LivenessOverlayView.LivenessDirection.BLINK) {
            tvTitle.text = "Perform Action"
            tvSubtitle.text = "Follow the instruction (${currentMoveIndex + 1}/${moveSequence.size})"
            layoutMoveLeft.visibility = View.GONE
            layoutMoveRight.visibility = View.GONE
            tvInstructionMain.text = "Great! Now blink your eyes"
            tvInstructionSub.text = "Blink once"
            tvInstructionSub.setTextColor(Color.parseColor("#66BB6A"))
            eyesClosedDetected = false // Reset for this step
        } else {
            tvTitle.text = "Perform Movement"
            tvSubtitle.text = "Turn your head in the indicated direction (${currentMoveIndex + 1}/${moveSequence.size})"
            if (direction == LivenessOverlayView.LivenessDirection.RIGHT) {
                layoutMoveRight.visibility = View.VISIBLE
                layoutMoveLeft.visibility = View.GONE
                tvInstructionMain.text = "Great! Now move your face"
                tvInstructionSub.text = "slightly right"
                tvInstructionSub.setTextColor(Color.parseColor("#66BB6A"))
            } else {
                layoutMoveLeft.visibility = View.VISIBLE
                layoutMoveRight.visibility = View.GONE
                tvInstructionMain.text = "Great! Now move your face"
                tvInstructionSub.text = "slightly left"
                tvInstructionSub.setTextColor(Color.parseColor("#66BB6A"))
            }
        }
    }

    private fun transitionToCaptureStep() {
        updateStepper(LivenessStep.CAPTURE)
        livenessOverlay.activeDirection = LivenessOverlayView.LivenessDirection.NONE

        layoutMoveLeft.visibility = View.GONE
        layoutMoveRight.visibility = View.GONE

        tvTitle.text = "Look Straight"
        tvSubtitle.text = "Hold still for the camera"
        tvInstructionMain.text = "Perfect! Now look straight"
        tvInstructionSub.text = "Capturing photo..."
        tvInstructionSub.setTextColor(Color.parseColor("#66BB6A"))
    }

    private fun autoCapturePhoto() {
        val capture = imageCapture ?: return
        val photoFile = File(cacheDir, "liveness_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    currentStep = LivenessStep.DONE
                    updateStepper(LivenessStep.DONE)
                    tvInstructionMain.text = "Success!"
                    tvInstructionSub.text = "Liveness verified successfully"
                    tvInstructionSub.setTextColor(Color.parseColor("#66BB6A"))

                    // Crop the face region to match the liveness oval exactly
                    cropFaceRegion(photoFile)

                    Handler(Looper.getMainLooper()).postDelayed({
                        val resultIntent = Intent().apply {
                            putExtra("photo_path", photoFile.absolutePath)
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }, 1000)
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    Toast.makeText(this@LivenessDetectionActivity, "Capture failed, trying again", Toast.LENGTH_SHORT).show()
                    isCapturing = false
                    currentStep = LivenessStep.CAPTURE
                }
            }
        )
    }

    private fun cropFaceRegion(photoFile: File) {
        try {
            // 1. Decode original bitmap
            val originalBitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return

            // 2. Fix rotation using EXIF data
            val exif = androidx.exifinterface.media.ExifInterface(photoFile.absolutePath)
            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            var rotate = 0f
            var mirror = false
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270f
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> mirror = true
            }
            
            if (rotate != 0f) {
                matrix.postRotate(rotate)
            }
            if (mirror) {
                matrix.postScale(-1f, 1f)
            }

            val rotatedBitmap = if (rotate != 0f || mirror) {
                Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
            } else {
                originalBitmap
            }

            // 3. Map overlay oval coordinates to the rotated bitmap
            val screenWidth = viewFinder.width
            val screenHeight = viewFinder.height
            val ovalRect = livenessOverlay.getOvalRect()

            // scaleType fillCenter logic:
            val scale = Math.max(screenWidth.toFloat() / rotatedBitmap.width, screenHeight.toFloat() / rotatedBitmap.height)
            val dx = (rotatedBitmap.width * scale - screenWidth) / 2f
            val dy = (rotatedBitmap.height * scale - screenHeight) / 2f

            // Map oval rect coordinates to camera bitmap coordinates
            val width = ovalRect.width() / scale
            val height = ovalRect.height() / scale
            val centerX = (ovalRect.left + ovalRect.right) / 2f + dx
            val centerY = (ovalRect.top + ovalRect.bottom) / 2f + dy

            // Expand crop box by 25% (1.25x scale factor) outwards from center
            val expandedWidth = width * 1.25f
            val expandedHeight = height * 1.25f

            // Recalculate top-left corner (shifting the crop window upwards slightly to capture more hair/head space)
            val cropLeft = ((centerX / scale) - (expandedWidth / 2f)).toInt().coerceIn(0, rotatedBitmap.width - 1)
            val cropTop = ((centerY / scale) - (expandedHeight * 0.58f)).toInt().coerceIn(0, rotatedBitmap.height - 1)

            // Adjust width and height to fit rotatedBitmap boundary (capturing symmetric space at the bottom)
            val cropWidth = expandedWidth.toInt().coerceIn(1, rotatedBitmap.width - cropLeft)
            val cropHeight = (expandedHeight * 1.16f).toInt().coerceIn(1, rotatedBitmap.height - cropTop)

            // 4. Crop the bitmap to the expanded oval bounds
            val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropLeft, cropTop, cropWidth, cropHeight)

            // 5. Overwrite the file with the cropped bitmap
            FileOutputStream(photoFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.flush()
            }

            // Recycle bitmaps to free memory
            if (originalBitmap != rotatedBitmap) originalBitmap.recycle()
            rotatedBitmap.recycle()
            croppedBitmap.recycle()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateStepper(step: LivenessStep) {
        val greenColor = Color.parseColor("#66BB6A")
        val darkGray = Color.parseColor("#2C2C2E")
        val lightGray = Color.parseColor("#8E8E93")
        val white = Color.parseColor("#FFFFFF")

        // Step 1
        step1Circle.backgroundTintList = ColorStateList.valueOf(greenColor)
        step1Check.visibility = View.VISIBLE
        step1Text.visibility = View.GONE
        tvStep1Label.setTextColor(greenColor)

        // Step 2
        step2Circle.backgroundTintList = ColorStateList.valueOf(if (step >= LivenessStep.MOVE) greenColor else darkGray)
        step2Check.visibility = if (step > LivenessStep.MOVE) View.VISIBLE else View.GONE
        step2Text.visibility = if (step > LivenessStep.MOVE) View.GONE else View.VISIBLE
        step2Text.setTextColor(if (step >= LivenessStep.MOVE) white else lightGray)
        tvStep2Label.setTextColor(if (step >= LivenessStep.MOVE) greenColor else lightGray)
        line1_2.setBackgroundColor(if (step >= LivenessStep.MOVE) greenColor else Color.parseColor("#30FFFFFF"))

        // Step 3
        step3Circle.backgroundTintList = ColorStateList.valueOf(if (step >= LivenessStep.CAPTURE) greenColor else darkGray)
        step3Check.visibility = if (step > LivenessStep.CAPTURE) View.VISIBLE else View.GONE
        step3Text.visibility = if (step > LivenessStep.CAPTURE) View.GONE else View.VISIBLE
        step3Text.setTextColor(if (step >= LivenessStep.CAPTURE) white else lightGray)
        tvStep3Label.setTextColor(if (step >= LivenessStep.CAPTURE) greenColor else lightGray)
        line2_3.setBackgroundColor(if (step >= LivenessStep.CAPTURE) greenColor else Color.parseColor("#30FFFFFF"))

        // Step 4
        step4Circle.backgroundTintList = ColorStateList.valueOf(if (step == LivenessStep.DONE) greenColor else darkGray)
        step4Check.visibility = if (step == LivenessStep.DONE) View.VISIBLE else View.GONE
        step4Text.visibility = if (step == LivenessStep.DONE) View.GONE else View.VISIBLE
        step4Text.setTextColor(if (step == LivenessStep.DONE) white else lightGray)
        tvStep4Label.setTextColor(if (step == LivenessStep.DONE) greenColor else lightGray)
        line3_4.setBackgroundColor(if (step == LivenessStep.DONE) greenColor else Color.parseColor("#30FFFFFF"))
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Liveness Verification")
            .setMessage("This verification helps prevent unauthorized attendance logging. Please align your face in the oval, turn your head slightly left or right when prompted, and look straight again for automatic photo capture.")
            .setPositiveButton("Got It", null)
            .show()
    }

    private fun cancelAndFinish() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector.close()
    }
}
