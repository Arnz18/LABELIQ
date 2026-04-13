package com.labeliq.app.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.registerAndLoginUser
import com.labeliq.app.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener { attemptSignup() }
        binding.tvGoLogin.setOnClickListener { finish() }   // back to LoginActivity
    }

    // ── Auth ─────────────────────────────────────────────────────────────────────

    private fun attemptSignup() {
        val name     = binding.etName.text?.toString().orEmpty().trim()
        val email    = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (!validateInputs(name, email, password)) return

        val success = registerAndLoginUser(
            context       = this,
            name          = name,
            email         = email,
            password      = password,
            isDiabetic    = binding.switchDiabetic.isChecked,
            isVegan       = binding.switchVegan.isChecked,
            hasNutAllergy = binding.switchNutAllergy.isChecked
        )

        if (success) {
            // Mark setup as complete so MainActivity skips SetupActivity
            getSharedPreferences("app_setup", android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("is_first_time", false).apply()
            goToMain()
        } else {
            showError("An account with this email already exists.")
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────────

    private fun validateInputs(name: String, email: String, password: String): Boolean {
        var valid = true

        if (name.length < 2) {
            binding.etName.error = "Enter valid name"
            valid = false
        } else {
            binding.etName.error = null
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter valid email"
            valid = false
        } else {
            binding.etEmail.error = null
        }

        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            valid = false
        } else {
            binding.etPassword.error = null
        }

        return valid
    }

    // ── Navigation ────────────────────────────────────────────────────────────────

    private fun goToMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    // ── UI helpers ────────────────────────────────────────────────────────────────

    private fun showError(message: String) {
        binding.tvSignupError.text       = message
        binding.tvSignupError.visibility = View.VISIBLE
    }
}
