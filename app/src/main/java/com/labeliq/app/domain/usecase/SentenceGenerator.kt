package com.labeliq.app.domain.usecase

import kotlin.random.Random

object SentenceGenerator {

    fun generateConcernSentence(ingredient: String, reasons: List<String>): String {
        val cleanedReasons = reasons
            .map(::normalizeReasonText)
            .filter { it.isNotBlank() }
            .distinct()

        val reasonText = if (cleanedReasons.isNotEmpty()) {
            cleanedReasons.toNaturalList()
        } else {
            "your health profile"
        }

        val template = concernTemplates.random()
        return template.format(ingredient, reasonText)
    }

    fun generateBenefitSentence(ingredient: String, reasons: List<String>): String {
        val cleanedReasons = reasons
            .map(::normalizeReasonText)
            .filter { it.isNotBlank() }
            .distinct()

        return if (cleanedReasons.isNotEmpty()) {
            benefitTemplatesWithReason.random().format(ingredient, cleanedReasons.toNaturalList())
        } else {
            benefitTemplatesGeneric.random().format(ingredient)
        }
    }

    fun generateNeutralSentence(ingredient: String): String {
        return neutralTemplates.random().format(ingredient)
    }

    fun generateLimitedInsightSentence(ingredient: String): String {
        return limitedInsightTemplates.random().format(ingredient)
    }

    fun generateOfflineVerdict(
        verdict: String,
        score: Int,
        concernCount: Int,
        benefitCount: Int,
        neutralCount: Int,
        limitedInsightCount: Int
    ): String {
        val verdictLine = when (verdict) {
            "HIGH RISK" -> highRiskVerdictTemplates.random()
            "SAFE" -> safeVerdictTemplates.random()
            else -> moderateVerdictTemplates.random()
        }

        val contextParts = mutableListOf<String>()
        if (concernCount > 0) contextParts += "$concernCount concern ingredient${pluralSuffix(concernCount)}"
        if (benefitCount > 0) contextParts += "$benefitCount beneficial ingredient${pluralSuffix(benefitCount)}"
        if (neutralCount > 0) contextParts += "$neutralCount neutral ingredient${pluralSuffix(neutralCount)}"
        if (limitedInsightCount > 0) contextParts += "$limitedInsightCount ingredient${pluralSuffix(limitedInsightCount)} with limited insight"

        val contextLine = if (contextParts.isNotEmpty()) {
            "Detected ${contextParts.toNaturalList()}."
        } else {
            "No major ingredient signals were detected."
        }

        return "$verdictLine Score: $score. $contextLine"
    }

    private fun normalizeReasonText(reason: String): String {
        return reason
            .trim()
            .trimEnd('.', ',')
            .replace(Regex("\\s+"), " ")
    }

    private fun pluralSuffix(count: Int): String = if (count == 1) "" else "s"

    private fun List<String>.toNaturalList(): String {
        return when (size) {
            0 -> ""
            1 -> this[0]
            2 -> "${this[0]} and ${this[1]}"
            else -> {
                val head = dropLast(1).joinToString(", ")
                "$head, and ${last()}"
            }
        }
    }

    private fun <T> List<T>.random(): T {
        return this[Random.nextInt(size)]
    }

    private val concernTemplates = listOf(
        "%s may not be ideal for your health profile due to %s.",
        "%s should be limited because of %s.",
        "%s could negatively affect %s.",
        "%s is better in moderation given %s."
    )

    private val benefitTemplatesWithReason = listOf(
        "%s may support %s.",
        "%s can be helpful for %s.",
        "%s is generally beneficial for %s."
    )

    private val benefitTemplatesGeneric = listOf(
        "%s can support overall health.",
        "%s is generally considered beneficial.",
        "%s may be a positive ingredient for most profiles."
    )

    private val neutralTemplates = listOf(
        "%s is commonly used and generally safe.",
        "%s is widely used and poses low risk.",
        "%s is typically safe in normal amounts."
    )

    private val limitedInsightTemplates = listOf(
        "%s has limited available evidence in our database.",
        "%s has limited insight, so monitor intake cautiously.",
        "%s is less understood, so occasional use is safer."
    )

    private val highRiskVerdictTemplates = listOf(
        "This product appears high risk for your profile.",
        "This label looks risky for your current health profile."
    )

    private val moderateVerdictTemplates = listOf(
        "This product falls in a moderate range for your profile.",
        "This label shows a mixed profile with moderate caution."
    )

    private val safeVerdictTemplates = listOf(
        "This product appears generally safe for your profile.",
        "This label looks mostly suitable for your health profile."
    )
}
