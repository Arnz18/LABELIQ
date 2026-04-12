package com.labeliq.app.domain.usecase

import android.util.Log
import com.labeliq.app.domain.model.Ingredient

private data class AliasEntry(
    val alias: String,
    val ingredient: Ingredient
)

internal data class IngredientMatchDetail(
    val ingredient: Ingredient,
    val matchedBy: String
)

private data class MatcherIndex(
    val normalizedKeyMap: Map<String, Ingredient>,
    val aliasLookup: Map<String, Ingredient>,
    val partialAliases: List<AliasEntry>
)

private data class MatcherCache(
    val mapIdentity: Int,
    val mapSize: Int,
    val index: MatcherIndex
)

private val matcherCacheLock = Any()
@Volatile
private var matcherCache: MatcherCache? = null

internal fun preprocessAliasLookup(map: Map<String, Ingredient>) {
    getMatcherIndex(map)
}

fun findIngredient(input: String, map: Map<String, Ingredient>): Ingredient? {
    return findIngredientMatchDetail(input, map)?.ingredient
}

internal fun findIngredientMatchDetail(
    input: String,
    map: Map<String, Ingredient>
): IngredientMatchDetail? {
    val normalizedInput = normalizeForMatch(input)
    if (normalizedInput.isBlank()) {
        Log.d(MATCH_TAG, "Input: $input → Match: null")
        return null
    }

    val index = getMatcherIndex(map)

    index.normalizedKeyMap[normalizedInput]?.let { ingredient ->
        Log.d(MATCH_TAG, "Input: $input → Match: ${ingredient.name}")
        return IngredientMatchDetail(ingredient, "exact")
    }

    index.aliasLookup[normalizedInput]?.let { ingredient ->
        Log.d(MATCH_TAG, "Input: $input → Match: ${ingredient.name}")
        return IngredientMatchDetail(ingredient, "alias")
    }

    val partialMatch = findPartialMatch(normalizedInput, index)
    if (partialMatch != null) {
        Log.d(MATCH_TAG, "Input: $input → Match: ${partialMatch.ingredient.name}")
        return partialMatch
    }

    val fallback = resolveGenericFallback(normalizedInput, index)
    Log.d(MATCH_TAG, "Input: $input → Match: ${fallback?.ingredient?.name}")
    return fallback
}

private fun getMatcherIndex(map: Map<String, Ingredient>): MatcherIndex {
    val identity = System.identityHashCode(map)
    val size = map.size
    val cached = matcherCache

    if (cached != null && cached.mapIdentity == identity && cached.mapSize == size) {
        return cached.index
    }

    synchronized(matcherCacheLock) {
        val latest = matcherCache
        if (latest != null && latest.mapIdentity == identity && latest.mapSize == size) {
            return latest.index
        }

        val built = buildMatcherIndex(map)
        matcherCache = MatcherCache(identity, size, built)
        return built
    }
}

private fun buildMatcherIndex(map: Map<String, Ingredient>): MatcherIndex {
    val normalizedKeyMap = LinkedHashMap<String, Ingredient>()
    val aliasLookup = LinkedHashMap<String, Ingredient>()
    val partialAliases = ArrayList<AliasEntry>()
    val seenPartialAliases = HashSet<String>()

    map.forEach { (key, rawIngredient) ->
        val canonicalName = normalizeForMatch(rawIngredient.name.ifBlank { key })
        if (canonicalName.isBlank()) return@forEach

        val normalizedAliases = rawIngredient.aliases
            .map(::normalizeForMatch)
            .filter { it.isNotBlank() && it != canonicalName }
            .distinct()

        val ingredient = rawIngredient.copy(
            name = canonicalName,
            aliases = normalizedAliases
        )

        val keyCandidate = normalizeForMatch(key)
        if (keyCandidate.isNotBlank()) {
            normalizedKeyMap.putIfAbsent(keyCandidate, ingredient)
        }
        normalizedKeyMap.putIfAbsent(canonicalName, ingredient)

        val lookupTerms = buildList {
            add(canonicalName)
            if (keyCandidate.isNotBlank()) add(keyCandidate)
            addAll(normalizedAliases)
        }

        lookupTerms.forEach { term ->
            aliasLookup.putIfAbsent(term, ingredient)
            if (term.length >= 3 && seenPartialAliases.add(term)) {
                partialAliases += AliasEntry(term, ingredient)
            }
        }
    }

    partialAliases.sortByDescending { it.alias.length }

    return MatcherIndex(
        normalizedKeyMap = normalizedKeyMap,
        aliasLookup = aliasLookup,
        partialAliases = partialAliases
    )
}

private fun findPartialMatch(
    normalizedInput: String,
    index: MatcherIndex
): IngredientMatchDetail? {
    val candidates = buildList {
        add(normalizedInput)
        addAll(splitIngredients(normalizedInput).map(::normalizeForMatch))
    }.filter { it.length >= 3 }
        .distinct()

    candidates.forEach { candidate ->
        index.partialAliases.firstOrNull { entry ->
            candidate.contains(entry.alias) || entry.alias.contains(candidate)
        }?.let { match ->
            return IngredientMatchDetail(match.ingredient, "partial")
        }
    }

    return null
}

private fun resolveGenericFallback(
    normalizedInput: String,
    index: MatcherIndex
): IngredientMatchDetail? {
    val matchedIngredient = when {
        "spice" in normalizedInput -> findByNameOrAlias("spices", index) ?: defaultSpices
        "oil" in normalizedInput -> findByNameOrAlias("vegetable oil", index) ?: defaultVegetableOil
        "salt" in normalizedInput -> findByNameOrAlias("salt", index) ?: defaultSalt
        "flavour" in normalizedInput || "flavor" in normalizedInput ->
            findByNameOrAlias("flavouring", index)
                ?: findByNameOrAlias("flavoring", index)
                ?: defaultFlavouring

        else -> null
    }

    return matchedIngredient?.let { IngredientMatchDetail(it, "generic") }
}

private fun findByNameOrAlias(value: String, index: MatcherIndex): Ingredient? {
    val normalized = normalizeForMatch(value)
    return index.normalizedKeyMap[normalized] ?: index.aliasLookup[normalized]
}

private fun normalizeForMatch(value: String): String {
    val cleaned = normalize(value)
    if (cleaned.isBlank()) return ""

    return cleaned
        .replace(Regex("^(?:ingredients?|contains?)\\s+"), "")
        .trim()
}

private const val MATCH_TAG = "MATCH"

private val defaultSpices = Ingredient(
    name = "spices",
    aliases = listOf("spice", "condiments"),
    category = "spices",
    description = "Generic spices fallback"
)

private val defaultVegetableOil = Ingredient(
    name = "vegetable oil",
    aliases = listOf("edible oil"),
    category = "oil",
    description = "Generic vegetable oil fallback"
)

private val defaultSalt = Ingredient(
    name = "salt",
    aliases = listOf("iodised salt", "iodized salt"),
    category = "mineral",
    description = "Generic salt fallback"
)

private val defaultFlavouring = Ingredient(
    name = "flavouring",
    aliases = listOf("flavoring", "flavour"),
    category = "flavoring",
    description = "Generic flavouring fallback"
)
