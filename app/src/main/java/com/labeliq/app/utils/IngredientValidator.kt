package com.labeliq.app.utils

/**
 * Lightweight heuristic to determine whether a block of OCR text looks like
 * an ingredient list, rather than arbitrary (non-food) text.
 *
 * The check is intentionally permissive to avoid false-negatives on real labels,
 * while still blocking clearly non-food scans (receipts, books, road signs, etc.).
 */

private val FOOD_KEYWORDS = setOf(
    "sugar", "salt", "oil", "milk", "flour", "wheat",
    "cocoa", "starch", "spice", "water", "butter", "cream",
    "syrup", "corn", "soy", "yeast", "acid", "vinegar",
    "chocolate", "vanilla", "protein", "fat", "sodium", "fiber"
)

/**
 * Returns `true` when [text] is likely an ingredient list.
 *
 * Decision logic — returns TRUE if ANY condition holds:
 *   A) keywordMatches >= 2  (standard multi-ingredient label)
 *   B) keywordMatches >= 1  AND  totalWords >= 3  (short label, e.g. "Water, Salt")
 *   C) keywordMatches >= 1  AND  text contains "ingredients" or "contains"
 *      (label header is a strong structural signal, e.g. "Ingredients: Water")
 *
 * Always returns FALSE for blank input or zero keyword matches.
 */
fun isLikelyIngredientList(text: String): Boolean {
    if (text.isBlank()) return false

    val lower = text.lowercase()
    val words = lower.split(Regex("\\W+")).filter { it.isNotBlank() }

    val matchCount = words.count { it in FOOD_KEYWORDS }

    // No food keyword found → definitely not an ingredient list
    if (matchCount == 0) return false

    // A) Two or more keyword hits → high confidence
    if (matchCount >= 2) return true

    // B) One keyword hit + at least 3 words → plausible short list
    if (words.size >= 3) return true

    // C) One keyword hit + explicit label header → strong structural signal
    val hasLabelHeader = "ingredients" in lower || "contains" in lower
    if (hasLabelHeader) return true

    return false
}
