package com.labeliq.app.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.UserProfile
import com.labeliq.app.data.local.getCurrentUser
import com.labeliq.app.data.local.loadUserProfile
import com.labeliq.app.data.local.logoutUser
import com.labeliq.app.data.local.saveUserProfile
import com.labeliq.app.data.local.updateUser
import com.labeliq.app.databinding.ActivityProfileBinding
import com.labeliq.app.domain.model.User
import com.labeliq.app.domain.usecase.parseNote
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Toolbar back navigation ──────────────────────────────────
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Load current user (name + dietary flags) + full profile ─────
        val user    = getCurrentUser(this)
        val profile = loadUserProfile(this)

        // ── Populate UI ──────────────────────────────────────────────
        // name / boolean flags → User is source of truth
        binding.etName.setText(user?.name ?: profile.name)
        binding.switchDiabetic.isChecked   = user?.isDiabetic    ?: ("diabetes" in profile.conditions)
        binding.switchVegan.isChecked      = user?.isVegan        ?: ("vegan" in profile.preferences)
        binding.switchNutAllergy.isChecked = user?.hasNutAllergy  ?: ("nuts" in profile.allergies || "nut_allergy" in profile.allergies)
        // rich fields → still from UserProfile
        binding.etConditions.setText(profile.conditions.joinToString(", "))
        binding.etAllergies.setText(profile.allergies.joinToString(", "))
        binding.etDietGoal.setText(profile.dietGoal)
        binding.etLifestyle.setText(profile.lifestyle)
        binding.etPreferences.setText(profile.preferences.joinToString(", "))
        binding.etAvoidTags.setText(profile.avoidTags.joinToString(", "))
        binding.etCustomNote.setText(profile.customNote)

        // ── Save button ──────────────────────────────────────────
        binding.btnSaveProfile.setOnClickListener {
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
            if (binding.switchVegan.isChecked) preferences += "vegan"
            if (binding.switchNutAllergy.isChecked) allergies += "nuts"
            avoidTags += noteTags

            val updatedProfile = UserProfile(
                name = binding.etName.text.toString().trim().ifBlank { "Guest" },
                conditions = conditions.toList(),
                allergies = allergies.toList(),
                dietGoal = normalizeDietGoal(binding.etDietGoal.text?.toString(), noteTags),
                lifestyle = normalizeLifestyle(binding.etLifestyle.text?.toString()),
                preferences = preferences.toList(),
                avoidTags = avoidTags.toList(),
                customNote = customNote
            )
            saveUserProfile(this, updatedProfile)

            // Also persist name + dietary flags back to the User record
            user?.let {
                val updatedUser = User(
                    id           = it.id,
                    name         = binding.etName.text.toString().trim().ifBlank { "Guest" },
                    email        = it.email,
                    password     = it.password,
                    isDiabetic   = binding.switchDiabetic.isChecked,
                    isVegan      = binding.switchVegan.isChecked,
                    hasNutAllergy = binding.switchNutAllergy.isChecked
                )
                updateUser(this, updatedUser)
            }

            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            finish()
        }

        // ── Logout button ────────────────────────────────────────────
        binding.btnLogout.setOnClickListener {
            // Clear session only — user data is preserved
            logoutUser(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
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
