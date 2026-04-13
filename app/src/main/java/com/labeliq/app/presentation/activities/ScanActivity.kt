package com.labeliq.app.presentation.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.labeliq.app.data.local.ScanResult
import com.labeliq.app.data.local.loadScanHistory
import com.labeliq.app.data.local.UserProfile
import com.labeliq.app.data.local.getCurrentUser
import com.labeliq.app.data.local.saveScanResult
import com.labeliq.app.data.repository.IngredientRepository
import com.labeliq.app.databinding.ActivityScanBinding
import com.labeliq.app.domain.usecase.IngredientTextProcessor
import com.labeliq.app.domain.usecase.RiskEngine
import java.io.File

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var ingredientRepository: IngredientRepository
    private lateinit var riskEngine: RiskEngine
    private lateinit var ingredientTextProcessor: IngredientTextProcessor

    /** Holds the last captured file so Confirm can reference it. */
    private var lastCapturedFile: File? = null

    // ── Permission launcher ──────────────────────────────────────────────────
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else showPermissionDenied()
        }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ingredientRepository = IngredientRepository.getInstance(applicationContext)
        riskEngine = RiskEngine(ingredientRepository)
        ingredientTextProcessor = IngredientTextProcessor(ingredientRepository)

        if (hasCameraPermission()) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnCapture.setOnClickListener { captureImage() }
        binding.btnRetake.setOnClickListener { showPreview() }
        binding.btnConfirm.setOnClickListener {
            lastCapturedFile?.let { file ->
                binding.layoutLoading.visibility = View.VISIBLE
                runOcr(file)
            }
        }

        // ── Temporary: verify scan history persistence ───────────────────────
        val history = loadScanHistory(this)
        Log.d("HISTORY", "Loaded history size: ${history.size}")
        for (item in history) {
            Log.d("HISTORY_ITEM", item.toString())
        }
    }

    // ── Camera setup ─────────────────────────────────────────────────────────
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { p ->
                p.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )

        }, ContextCompat.getMainExecutor(this))
    }

    // ── Capture ──────────────────────────────────────────────────────────────
    private fun captureImage() {
        val capture = imageCapture ?: return

        val outputFile = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("SCAN", "Saved: ${outputFile.absolutePath}")
                    showCapturedImage(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("SCAN", "Capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    // ── UI state — review screen ─────────────────────────────────────────────
    private fun showCapturedImage(file: File) {
        lastCapturedFile = file

        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val bitmap = rotateBitmapToUpright(file, raw)
        binding.ivCaptured.setImageBitmap(bitmap)

        binding.previewView.visibility = View.GONE
        binding.btnCapture.visibility = View.GONE
        binding.ivCaptured.visibility = View.VISIBLE
        binding.layoutReviewActions.visibility = View.VISIBLE
    }

    // ── UI state — back to preview ───────────────────────────────────────────
    private fun showPreview() {
        lastCapturedFile = null
        binding.ivCaptured.setImageBitmap(null)

        binding.ivCaptured.visibility = View.GONE
        binding.layoutReviewActions.visibility = View.GONE
        binding.layoutLoading.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        binding.btnCapture.visibility = View.VISIBLE
    }

    // ── OCR (triggered by Confirm only) ─────────────────────────────────────
    private fun runOcr(file: File) {
        val image = InputImage.fromFilePath(this, Uri.fromFile(file))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text
                if (resultText.isEmpty()) {
                    Log.d("OCR", "No text detected")
                } else {
                    Log.d("OCR", "Detected: $resultText")
                }

                // ── Parse + clean ingredients ─────────────────────────────
                val extractedIngredients = ingredientTextProcessor.extractIngredients(resultText)
                val matchedIngredients = ingredientTextProcessor.processIngredients(resultText)
                val matchedIngredientList = matchedIngredients.map { it.name }
                Log.d("OCR_INGREDIENTS_EXTRACTED", extractedIngredients.toString())
                Log.d("OCR_INGREDIENTS_MATCHED", matchedIngredientList.toString())

                // ── Load current user and map to UserProfile ───────────────
                val user = getCurrentUser(this@ScanActivity)
                val profile = UserProfile(
                    name          = user?.name.orEmpty(),
                    conditions    = if (user?.isDiabetic == true) listOf("diabetes") else emptyList(),
                    allergies     = if (user?.hasNutAllergy == true) listOf("nuts") else emptyList(),
                    preferences   = if (user?.isVegan == true) listOf("vegan") else emptyList(),
                    avoidTags     = emptyList(),
                    dietGoal      = "balanced",
                    lifestyle     = "normal",
                    customNote    = ""
                )
                Log.d("PROFILE", "Using user: ${user?.name}, profile: $profile")

                // ── Evaluate risk using local knowledge base ──────────────
                val report = riskEngine.evaluate(extractedIngredients, profile)
                val concerns = ArrayList(report.concerns)
                val benefits = ArrayList(report.benefits)
                val neutral = ArrayList(report.neutral)
                val unknowns = ArrayList(report.unknowns)
                Log.d("CLASSIFY", "Concerns: $concerns")
                Log.d("CLASSIFY", "Benefits: $benefits")
                Log.d("CLASSIFY", "Neutral: $neutral")
                Log.d("CLASSIFY", "Unknowns: $unknowns")

                // ── Save scan result ─────────────────────────────────────────────
                val result = ScanResult(
                    status = report.verdict,
                    summary = report.advice,
                    highRisk = concerns,
                    moderate = ArrayList(neutral + unknowns),
                    safe = benefits,
                    score = report.score,
                    advice = report.advice,
                    timestamp = System.currentTimeMillis(),
                    imagePath = lastCapturedFile?.absolutePath ?: ""
                )
                saveScanResult(this@ScanActivity, result)
                Log.d("SAVE", "Scan saved")

                // ── Navigate to ResultActivity ───────────────────────────
                binding.layoutLoading.visibility = View.GONE
                val intent = Intent(this@ScanActivity, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_VERDICT, report.verdict)
                    putExtra(ResultActivity.EXTRA_ADVICE, report.advice)
                    putExtra(ResultActivity.EXTRA_SCORE, report.score)
                    putStringArrayListExtra(ResultActivity.EXTRA_CONCERNS, concerns)
                    putStringArrayListExtra(ResultActivity.EXTRA_BENEFITS, benefits)
                    putStringArrayListExtra(ResultActivity.EXTRA_NEUTRAL, neutral)
                    putStringArrayListExtra(ResultActivity.EXTRA_UNKNOWNS, unknowns)
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Failed", e)
                binding.layoutLoading.visibility = View.GONE
            }
    }

    // ── EXIF rotation ─────────────────────────────────────────────────────────
    private fun rotateBitmapToUpright(file: File, bitmap: Bitmap): Bitmap {
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else                                 -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun showPermissionDenied() {
        binding.previewView.visibility = View.GONE
        binding.tvPermissionDenied.visibility = View.VISIBLE
    }
}
