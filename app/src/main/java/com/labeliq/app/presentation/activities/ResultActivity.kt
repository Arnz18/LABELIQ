package com.labeliq.app.presentation.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.labeliq.app.data.remote.gemini.GeminiService
import com.labeliq.app.databinding.ActivityResultBinding
import com.labeliq.app.domain.usecase.SentenceGenerator
import com.labeliq.app.utils.isNetworkAvailable
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private val geminiService = GeminiService()

    companion object {
        private const val MAX_VISIBLE_ITEMS = 3
        private const val API_KEY = "AIzaSyDGpjPBRSNRgZh_gw6L89qjqwlz6PDiWlc"
        const val EXTRA_VERDICT = "extra_verdict"
        const val EXTRA_ADVICE = "extra_advice"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_CONCERNS = "extra_concerns"
        const val EXTRA_BENEFITS = "extra_benefits"
        const val EXTRA_NEUTRAL = "extra_neutral"
        const val EXTRA_UNKNOWNS = "extra_unknowns"
    }

    private data class ResultReport(
        val verdict: String,
        val score: Int,
        val concerns: List<String>,
        val benefits: List<String>,
        val neutral: List<String>,
        val limitedInsight: List<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Toolbar back navigation ──────────────────────────────────
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Receive data ─────────────────────────────────────────────
        val report = ResultReport(
            verdict = intent.getStringExtra(EXTRA_VERDICT) ?: "MODERATE",
            score = intent.getIntExtra(EXTRA_SCORE, 0),
            concerns = intent.getStringArrayListExtra(EXTRA_CONCERNS) ?: arrayListOf(),
            benefits = intent.getStringArrayListExtra(EXTRA_BENEFITS) ?: arrayListOf(),
            neutral = intent.getStringArrayListExtra(EXTRA_NEUTRAL) ?: arrayListOf(),
            limitedInsight = intent.getStringArrayListExtra(EXTRA_UNKNOWNS) ?: arrayListOf()
        )

        // ── Populate summary card ────────────────────────────────────
        when (report.verdict) {
            "HIGH RISK" -> {
                binding.cardResult.setCardBackgroundColor(0xFFB71C1C.toInt())
                binding.tvResultIcon.text = "❌"
                binding.tvResultTitle.setTextColor(0xFFFFCDD2.toInt())
            }
            "MODERATE" -> {
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
        binding.tvResultTitle.text = report.verdict
        binding.tvResultScore.text = "Score: ${report.score}"

        binding.tvHighRiskTitle.text = "Concerns"
        bindSection(
            card = binding.cardHighRisk,
            textView = binding.tvHighRiskItems,
            items = report.concerns
        )

        binding.tvSafeTitle.text = "Benefits"
        bindSection(
            card = binding.cardSafe,
            textView = binding.tvSafeItems,
            items = report.benefits
        )

        binding.tvModerateTitle.text = "Neutral Ingredients"
        bindNeutralSection(report.neutral, report.limitedInsight)

        lifecycleScope.launch {
            val structuredData = buildStructuredData(report)
            val offlineText = generateOfflineVerdict(report)
            binding.tvVerdict.text = offlineText

            if (isNetworkAvailable(this@ResultActivity) && API_KEY.isNotBlank()) {
                val aiText = geminiService.generateVerdictText(API_KEY, structuredData)
                if (!aiText.isNullOrBlank()) {
                    runOnUiThread {
                        binding.tvVerdict.text = aiText.trim()
                    }
                }
            }
        }

        // ── Scan Again ───────────────────────────────────────────────
        binding.btnScanAgain.setOnClickListener { finish() }
    }

    private fun bindSection(
        card: View,
        textView: TextView,
        items: List<String>
    ) {
        val lines = buildLimitedBulletLines(items)
        if (lines.isEmpty()) {
            card.visibility = View.GONE
            return
        }

        textView.text = lines.joinToString("\n")
        card.visibility = View.VISIBLE
    }

    private fun bindNeutralSection(
        neutralItems: List<String>,
        limitedInsightItems: List<String>
    ) {
        val neutralLines = buildLimitedBulletLines(neutralItems).toMutableList()
        val limitedInsightLines = buildLimitedBulletLines(limitedInsightItems)

        if (limitedInsightLines.isNotEmpty()) {
            if (neutralLines.isNotEmpty()) neutralLines += ""
            neutralLines += "  • Limited Insight:"
            neutralLines += limitedInsightLines.map { it.replaceFirst("  • ", "    - ") }
        }

        if (neutralLines.isEmpty()) {
            binding.cardModerate.visibility = View.GONE
            return
        }

        binding.tvModerateItems.text = neutralLines.joinToString("\n")
        binding.cardModerate.visibility = View.VISIBLE
    }

    private fun buildLimitedBulletLines(items: List<String>): List<String> {
        if (items.isEmpty()) return emptyList()

        val visibleItems = items.take(MAX_VISIBLE_ITEMS)
        val hiddenCount = (items.size - visibleItems.size).coerceAtLeast(0)
        val lines = visibleItems.map { "  • $it" }.toMutableList()
        if (hiddenCount > 0) {
            lines += "  • +$hiddenCount more ingredients"
        }
        return lines
    }

    private fun buildStructuredData(report: ResultReport): String {
        return """
Verdict: ${report.verdict}
Score: ${report.score}
Concerns: ${report.concerns}
Benefits: ${report.benefits}
Neutral Ingredients: ${report.neutral}
Limited Insight: ${report.limitedInsight}
""".trimIndent()
    }

    private fun generateOfflineVerdict(report: ResultReport): String {
        return SentenceGenerator.generateOfflineVerdict(
            verdict = report.verdict,
            score = report.score,
            concernCount = report.concerns.size,
            benefitCount = report.benefits.size,
            neutralCount = report.neutral.size,
            limitedInsightCount = report.limitedInsight.size
        )
    }
}
