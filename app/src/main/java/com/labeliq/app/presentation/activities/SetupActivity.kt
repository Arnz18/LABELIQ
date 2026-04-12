package com.labeliq.app.presentation.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.UserProfile
import com.labeliq.app.data.local.saveUserProfile
import com.labeliq.app.databinding.ActivitySetupBinding
import com.labeliq.app.domain.usecase.parseNote
import java.util.Locale

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
        val conditions = parseCsv(binding.etConditions.text?.toString())
            .toMutableSet()
        val allergies = parseCsv(binding.etAllergies.text?.toString())
            .toMutableSet()
        val preferences = parseCsv(binding.etPreferences.text?.toString())
            .toMutableSet()
        val avoidTags = parseCsv(binding.etAvoidTags.text?.toString())
            .toMutableSet()
        val customNote = binding.etCustomNote.text?.toString()?.trim().orEmpty()
        val noteTags = parseNote(customNote)

        if (binding.switchDiabetic.isChecked) conditions += "diabetes"
        if (binding.switchNutAllergy.isChecked) allergies += "nuts"
        if (binding.switchVegan.isChecked) preferences += "vegan"
        avoidTags += noteTags

        val dietGoal = normalizeDietGoal(binding.etDietGoal.text?.toString(), noteTags)
        val lifestyle = normalizeLifestyle(binding.etLifestyle.text?.toString())

        val profile = UserProfile(
            name = name,
            conditions = conditions.toList(),
            allergies = allergies.toList(),
            dietGoal = dietGoal,
            lifestyle = lifestyle,
            preferences = preferences.toList(),
            avoidTags = avoidTags.toList(),
            customNote = customNote
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

    private fun parseCsv(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",")
            .map { normalizeToken(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun normalizeDietGoal(raw: String?, noteTags: List<String>): String {
        val normalized = normalizeToken(raw ?: "")
        if (normalized in setOf("fat_loss", "muscle_gain", "balanced")) return normalized
        if ("fat_loss" in noteTags) return "fat_loss"
        if ("protein" in noteTags) return "muscle_gain"
        return "balanced"
    }

    private fun normalizeLifestyle(raw: String?): String {
        val normalized = normalizeToken(raw ?: "")
        return if (normalized in setOf("athlete", "normal")) normalized else "normal"
    }

    private fun normalizeToken(raw: String): String {
        return raw
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9\\s_-]"), " ")
            .replace(Regex("[\\s-]+"), "_")
            .trim('_')
            .trim()
    }
}
