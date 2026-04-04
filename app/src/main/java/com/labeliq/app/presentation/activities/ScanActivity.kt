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
import com.labeliq.app.data.local.loadUserProfile
import com.labeliq.app.data.local.saveScanResult
import com.labeliq.app.data.local.loadScanHistory
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

                // ── Parse + filter ingredients ─────────────────────────────
                val parsedIngredients = parseIngredients(resultText)
                    .filter { isValidIngredient(it) }
                Log.d("CLEANED", parsedIngredients.toString())

                // ── Load user profile ─────────────────────────────────────
                val profile = loadUserProfile(this@ScanActivity)
                Log.d("PROFILE", "Using profile: $profile")

                // ── Classify ingredients by risk ──────────────────────────
                val (highRisk, moderateRisk, safeList) = classifyIngredients(parsedIngredients, profile)
                Log.d("CLASSIFY", "High: $highRisk")
                Log.d("CLASSIFY", "Moderate: $moderateRisk")
                Log.d("CLASSIFY", "Safe: $safeList")

                // ── Weighted risk score ──────────────────────────────────
                val total = highRisk.size + moderateRisk.size + safeList.size
                val status = if (total == 0) {
                    "✅ Safe"
                } else {
                    val score = (highRisk.size * 3 +
                                 moderateRisk.size * 2 +
                                 safeList.size * 1).toDouble() / total
                    when {
                        highRisk.isNotEmpty() && score >= 2.3 -> "❌ High Risk"
                        highRisk.isNotEmpty()                 -> "⚠️ Moderate Risk"
                        moderateRisk.isNotEmpty()             -> "⚠️ Moderate Risk"
                        else                                  -> "✅ Safe"
                    }
                }

                // ── Summary sentence ─────────────────────────────────────
                val summary = when (status) {
                    "❌ High Risk"     -> "This product contains harmful additives."
                    "⚠️ Moderate Risk" -> "This product contains some processed or risky ingredients."
                    else               -> "This product is generally safe to consume."
                }

                // ── Clean + normalize ingredient lists ──────────────────────
                val cleanHigh     = ArrayList(highRisk.map     { normalizeIngredient(it) }.distinct())
                val cleanModerate = ArrayList(moderateRisk.map { normalizeIngredient(it) }.distinct())
                val cleanSafe     = ArrayList(safeList.map     { normalizeIngredient(it) }.distinct())

                // ── Save scan result ─────────────────────────────────────────────
                val result = ScanResult(
                    status = if (highRisk.isNotEmpty()) "HIGH_RISK" else "SAFE",
                    summary = "Scan completed",
                    highRisk = highRisk,
                    moderate = moderateRisk,
                    safe = safeList,
                    timestamp = System.currentTimeMillis()
                )
                saveScanResult(this@ScanActivity, result)
                Log.d("SAVE", "Scan saved")

                // ── Navigate to ResultActivity ───────────────────────────
                binding.layoutLoading.visibility = View.GONE
                val intent = Intent(this@ScanActivity, ResultActivity::class.java).apply {
                    putExtra(ResultActivity.EXTRA_STATUS,    status)
                    putExtra(ResultActivity.EXTRA_SUMMARY,   summary)
                    putStringArrayListExtra(ResultActivity.EXTRA_HIGH_RISK, cleanHigh)
                    putStringArrayListExtra(ResultActivity.EXTRA_MODERATE,  cleanModerate)
                    putStringArrayListExtra(ResultActivity.EXTRA_SAFE,      cleanSafe)
                }
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Failed", e)
                binding.layoutLoading.visibility = View.GONE
            }
    }

    // ── Ingredient classifier ───────────────────────────────────────
    private fun classifyIngredients(
        ingredients: List<String>,
        profile: com.labeliq.app.data.local.UserProfile
    ): Triple<List<String>, List<String>, List<String>> {

        // Base high-risk keywords (always applied)
        val baseHighRisk = mutableListOf(
            "artificial", "preservative", "aspartame",
            "sodium benzoate", "msg", "color", "colour"
        )

        // Profile-driven extensions
        if (profile.isDiabetic)    baseHighRisk += listOf("sugar")
        if (profile.hasNutAllergy) baseHighRisk += listOf("nuts", "peanut", "almond", "cashew")
        if (profile.isVegan)       baseHighRisk += listOf("milk", "egg", "gelatin", "dairy")

        val highRiskKeywords = baseHighRisk.toList()

        val moderateRiskKeywords = listOf(
            "sugar", "maltodextrin", "refined", "syrup"
        )
        val safeKeywords = listOf(
            "oats", "whole", "natural"
        )

        val high     = mutableListOf<String>()
        val moderate = mutableListOf<String>()
        val safe     = mutableListOf<String>()

        for (item in ingredients) {
            when {
                highRiskKeywords.any     { item.contains(it) } -> high.add(item)
                moderateRiskKeywords.any { item.contains(it) } -> moderate.add(item)
                safeKeywords.any         { item.contains(it) } -> safe.add(item)
            }
        }

        return Triple(high, moderate, safe)
    }

    // ── Ingredient text cleaner ────────────────────────────────────────────────
    /**
     * Strips common OCR filler words ("contains", "permitted", "added", etc.) from
     * the front of the string, then returns at most 3 words of whatever remains.
     * Example: "contains permitted natural food colour 110" → "food colour 110"
     */
    private fun cleanIngredient(item: String): String {
        val fillerPrefixes = setOf(
            "contains", "permitted", "added",
            "and", "or", "with", "as", "a", "an", "the", "no", "not",
            "less", "than", "of", "from", "for", "in", "processed"
        )

        val words = item.trim().split(" ").filter { it.isNotEmpty() }

        // Drop leading filler words to surface the meaningful keyword
        val meaningful = words.dropWhile { it.lowercase() in fillerPrefixes }
            .ifEmpty { words }   // fallback: keep original if everything was filler

        // Cap at 3 words so the card never overflows
        return if (meaningful.size > 3) meaningful.take(3).joinToString(" ") + "…"
        else meaningful.joinToString(" ")
    }

    // ── Ingredient name normalizer ────────────────────────────────────────────
    /**
     * 1. Checks a keyword map — if the item contains a known keyword,
     *    returns the human-readable label directly.
     * 2. Falls back to cleanIngredient() (strips fillers + caps at 4 words)
     *    so even unmapped items look tidy.
     */
    private fun normalizeIngredient(item: String): String {
        // Step 1: strip digits to avoid "colour 110", "e102" noise
        val cleaned = item.replace(Regex("\\d+"), "").trim()

        val replacements = mapOf(
            "colour"       to "artificial colour",
            "color"        to "artificial colour",
            "flavour"      to "added flavour",
            "flavor"       to "added flavour",
            "artificial"   to "artificial additive",
            "preservative" to "preservative",
            "maltodextrin" to "maltodextrin",
            "sugar"        to "sugar",
            "syrup"        to "syrup"
        )

        // Step 2: keyword map lookup on digit-stripped text
        val lower = cleaned.lowercase()
        for ((keyword, label) in replacements) {
            if (lower.contains(keyword)) return label
        }

        // Step 3: fallback — strip fillers, remove digits, cap at 3 words
        val fallback = cleanIngredient(cleaned)
        val words = fallback.split(" ").filter { it.isNotEmpty() && it != "…" }
        return if (words.size > 3) words.take(3).joinToString(" ") + "…"
        else fallback
    }

    // ── Noise filter ─────────────────────────────────────────────────
    /**
     * Returns true only if the item looks like an actual ingredient.
     * Rejects nutrition-facts lines, URLs, digit-only tokens, and
     * anything that is clearly a sentence rather than a keyword.
     */
    private fun isValidIngredient(item: String): Boolean {
        val invalidKeywords = listOf(
            "per", "serving", "value", "energy", "api",
            "system", "number", "code", "www", "http"
        )

        // Too short to be meaningful
        if (item.length < 3) return false

        // Pure digits (e.g. "110", "200")
        if (item.all { it.isDigit() || it.isWhitespace() }) return false

        // Contains a URL or path separator
        if (item.contains("/") || item.contains("http")) return false

        // Too many words — it's a sentence, not an ingredient
        if (item.split(" ").size > 6) return false

        // Contains a known noise keyword
        val lower = item.lowercase()
        if (invalidKeywords.any { lower.contains(it) }) return false

        return true
    }

    // ── Ingredient parser ─────────────────────────────────────────────────
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
