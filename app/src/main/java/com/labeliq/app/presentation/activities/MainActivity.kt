package com.labeliq.app.presentation.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.getCurrentUserId
import com.labeliq.app.databinding.ActivityMainBinding
import com.labeliq.app.presentation.viewmodels.MainViewModel

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

        observeViewModel()
        setupClickListeners()
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
        binding.btnScanIngredients.setOnClickListener {
            viewModel.onScanClicked()
        }
        binding.btnViewHistory.setOnClickListener {
            viewModel.onHistoryClicked()
        }
        binding.btnProfile.setOnClickListener {
            viewModel.onProfileClicked()
        }
    }
}
