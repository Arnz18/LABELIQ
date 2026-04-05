package com.labeliq.app.presentation.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.UserProfile
import com.labeliq.app.data.local.loadUserProfile
import com.labeliq.app.data.local.saveUserProfile
import com.labeliq.app.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Toolbar back navigation ──────────────────────────────────
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ── Load saved profile ───────────────────────────────────────
        val profile = loadUserProfile(this)

        // ── Populate UI with saved values ────────────────────────────
        binding.etName.setText(profile.name)
        binding.switchDiabetic.isChecked  = profile.isDiabetic
        binding.switchVegan.isChecked     = profile.isVegan
        binding.switchNutAllergy.isChecked = profile.hasNutAllergy

        // ── Save button ──────────────────────────────────────────
        binding.btnSaveProfile.setOnClickListener {
            val updatedProfile = UserProfile(
                name          = binding.etName.text.toString().trim(),
                isDiabetic    = binding.switchDiabetic.isChecked,
                isVegan       = binding.switchVegan.isChecked,
                hasNutAllergy = binding.switchNutAllergy.isChecked
            )
            saveUserProfile(this, updatedProfile)
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
