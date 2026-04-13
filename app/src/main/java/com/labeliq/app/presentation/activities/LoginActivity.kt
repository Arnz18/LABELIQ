package com.labeliq.app.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.data.local.loginUser
import com.labeliq.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.tvGoSignup.setOnClickListener { goToSignup() }
    }

    // ── Auth ─────────────────────────────────────────────────────────────────────

    private fun attemptLogin() {
        val email    = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (!validateInputs(email, password)) return

        val success = loginUser(this, email, password)
        if (success) {
            goToMain()
        } else {
            showError("Incorrect email or password.")
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────────

    private fun validateInputs(email: String, password: String): Boolean {
        var valid = true

        if (email.isBlank()) {
            binding.etEmail.error = "Enter valid email"
            valid = false
        } else {
            binding.etEmail.error = null
        }

        if (password.isBlank()) {
            binding.etPassword.error = "Enter your password"
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

    private fun goToSignup() {
        startActivity(Intent(this, SignupActivity::class.java))
    }

    // ── UI helpers ────────────────────────────────────────────────────────────────

    private fun showError(message: String) {
        binding.tvLoginError.text    = message
        binding.tvLoginError.visibility = View.VISIBLE
    }
}
