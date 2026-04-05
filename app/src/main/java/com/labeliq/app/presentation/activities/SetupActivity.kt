package com.labeliq.app.presentation.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.UserProfile
import com.labeliq.app.data.local.saveUserProfile
import com.labeliq.app.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    companion object {
        private const val PREFS_SETUP   = "app_setup"
        private const val KEY_FIRST_TIME = "is_first_time"

        /** Call from any context to check whether setup has been completed. */
        fun isFirstTime(context: android.content.Context): Boolean {
            return context
                .getSharedPreferences(PREFS_SETUP, android.content.Context.MODE_PRIVATE)
                .getBoolean(KEY_FIRST_TIME, true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            saveAndProceed()
        }
    }

    private fun saveAndProceed() {
        // ── Build profile from UI inputs ─────────────────────────────
        val name        = binding.etName.text?.toString()?.trim()
                              .takeIf { !it.isNullOrEmpty() } ?: "Guest"
        val isDiabetic  = binding.switchDiabetic.isChecked
        val isVegan     = binding.switchVegan.isChecked
        val hasNutAllergy = binding.switchNutAllergy.isChecked

        val profile = UserProfile(
            name          = name,
            isDiabetic    = isDiabetic,
            isVegan       = isVegan,
            hasNutAllergy = hasNutAllergy
        )

        // ── Persist profile ──────────────────────────────────────────
        saveUserProfile(this, profile)

        // ── Mark setup as complete ───────────────────────────────────
        getSharedPreferences(PREFS_SETUP, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_TIME, false)
            .apply()

        // ── Navigate to MainActivity (clear back stack) ──────────────
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
