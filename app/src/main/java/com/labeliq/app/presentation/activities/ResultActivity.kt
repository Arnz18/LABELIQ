package com.labeliq.app.presentation.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.loadUserProfile
import com.labeliq.app.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    companion object {
        const val EXTRA_STATUS       = "extra_status"
        const val EXTRA_SUMMARY      = "extra_summary"
        const val EXTRA_HIGH_RISK    = "extra_high_risk"
        const val EXTRA_MODERATE     = "extra_moderate"
        const val EXTRA_SAFE         = "extra_safe"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Toolbar back navigation ──────────────────────────────────
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Load user profile (backend test) ────────────────────────
        val profile = loadUserProfile(this)
        Log.d("PROFILE", profile.toString())

        // ── Receive data ─────────────────────────────────────────────
        val status    = intent.getStringExtra(EXTRA_STATUS)    ?: "✅ Safe"
        val summary   = intent.getStringExtra(EXTRA_SUMMARY)   ?: ""
        val highRisk  = intent.getStringArrayListExtra(EXTRA_HIGH_RISK)  ?: arrayListOf()
        val moderate  = intent.getStringArrayListExtra(EXTRA_MODERATE)   ?: arrayListOf()
        val safeList  = intent.getStringArrayListExtra(EXTRA_SAFE)       ?: arrayListOf()

        // ── Populate summary card ────────────────────────────────────
        when (status) {
            "❌ High Risk" -> {
                binding.cardResult.setCardBackgroundColor(0xFFB71C1C.toInt())
                binding.tvResultIcon.text = "❌"
                binding.tvResultTitle.setTextColor(0xFFFFCDD2.toInt())
            }
            "⚠️ Moderate Risk" -> {
                binding.cardResult.setCardBackgroundColor(0xFFE65100.toInt())
                binding.tvResultIcon.text = "⚠️"
                binding.tvResultTitle.setTextColor(0xFFFFE0B2.toInt())
            }
            else -> {
                binding.cardResult.setCardBackgroundColor(0xFF1B5E20.toInt())
                binding.tvResultIcon.text = "✅"
                binding.tvResultTitle.setTextColor(0xFFA5D6A7.toInt())
            }
        }
        binding.tvResultTitle.text = status
        binding.tvResultDescription.text = summary

        // ── High Risk block ──────────────────────────────────────────
        if (highRisk.isNotEmpty()) {
            binding.tvHighRiskItems.text = highRisk.joinToString("\n") { "  • $it" }
            binding.cardHighRisk.visibility = View.VISIBLE
        }

        // ── Moderate block ───────────────────────────────────────────
        if (moderate.isNotEmpty()) {
            binding.tvModerateItems.text = moderate.joinToString("\n") { "  • $it" }
            binding.cardModerate.visibility = View.VISIBLE
        }

        // ── Safe block ───────────────────────────────────────────────
        if (safeList.isNotEmpty()) {
            binding.tvSafeItems.text = safeList.joinToString("\n") { "  • $it" }
            binding.cardSafe.visibility = View.VISIBLE
        }

        // ── Scan Again ───────────────────────────────────────────────
        binding.btnScanAgain.setOnClickListener { finish() }
    }
}
