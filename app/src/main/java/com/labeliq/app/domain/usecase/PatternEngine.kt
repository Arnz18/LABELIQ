package com.labeliq.app.domain.usecase

import com.labeliq.app.domain.model.PatternResult

class PatternEngine {

    fun classify(name: String): PatternResult {
        val normalized = normalize(name)
        if (normalized.isBlank()) {
            return PatternResult(
                category = "unknown",
                riskLevel = "LOW",
                explanation = "No harmful pattern detected",
                confidence = "LOW"
            )
        }

        return when {
            nutrientKeywords.any { it in normalized } -> PatternResult(
                category = "nutrient",
                riskLevel = "LOW",
                explanation = "Essential nutrient beneficial for health",
                confidence = "HIGH"
            )

            safeAdditiveKeywords.any { it in normalized } -> PatternResult(
                category = "additive",
                riskLevel = "LOW",
                explanation = "Common additive, generally safe in small amounts",
                confidence = "MEDIUM"
            )

            normalized.endsWith("ate") -> PatternResult(
                category = "additive",
                riskLevel = "LOW",
                explanation = "Common additive, generally safe in small amounts",
                confidence = "MEDIUM"
            )

            normalized.endsWith("ite") -> PatternResult(
                category = "sulfite",
                riskLevel = "MODERATE",
                explanation = "May be a sulfite-related additive; sensitive users should monitor intake",
                confidence = "MEDIUM"
            )

            normalized.endsWith("ol") -> PatternResult(
                category = "sweetener",
                riskLevel = "MODERATE",
                explanation = "May be a sweetener; moderate intake is usually preferred",
                confidence = "MEDIUM"
            )

            "gum" in normalized -> PatternResult(
                category = "stabilizer",
                riskLevel = "LOW",
                explanation = "Stabilizer commonly used for texture, generally safe",
                confidence = "MEDIUM"
            )

            else -> PatternResult(
                category = "unknown",
                riskLevel = "LOW",
                explanation = "No harmful pattern detected",
                confidence = "LOW"
            )
        }
    }

    fun classifyUnknown(name: String): PatternResult = classify(name)

    private val nutrientKeywords = setOf(
        "calcium",
        "magnesium",
        "zinc",
        "iron",
        "vitamin",
        "folic",
        "niacin"
    )

    private val safeAdditiveKeywords = setOf(
        "acid",
        "lecithin",
        "pectin"
    )
}
