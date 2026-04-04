package com.labeliq.app.presentation.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.databinding.ActivityMainBinding
import com.labeliq.app.presentation.viewmodels.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    private fun setupClickListeners() {
        binding.btnScanIngredients.setOnClickListener {
            viewModel.onScanClicked()
        }
        binding.btnViewHistory.setOnClickListener {
            viewModel.onHistoryClicked()
        }
    }
}
