package com.labeliq.app.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.labeliq.app.domain.model.Ingredient
import com.labeliq.app.domain.model.IngredientInfo
import com.labeliq.app.domain.usecase.cleanText
import com.labeliq.app.domain.usecase.findIngredient
import com.labeliq.app.domain.usecase.findIngredientMatchDetail
import com.labeliq.app.domain.usecase.preprocessAliasLookup
import com.labeliq.app.domain.usecase.splitIngredients

class IngredientRepository private constructor(
    context: Context
) {
    private val ingredientMap: Map<String, Ingredient> = loadIngredients(context.applicationContext)

    init {
        preprocessAliasLookup(ingredientMap)
    }

    fun findIngredient(name: String): IngredientInfo? {
        return findIngredient(name, ingredientMap)
    }

    fun getIngredientInfo(name: String): IngredientInfo? = findIngredient(name)

    fun findIngredientMatch(name: String): IngredientMatch? {
        val detail = findIngredientMatchDetail(name, ingredientMap) ?: return null
        val confidence = when (detail.matchedBy) {
            "exact", "alias" -> "HIGH"
            else -> "MEDIUM"
        }

        return IngredientMatch(
            ingredient = detail.ingredient,
            confidence = confidence,
            matchedBy = detail.matchedBy
        )
    }

    fun processIngredients(rawText: String): List<Ingredient> {
        val ingredientSection = extractIngredientSection(rawText)
        val cleanedSection = cleanText(ingredientSection)
        if (cleanedSection.isBlank()) return emptyList()

        val splitCandidates = splitIngredients(ingredientSection)
            .ifEmpty { splitIngredients(cleanedSection) }

        return splitCandidates
            .asSequence()
            .map(::cleanText)
            .filter { it.isNotBlank() }
            .map { token -> findIngredient(token, ingredientMap) }
            .filterNotNull()
            .distinctBy { it.name }
            .toList()
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

    companion object {
        private const val TAG = "IngredientRepository"

        @Volatile
        private var instance: IngredientRepository? = null

        fun getInstance(context: Context): IngredientRepository {
            return instance ?: synchronized(this) {
                instance ?: IngredientRepository(context).also { instance = it }
            }
        }
    }

    data class IngredientMatch(
        val ingredient: IngredientInfo,
        val confidence: String,
        val matchedBy: String
    )
}

fun loadIngredients(context: Context): Map<String, Ingredient> {
    return try {
        val json = context.assets.open(INGREDIENTS_FILE).bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, Ingredient>>() {}.type
        val rawMap: Map<String, Ingredient> = Gson().fromJson<Map<String, Ingredient>>(json, type).orEmpty()

        val normalizedMap = LinkedHashMap<String, Ingredient>()

        rawMap.forEach { (rawKey, rawIngredient) ->
            val canonicalName = cleanText(rawIngredient.name.ifBlank { rawKey })
            if (canonicalName.isBlank()) return@forEach

            val aliases = (rawIngredient.aliases + rawIngredient.name + rawKey)
                .asSequence()
                .flatMap { alias -> splitIngredients(alias).asSequence() }
                .map(::cleanText)
                .filter { it.isNotBlank() && it != canonicalName }
                .distinct()
                .toList()

            val normalizedIngredient = Ingredient(
                name = canonicalName,
                aliases = aliases,
                category = rawIngredient.category.trim().ifBlank { "unknown" },
                tags = rawIngredient.tags.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                badFor = rawIngredient.badFor.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                goodFor = rawIngredient.goodFor.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                description = rawIngredient.description.trim()
            )

            normalizedMap[canonicalName] = normalizedIngredient
        }

        Log.d(TAG, "Loaded ${normalizedMap.size} ingredients from assets.")
        normalizedMap
    } catch (error: Exception) {
        Log.e(TAG, "Failed to load ingredients from assets.", error)
        emptyMap()
    }
}

private const val TAG = "IngredientRepository"
private const val INGREDIENTS_FILE = "ingredients.json"
