package com.labeliq.app.presentation.activities

import android.os.Bundle
import android.graphics.BitmapFactory
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.databinding.ActivityResultDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultDetailBinding

    companion object {
        const val EXTRA_STATUS     = "detail_status"
        const val EXTRA_TIMESTAMP  = "detail_timestamp"
        const val EXTRA_HIGH_RISK  = "detail_high_risk"
        const val EXTRA_MODERATE   = "detail_moderate"
        const val EXTRA_SAFE       = "detail_safe"
        const val EXTRA_IMAGE_PATH = "EXTRA_IMAGE_PATH"
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Toolbar back navigation ──────────────────────────────────
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Retrieve Intent extras ───────────────────────────────────
        val status    = intent.getStringExtra(EXTRA_STATUS)    ?: "SAFE"
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        val highRisk  = intent.getStringArrayListExtra(EXTRA_HIGH_RISK)  ?: arrayListOf()
        val moderate  = intent.getStringArrayListExtra(EXTRA_MODERATE)   ?: arrayListOf()
        val safeList  = intent.getStringArrayListExtra(EXTRA_SAFE)       ?: arrayListOf()
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH) ?: ""

        // ── Show scanned image if path is available ────────────────
        if (imagePath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            binding.ivScanImage.setImageBitmap(bitmap)
        }

        // ── Header card: status icon + label + colour ────────────────
        when (status) {
            "HIGH_RISK" -> {
                binding.cardDetailHeader.setCardBackgroundColor(0xFFB71C1C.toInt())
                binding.tvDetailIcon.text   = "❌"
                binding.tvDetailStatus.text = "High Risk"
                binding.tvDetailStatus.setTextColor(0xFFFFCDD2.toInt())
            }
            "MODERATE" -> {
                binding.cardDetailHeader.setCardBackgroundColor(0xFFE65100.toInt())
                binding.tvDetailIcon.text   = "⚠️"
                binding.tvDetailStatus.text = "Moderate Risk"
                binding.tvDetailStatus.setTextColor(0xFFFFE0B2.toInt())
            }
            else -> {
                binding.cardDetailHeader.setCardBackgroundColor(0xFF1B5E20.toInt())
                binding.tvDetailIcon.text   = "✅"
                binding.tvDetailStatus.text = "Safe"
                binding.tvDetailStatus.setTextColor(0xFFA5D6A7.toInt())
            }
        }

        // ── Timestamp ────────────────────────────────────────────────
        binding.tvDetailTimestamp.text = "🕒  ${dateFormat.format(Date(timestamp))}"

        // ── High Risk block ──────────────────────────────────────────
        if (highRisk.isNotEmpty()) {
            binding.tvDetailHighItems.text = highRisk.joinToString("\n") { "  • $it" }
            binding.cardDetailHigh.visibility = View.VISIBLE
        }

        // ── Moderate block ───────────────────────────────────────────
        if (moderate.isNotEmpty()) {
            binding.tvDetailModerateItems.text = moderate.joinToString("\n") { "  • $it" }
            binding.cardDetailModerate.visibility = View.VISIBLE
        }

        // ── Safe block ───────────────────────────────────────────────
        if (safeList.isNotEmpty()) {
            binding.tvDetailSafeItems.text = safeList.joinToString("\n") { "  • $it" }
            binding.cardDetailSafe.visibility = View.VISIBLE
        }

        // ── No data fallback ─────────────────────────────────────────
        if (highRisk.isEmpty() && moderate.isEmpty() && safeList.isEmpty()) {
            binding.tvDetailNoData.visibility = View.VISIBLE
        }
    }
}
