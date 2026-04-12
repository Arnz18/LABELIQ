package com.labeliq.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.labeliq.app.domain.usecase.parseNote
import java.util.Locale

// ── Data class ────────────────────────────────────────────────────────────────
data class UserProfile(
    val name: String = "Guest",
    val conditions: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val dietGoal: String = "balanced",
    val lifestyle: String = "normal",
    val preferences: List<String> = emptyList(),
    val avoidTags: List<String> = emptyList(),
    val customNote: String = ""
)

// ── Constants ─────────────────────────────────────────────────────────────────
private const val PREFS_FILE = "user_profile"
private const val KEY_PROFILE_JSON = "profile_json"

// Legacy keys used by older builds.
private const val KEY_NAME = "name"
private const val KEY_DIABETIC = "isDiabetic"
private const val KEY_VEGAN = "isVegan"
private const val KEY_NUT = "hasNutAllergy"

private val gson = Gson()

// ── Save ──────────────────────────────────────────────────────────────────────
fun saveUserProfile(context: Context, profile: UserProfile) {
    val cleanProfile = sanitizeProfile(profile)

    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PROFILE_JSON, gson.toJson(cleanProfile))
        .putString(KEY_NAME, cleanProfile.name)
        .putBoolean(KEY_DIABETIC, "diabetes" in cleanProfile.conditions)
        .putBoolean(KEY_VEGAN, "vegan" in cleanProfile.preferences)
        .putBoolean(KEY_NUT, "nuts" in cleanProfile.allergies || "nut_allergy" in cleanProfile.allergies)
        .apply()
}

// ── Load ──────────────────────────────────────────────────────────────────────
/**
 * Returns the stored profile, or a default UserProfile() if nothing is saved yet.
 */
fun loadUserProfile(context: Context): UserProfile {
    val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    val storedJson = prefs.getString(KEY_PROFILE_JSON, null)
    if (!storedJson.isNullOrBlank()) {
        runCatching { gson.fromJson(storedJson, UserProfile::class.java) }
            .getOrNull()
            ?.let { return sanitizeProfile(it) }
    }

    // Backward-compatible migration for older profile structure.
    val name = prefs.getString(KEY_NAME, "Guest") ?: "Guest"
    val legacyDiabetic = prefs.getBoolean(KEY_DIABETIC, false)
    val legacyVegan = prefs.getBoolean(KEY_VEGAN, false)
    val legacyNut = prefs.getBoolean(KEY_NUT, false)

    val migrated = UserProfile(
        name = name,
        conditions = if (legacyDiabetic) listOf("diabetes") else emptyList(),
        allergies = if (legacyNut) listOf("nuts") else emptyList(),
        dietGoal = "balanced",
        lifestyle = "normal",
        preferences = if (legacyVegan) listOf("vegan") else emptyList(),
        avoidTags = emptyList(),
        customNote = ""
    )

    return sanitizeProfile(migrated)
}

private fun sanitizeProfile(profile: UserProfile): UserProfile {
    val cleanName = profile.name.trim().ifBlank { "Guest" }
    val conditions = normalizeList(profile.conditions)
    val allergies = normalizeList(profile.allergies)
    val preferences = normalizeList(profile.preferences)
    val directAvoidTags = normalizeList(profile.avoidTags)
    val customNote = profile.customNote.trim()

    val derivedAvoidTags = LinkedHashSet<String>()
    derivedAvoidTags += directAvoidTags
    derivedAvoidTags += parseNote(customNote).map(::normalizeTag)

    if ("diabetes" in conditions) derivedAvoidTags += "high_glycemic"
    if ("hypertension" in conditions) derivedAvoidTags += "high_sodium"
    if ("nuts" in allergies || "nut_allergy" in allergies) derivedAvoidTags += "nuts"
    if ("lactose" in allergies || "lactose_intolerance" in allergies) derivedAvoidTags += "dairy"
    if ("vegan" in preferences) {
        derivedAvoidTags += "animal"
        derivedAvoidTags += "dairy"
    }

    return UserProfile(
        name = cleanName,
        conditions = conditions,
        allergies = allergies,
        dietGoal = normalizeGoal(profile.dietGoal),
        lifestyle = normalizeLifestyle(profile.lifestyle),
        preferences = preferences,
        avoidTags = derivedAvoidTags.toList(),
        customNote = customNote
    )
}

private fun normalizeGoal(raw: String): String {
    return when (normalizeTag(raw)) {
        "fat_loss", "muscle_gain", "balanced" -> normalizeTag(raw)
        else -> "balanced"
    }
}

private fun normalizeLifestyle(raw: String): String {
    return when (normalizeTag(raw)) {
        "athlete", "normal" -> normalizeTag(raw)
        else -> "normal"
    }
}

private fun normalizeList(values: List<String>): List<String> {
    return values
        .map(::normalizeTag)
        .filter { it.isNotBlank() }
        .distinct()
}

private fun normalizeTag(raw: String): String {
    return raw
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9\\s_-]"), " ")
        .replace(Regex("[\\s-]+"), "_")
        .trim('_')
        .trim()
}
