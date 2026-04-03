package com.labeliq.app.data.local

import android.content.Context

// ── Data class ────────────────────────────────────────────────────────────────
data class UserProfile(
    val name: String         = "Guest",
    val isDiabetic: Boolean  = false,
    val isVegan: Boolean     = false,
    val hasNutAllergy: Boolean = false
)

// ── Constants ─────────────────────────────────────────────────────────────────
private const val PREFS_FILE   = "user_profile"
private const val KEY_NAME     = "name"
private const val KEY_DIABETIC = "isDiabetic"
private const val KEY_VEGAN    = "isVegan"
private const val KEY_NUT      = "hasNutAllergy"

// ── Save ──────────────────────────────────────────────────────────────────────
fun saveUserProfile(context: Context, profile: UserProfile) {
    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_NAME,     profile.name)
        .putBoolean(KEY_DIABETIC, profile.isDiabetic)
        .putBoolean(KEY_VEGAN,    profile.isVegan)
        .putBoolean(KEY_NUT,      profile.hasNutAllergy)
        .apply()
}

// ── Load ──────────────────────────────────────────────────────────────────────
/**
 * Returns the stored profile, or a default UserProfile() if nothing is saved yet.
 */
fun loadUserProfile(context: Context): UserProfile {
    val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    return UserProfile(
        name          = prefs.getString(KEY_NAME, "Guest") ?: "Guest",
        isDiabetic    = prefs.getBoolean(KEY_DIABETIC, false),
        isVegan       = prefs.getBoolean(KEY_VEGAN,    false),
        hasNutAllergy = prefs.getBoolean(KEY_NUT,      false)
    )
}
