package com.labeliq.app.presentation.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.getCurrentUser
import com.labeliq.app.data.local.getCurrentUserId
import com.labeliq.app.data.local.loadScanHistory
import com.labeliq.app.databinding.ActivityMainBinding
import com.labeliq.app.presentation.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Auth gate — redirect to Login if no active session ───────
        if (getCurrentUserId(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        // ── First-time setup gate ────────────────────────────────────
        if (SetupActivity.isFirstTime(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindUserName()
        updateDashboardStats()
        observeViewModel()
        setupClickListeners()
    }

    /** Populate the greeting with the stored user name (fallback: "User") */
    private fun bindUserName() {
        val name = getCurrentUser(this)?.name?.ifBlank { "User" } ?: "User"
        binding.tvGreetingName.text = name
    }

    private fun updateDashboardStats() {
        val history = loadScanHistory(this)
        if (history.isEmpty()) {
            binding.tvInsightsSafe.text = "0"
            binding.tvInsightsWarning.text = "0"
            binding.tvInsightsDanger.text = "0"
            binding.tvLastScanStatus.text = "No scans yet"
            binding.tvLastScanTime.text = "-"
            return
        }

        // Insights
        val danger = history.count { it.status == "HIGH RISK" }
        val caution = history.count { it.status == "MODERATE" }
        val safe = history.count { it.status == "SAFE" }

        binding.tvInsightsSafe.text = safe.toString()
        binding.tvInsightsWarning.text = caution.toString()
        binding.tvInsightsDanger.text = danger.toString()

        // Last Scan
        val last = history.first() // assuming history is sorted newest-first
        val icon = when (last.status) {
            "HIGH RISK" -> "❌ "
            "MODERATE" -> "⚠️ "
            else -> "✅ "
        }
        binding.tvLastScanStatus.text = "$icon${last.status}"

        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        binding.tvLastScanTime.text = sdf.format(Date(last.timestamp))
    }

    private fun observeViewModel() {
        viewModel.navigateToScan.observe(this) { shouldNavigate ->
            if (shouldNavigate == true) {
                startActivity(Intent(this, ScanActivity::class.java))
                viewModel.onNavigated()
            }
        }
        viewModel.navigateToHistory.observe(this) { shouldNavigate ->
            if (shouldNavigate == true) {
                startActivity(Intent(this, HistoryActivity::class.java))
                viewModel.onNavigated()
            }
        }
        viewModel.navigateToProfile.observe(this) { shouldNavigate ->
            if (shouldNavigate == true) {
                startActivity(Intent(this, ProfileActivity::class.java))
                viewModel.onNavigated()
            }
        }
    }

    private fun setupClickListeners() {
        // Big scan CTA
        binding.btnScanIngredients.setOnClickListener {
            viewModel.onScanClicked()
        }
        // Tap last scan card → goes to history
        binding.cardLastScan.setOnClickListener {
            viewModel.onHistoryClicked()
        }
        // Quick actions
        binding.btnViewHistory.setOnClickListener {
            viewModel.onHistoryClicked()
        }
        binding.btnProfile.setOnClickListener {
            viewModel.onProfileClicked()
        }
        // Bottom nav bar
        binding.navHistory.setOnClickListener {
            viewModel.onHistoryClicked()
        }
        binding.navProfile.setOnClickListener {
            viewModel.onProfileClicked()
        }
    }
}
