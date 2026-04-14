package com.labeliq.app.presentation.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.labeliq.app.R
import com.labeliq.app.data.local.UserProfile
import com.labeliq.app.data.local.registerAndLoginUser
import com.labeliq.app.data.local.saveUserProfile
import com.labeliq.app.databinding.ActivitySignupBinding
import com.labeliq.app.domain.usecase.parseNote

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    // ── Step state ────────────────────────────────────────────────────────────────
    private var onStep2 = false

    // ── Dropdown options ──────────────────────────────────────────────────────────
    // Display label → internal key used by UserProfile / RiskEngine
    private val dietGoalOptions = linkedMapOf(
        "Balanced"     to "balanced",
        "Weight Loss"  to "fat_loss",
        "Muscle Gain"  to "muscle_gain"
    )
    private val lifestyleOptions = linkedMapOf(
        "Normal"    to "normal",
        "Athlete"   to "athlete",
        "Sedentary" to "normal"   // treat sedentary same as normal for now
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdowns()
        showStep1()

        binding.btnNext.setOnClickListener        { onNextClicked() }
        binding.btnSignup.setOnClickListener      { onCreateAccountClicked() }
        binding.tvBackToStep1.setOnClickListener  { showStep1() }
        binding.tvGoLogin.setOnClickListener      { finish() }
    }

    // ── Dropdown setup ────────────────────────────────────────────────────────────

    private fun setupDropdowns() {
        val dietAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            dietGoalOptions.keys.toList()
        )
        binding.actvDietGoal.setAdapter(dietAdapter)
        binding.actvDietGoal.setText(dietGoalOptions.keys.first(), false) // default: Balanced

        val lifestyleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            lifestyleOptions.keys.toList()
        )
        binding.actvLifestyle.setAdapter(lifestyleAdapter)
        binding.actvLifestyle.setText(lifestyleOptions.keys.first(), false) // default: Normal
    }

    // ── Step navigation ───────────────────────────────────────────────────────────

    private fun showStep1() {
        onStep2 = false
        binding.layoutStep1.visibility = View.VISIBLE
        binding.layoutStep2.visibility = View.GONE
        binding.tvSignupTagline.text   = "Create your account"
        binding.viewStep1Dot.background = getDrawable(R.drawable.step_dot_active)
        binding.viewStep2Dot.background = getDrawable(R.drawable.step_dot_inactive)
        clearStep1Errors()
    }

    private fun showStep2() {
        onStep2 = true
        binding.layoutStep1.visibility = View.GONE
        binding.layoutStep2.visibility = View.VISIBLE
        binding.tvSignupTagline.text   = "Set your preferences"
        binding.viewStep1Dot.background = getDrawable(R.drawable.step_dot_inactive)
        binding.viewStep2Dot.background = getDrawable(R.drawable.step_dot_active)
        binding.tvSignupError.visibility = View.GONE
    }

    // ── Button handlers ───────────────────────────────────────────────────────────

    private fun onNextClicked() {
        val name     = binding.etName.text?.toString().orEmpty().trim()
        val email    = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (validateStep1(name, email, password)) showStep2()
    }

    private fun onCreateAccountClicked() {
        val name     = binding.etName.text?.toString().orEmpty().trim()
        val email    = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()

        val success = registerAndLoginUser(
            context       = this,
            name          = name,
            email         = email,
            password      = password,
            isDiabetic    = binding.switchDiabetic.isChecked,
            isVegan       = binding.switchVegan.isChecked,
            hasNutAllergy = binding.switchNutAllergy.isChecked
        )

        if (!success) {
            showStep2Error("An account with this email already exists.")
            return
        }

        // ── Read chip selections ─────────────────────────────────────────────
        val conditions = mutableSetOf<String>()
        if (binding.chipConditionDiabetes.isChecked)     conditions += "diabetes"
        if (binding.chipConditionHeart.isChecked)        conditions += "heart_disease"
        if (binding.chipConditionHypertension.isChecked) conditions += "hypertension"
        if (binding.chipConditionThyroid.isChecked)      conditions += "thyroid"

        val allergies = mutableSetOf<String>()
        if (binding.chipAllergyNuts.isChecked)      allergies += "nuts"
        if (binding.chipAllergyGluten.isChecked)    allergies += "gluten"
        if (binding.chipAllergyDairy.isChecked)     allergies += "dairy"
        if (binding.chipAllergyShellfish.isChecked) allergies += "shellfish"
        if (binding.chipAllergySoy.isChecked)       allergies += "soy"

        // ── Sync switches → conditions / allergies ───────────────────────────
        if (binding.switchDiabetic.isChecked)   conditions += "diabetes"
        if (binding.switchNutAllergy.isChecked) allergies  += "nuts"

        // ── Read dropdowns → internal keys ──────────────────────────────────
        val dietGoalLabel  = binding.actvDietGoal.text?.toString().orEmpty()
        val lifestyleLabel = binding.actvLifestyle.text?.toString().orEmpty()
        val dietGoal  = dietGoalOptions[dietGoalLabel]  ?: "balanced"
        val lifestyle = lifestyleOptions[lifestyleLabel] ?: "normal"

        // ── Preferences from vegan switch ────────────────────────────────────
        val preferences = mutableSetOf<String>()
        if (binding.switchVegan.isChecked) preferences += "vegan"

        // ── Custom note → avoidTags ──────────────────────────────────────────
        val customNote = binding.etCustomNote.text?.toString()?.trim().orEmpty()
        val noteTags   = parseNote(customNote)
        val avoidTags  = noteTags.toMutableSet()

        // ── Build and save UserProfile ────────────────────────────────────────
        val profile = UserProfile(
            name        = name.ifBlank { "Guest" },
            conditions  = conditions.toList(),
            allergies   = allergies.toList(),
            dietGoal    = dietGoal,
            lifestyle   = lifestyle,
            preferences = preferences.toList(),
            avoidTags   = avoidTags.toList(),
            customNote  = customNote
        )
        saveUserProfile(this, profile)

        // ── Mark setup complete — new users skip SetupActivity ────────────────
        getSharedPreferences("app_setup", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_first_time", false)
            .apply()

        goToMain()
    }

    // ── Step 1 validation ─────────────────────────────────────────────────────────

    private fun validateStep1(name: String, email: String, password: String): Boolean {
        var valid = true

        if (name.length < 2) {
            binding.etName.error = "Enter a valid name"
            valid = false
        } else {
            binding.etName.error = null
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Enter a valid email"
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

        if (!valid) {
            binding.tvStep1Error.text       = "Please fix the errors above."
            binding.tvStep1Error.visibility = View.VISIBLE
        }

        return valid
    }

    private fun clearStep1Errors() {
        binding.tvStep1Error.visibility = View.GONE
        binding.etName.error     = null
        binding.etEmail.error    = null
        binding.etPassword.error = null
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

    private fun showStep2Error(message: String) {
        binding.tvSignupError.text       = message
        binding.tvSignupError.visibility = View.VISIBLE
    }
}
