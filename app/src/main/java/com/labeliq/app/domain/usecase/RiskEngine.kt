package com.labeliq.app.domain.usecase

import com.labeliq.app.data.local.UserProfile
import com.labeliq.app.data.repository.IngredientRepository
import com.labeliq.app.domain.model.IngredientInfo
import com.labeliq.app.domain.model.FinalReport
import com.labeliq.app.domain.model.IngredientInsight
import java.util.Locale

class RiskEngine(
    private val ingredientRepository: IngredientRepository,
    private val patternEngine: PatternEngine = PatternEngine()
) {
    fun evaluate(
        ingredients: List<String>,
        profile: UserProfile
    ): FinalReport {
        val concernInsights = linkedMapOf<String, IngredientInsight>()
        val benefitInsights = linkedMapOf<String, IngredientInsight>()
        val neutralInsights = linkedMapOf<String, IngredientInsight>()
        val limitedInsight = LinkedHashSet<String>()
        val strongNegativeIngredients = LinkedHashSet<String>()
        var score = 0

        val conditionSet = profile.conditions.map(::normalizeTag)
        val allergySet = profile.allergies.map(::normalizeTag)
        val preferenceSet = profile.preferences.map(::normalizeTag)
        val avoidTagSet = profile.avoidTags.map(::normalizeTag).toSet()
        val userSignals = buildSet {
            addAll(conditionSet)
            addAll(allergySet)
            addAll(preferenceSet)
            add(normalizeTag(profile.dietGoal))
            add(normalizeTag(profile.lifestyle))
        }

        ingredients
            .asSequence()
            .map(::normalizeIngredient)
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { ingredientName ->
                val ingredientMatch = ingredientRepository.findIngredientMatch(ingredientName)
                if (ingredientMatch != null) {
                    val displayName = toDisplayName(ingredientMatch.ingredient.name)
                    val knownResult = evaluateKnownIngredient(
                        ingredientInfo = ingredientMatch.ingredient,
                        userSignals = userSignals,
                        avoidTagSet = avoidTagSet,
                        profile = profile
                    )
                    score += knownResult.scoreDelta
                    mergeInsightReasons(concernInsights, displayName, knownResult.concernReasons)
                    mergeInsightReasons(benefitInsights, displayName, knownResult.benefitReasons)
                    mergeInsightReasons(neutralInsights, displayName, knownResult.neutralReasons)

                    if (knownResult.hasStrongNegative) {
                        strongNegativeIngredients += displayName
                    }

                    if (ingredientMatch.confidence == "MEDIUM") {
                        mergeInsightReasons(
                            neutralInsights,
                            displayName,
                            listOf("close ingredient variant match")
                        )
                    }
                } else {
                    val patternResult = patternEngine.classify(ingredientName)
                    val displayName = toDisplayName(ingredientName)

                    when (patternResult.category) {
                        "nutrient" -> {
                            score += 3
                            mergeInsightReasons(
                                benefitInsights,
                                displayName,
                                listOf("overall health")
                            )
                        }

                        "unknown" -> {
                            limitedInsight += displayName
                        }

                        else -> {
                            if (patternResult.riskLevel == "MODERATE") {
                                score -= 2
                                mergeInsightReasons(
                                    concernInsights,
                                    displayName,
                                    listOf("sensitive individuals")
                                )
                            } else {
                                mergeInsightReasons(
                                    neutralInsights,
                                    displayName,
                                    listOf(patternResult.explanation.ifBlank { patternResult.category })
                                )
                            }
                        }
                    }
                }
            }

        val concerns = concernInsights.values.map { insight ->
            SentenceGenerator.generateConcernSentence(insight.name, insight.reasons)
        }
        val benefits = benefitInsights.values.map { insight ->
            SentenceGenerator.generateBenefitSentence(insight.name, insight.reasons)
        }
        val neutral = neutralInsights.values.map { insight ->
            SentenceGenerator.generateNeutralSentence(insight.name)
        }
        val unknowns = limitedInsight.map { ingredient ->
            SentenceGenerator.generateLimitedInsightSentence(ingredient)
        }

        val verdict = determineVerdict(score, strongNegativeIngredients.size)
        val advice = SentenceGenerator.generateOfflineVerdict(
            verdict = verdict,
            score = score,
            concernCount = concerns.size,
            benefitCount = benefits.size,
            neutralCount = neutral.size,
            limitedInsightCount = unknowns.size
        )

        return FinalReport(
            concerns = concerns,
            benefits = benefits,
            neutral = neutral,
            unknowns = unknowns,
            score = score,
            verdict = verdict,
            advice = advice
        )
    }

    private fun evaluateKnownIngredient(
        ingredientInfo: IngredientInfo,
        userSignals: Set<String>,
        avoidTagSet: Set<String>,
        profile: UserProfile
    ): IngredientEvaluation {
        var delta = 0
        val concernReasons = mutableListOf<String>()
        val benefitReasons = mutableListOf<String>()
        val neutralReasons = mutableListOf<String>()
        val normalizedCategory = normalize(ingredientInfo.category)
        val normalizedTags = ingredientInfo.tags.map(::normalizeTag).toSet()
        val normalizedBadFor = ingredientInfo.badFor.map(::normalizeTag).toSet()
        val normalizedGoodFor = ingredientInfo.goodFor.map(::normalizeTag).toSet()
        val dietGoal = normalizeTag(profile.dietGoal)
        val lifestyle = normalizeTag(profile.lifestyle)
        val profileSignals = userSignals + setOf(dietGoal, lifestyle)
        var hasStrongNegative = false

        if (isNutrient(ingredientInfo, normalizedCategory)) {
            delta += 3
            benefitReasons += "overall health"
        }

        val badMatches = normalizedBadFor.intersect(profileSignals)
        if (badMatches.isNotEmpty()) {
            delta -= 3
            concernReasons += humanizeTags(badMatches)
            hasStrongNegative = true
        }

        val goodMatches = normalizedGoodFor.intersect(profileSignals)
        if (goodMatches.isNotEmpty()) {
            delta += 2
            benefitReasons += "your ${humanizeTags(goodMatches)} goals"
        }

        val avoidMatches = normalizedTags.intersect(avoidTagSet)
        if (avoidMatches.isNotEmpty()) {
            delta -= 4
            concernReasons += "your dietary preferences"
            hasStrongNegative = true
        }

        if (normalizedCategory == "unknown") {
            if (concernReasons.isEmpty() && benefitReasons.isEmpty()) {
                if (ingredientInfo.description.isNotBlank()) {
                    neutralReasons += "ingredient notes available"
                } else {
                    neutralReasons += "limited category metadata"
                }
            }
        } else if (concernReasons.isEmpty() && benefitReasons.isEmpty()) {
            if (isSafeAdditive(normalizedCategory, normalizedTags) || ingredientInfo.description.isNotBlank()) {
                neutralReasons += "commonly used ingredient"
            } else {
                neutralReasons += toNeutralReason(normalizedCategory)
            }
        }

        return IngredientEvaluation(
            scoreDelta = delta,
            concernReasons = concernReasons,
            benefitReasons = benefitReasons,
            neutralReasons = neutralReasons,
            hasStrongNegative = hasStrongNegative
        )
    }

    private fun determineVerdict(score: Int, strongNegativeIngredients: Int): String {
        return when {
            strongNegativeIngredients >= 2 && score <= -6 -> "HIGH RISK"
            score >= 4 && strongNegativeIngredients == 0 -> "SAFE"
            else -> "MODERATE"
        }
    }

    private fun mergeInsightReasons(
        target: MutableMap<String, IngredientInsight>,
        ingredientName: String,
        reasons: List<String>
    ) {
        if (reasons.isEmpty()) return
        val insight = target.getOrPut(ingredientName) {
            IngredientInsight(name = ingredientName, reasons = mutableListOf())
        }
        reasons
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { reason ->
                if (reason !in insight.reasons) {
                    insight.reasons += reason
                }
            }
    }

    private fun toNeutralReason(category: String): String {
        return when (category) {
            "additive" -> "common additive profile"
            "stabilizer" -> "texture stabilizer"
            "sulfite" -> "moderation is helpful for sensitivity"
            "sweetener" -> "best used in moderation"
            else -> "generally low-risk profile"
        }
    }

    private fun normalizeIngredient(value: String): String {
        return normalize(value)
    }

    private fun normalizeTag(value: String): String {
        return value
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9\\s_-]"), " ")
            .replace(Regex("[\\s-]+"), "_")
            .trim('_')
            .trim()
    }

    private fun isNutrient(ingredientInfo: IngredientInfo, normalizedCategory: String): Boolean {
        if (normalizedCategory == "nutrient") return true
        val normalizedName = normalize(ingredientInfo.name)
        return nutrientKeywords.any { keyword -> keyword in normalizedName }
    }

    private fun isSafeAdditive(category: String, tags: Set<String>): Boolean {
        if (category in additiveCategories) return true
        return tags.any { it in additiveTags }
    }

    private fun humanizeTags(tags: Set<String>): String {
        return tags
            .map { it.replace('_', ' ') }
            .distinct()
            .toNaturalList()
    }

    private fun toDisplayName(value: String): String {
        return value
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }
    }

    private data class IngredientEvaluation(
        val scoreDelta: Int,
        val concernReasons: List<String>,
        val benefitReasons: List<String>,
        val neutralReasons: List<String>,
        val hasStrongNegative: Boolean
    )

    private fun List<String>.toNaturalList(): String {
        return when (size) {
            0 -> ""
            1 -> this[0]
            2 -> "${this[0]} and ${this[1]}"
            else -> "${dropLast(1).joinToString(", ")}, and ${last()}"
        }
    }

    companion object {
        private val nutrientKeywords = setOf(
            "calcium",
            "magnesium",
            "zinc",
            "iron",
            "vitamin",
            "folic",
            "niacin"
        )

        private val additiveCategories = setOf(
            "additive",
            "acid regulator",
            "stabilizer",
            "emulsifier",
            "color",
            "flavor enhancer",
            "preservative",
            "anti-caking",
            "raising agent",
            "antioxidant"
        )

        private val additiveTags = setOf(
            "additive",
            "stabilizer",
            "emulsifier"
        )
    }
}
