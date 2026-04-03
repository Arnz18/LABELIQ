package com.labeliq.app.presentation.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
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
import com.labeliq.app.databinding.ActivityScanBinding
import java.io.File

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private var imageCapture: ImageCapture? = null

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

        if (hasCameraPermission()) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnCapture.setOnClickListener { captureImage() }
        binding.btnRetake.setOnClickListener { showPreview() }
        binding.btnConfirm.setOnClickListener {
            lastCapturedFile?.let { file ->
                binding.cardResult.visibility = View.GONE
                binding.layoutLoading.visibility = View.VISIBLE
                runOcr(file)
            }
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
        binding.cardResult.visibility = View.GONE
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

                // ── Parse ingredients ────────────────────────────────────
                val parsedIngredients = parseIngredients(resultText)
                Log.d("PARSED", parsedIngredients.toString())

                // ── Harmful ingredient analysis ──────────────────────────
                val text = resultText.lowercase()

                val harmfulIngredients = listOf(
                    "sodium benzoate",
                    "msg",
                    "aspartame",
                    "high fructose corn syrup",
                    "artificial color",
                    "preservative"
                )

                val detected = mutableListOf<String>()
                for (ingredient in harmfulIngredients) {
                    if (text.contains(ingredient)) {
                        Log.d("ANALYSIS", "Harmful ingredient found: $ingredient")
                        detected.add(ingredient)
                    }
                }

                // ── Update result card ───────────────────────────────────
                if (detected.isNotEmpty()) {
                    binding.cardResult.setCardBackgroundColor(0xFFB71C1C.toInt()) // deep red
                    binding.tvResultIcon.text = "⚠️"
                    binding.tvResultTitle.text = "Harmful Ingredients Found"
                    binding.tvResultTitle.setTextColor(0xFFFFCDD2.toInt())
                    binding.tvResultDescription.text = detected.joinToString("\n")
                    binding.tvResultDescription.setTextColor(0xFFEF9A9A.toInt())
                } else {
                    Log.d("ANALYSIS", "No harmful ingredients detected")
                    binding.cardResult.setCardBackgroundColor(0xFF1B5E20.toInt()) // dark emerald
                    binding.tvResultIcon.text = "✅"
                    binding.tvResultTitle.text = "Safe"
                    binding.tvResultTitle.setTextColor(0xFFA5D6A7.toInt())
                    binding.tvResultDescription.text = "No harmful ingredients detected"
                    binding.tvResultDescription.setTextColor(0xFFC8E6C9.toInt())
                }

                // ── Show card with scale + fade animation ────────────────
                binding.layoutLoading.visibility = View.GONE
                binding.cardResult.alpha = 0f
                binding.cardResult.scaleX = 0.9f
                binding.cardResult.scaleY = 0.9f
                binding.cardResult.visibility = View.VISIBLE
                binding.cardResult.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Failed", e)
                binding.layoutLoading.visibility = View.GONE
            }
    }

    // ── Ingredient parser ────────────────────────────────────────────────────
    private fun parseIngredients(resultText: String): List<String> {
        val cleanText = resultText.lowercase()
            .replace("(", "")
            .replace(")", "")
            .replace("%", "")
            .replace("[", "")
            .replace("]", "")
            .replace(Regex("\\s+"), " ")

        return cleanText.split(",", "\n", ".")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
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
