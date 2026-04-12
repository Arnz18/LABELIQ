package com.labeliq.app.domain.usecase

import java.util.Locale

fun parseNote(note: String): List<String> {
    if (note.isBlank()) return emptyList()

    val normalized = note
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    if (normalized.isBlank()) return emptyList()

    val tags = LinkedHashSet<String>()

    if (containsAny(normalized, "sugar", "sweet", "syrup", "glucose", "fructose")) {
        tags += "high_glycemic"
    }
    if (containsAny(normalized, "junk", "processed", "ultra processed", "fast food", "packet")) {
        tags += "processed"
    }
    if (containsAny(normalized, "muscle gain", "gain muscle", "bodybuilding", "protein", "bulk")) {
        tags += "protein"
    }
    if (containsAny(normalized, "fat loss", "weight loss", "lose weight", "cutting", "lean")) {
        tags += "fat_loss"
    }
    if (containsAny(normalized, "low sodium", "less salt", "avoid salt", "blood pressure")) {
        tags += "high_sodium"
    }
    if (containsAny(normalized, "low carb", "avoid carbs")) {
        tags += "high_glycemic"
    }
    if (containsAny(normalized, "energy", "endurance", "stamina")) {
        tags += "energy"
    }

    return tags.toList()
}

private fun containsAny(input: String, vararg phrases: String): Boolean {
    return phrases.any { phrase ->
        val escaped = Regex.escape(phrase.lowercase(Locale.US))
        Regex("\\b$escaped\\b").containsMatchIn(input)
    }
}
