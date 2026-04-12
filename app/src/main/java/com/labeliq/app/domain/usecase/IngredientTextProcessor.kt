package com.labeliq.app.domain.usecase

import com.labeliq.app.data.repository.IngredientRepository
import com.labeliq.app.domain.model.Ingredient

class IngredientTextProcessor(
    private val ingredientRepository: IngredientRepository
) {
    fun extractIngredients(ocrText: String): List<String> {
        val ingredientSection = extractIngredientSection(ocrText)
        val cleanedSection = cleanText(ingredientSection)
        val splitCandidates = splitIngredients(ingredientSection)
            .ifEmpty { splitIngredients(cleanedSection) }

        return splitCandidates
            .map(::cleanText)
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun processIngredients(rawText: String): List<Ingredient> {
        return ingredientRepository.processIngredients(rawText)
    }

    private fun extractIngredientSection(rawText: String): String {
        if (rawText.isBlank()) return ""

        val normalizedText = rawText.replace("\r", "\n")
        val ingredientStartRegex = Regex("(?i)ingredients?\\s*[:\\-]?")
        val startMatch = ingredientStartRegex.find(normalizedText)
        val fromIngredientBlock = if (startMatch != null) {
            normalizedText.substring(startMatch.range.last + 1)
        } else {
            normalizedText
        }

        val stopRegex = Regex(
            "(?i)\\b(allergen|allergy|nutrition|nutritional|fssai|manufacturer|marketed|storage|serving|net quantity|best before|instructions|usage|directions?)\\b"
        )
        val stopMatch = stopRegex.find(fromIngredientBlock)

        return if (stopMatch != null) {
            fromIngredientBlock.substring(0, stopMatch.range.first).trim()
        } else {
            fromIngredientBlock.trim()
        }
    }
}
