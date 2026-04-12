package com.labeliq.app.domain.usecase

private val bracketRegex = Regex("\\(([^)]*)\\)")
private val splitRegex = Regex("\\s*(?:,|&|\\band\\b|;)\\s*", RegexOption.IGNORE_CASE)
private val leadingNoiseRegex = Regex("^(?:ingredients?|contains?)\\b\\s*", RegexOption.IGNORE_CASE)

fun splitIngredients(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()

    val normalizedRaw = raw
        .replace('\n', ',')
        .replace('\r', ',')

    val parts = LinkedHashSet<String>()

    bracketRegex.findAll(normalizedRaw).forEach { match ->
        splitFragment(match.groupValues[1]).forEach(parts::add)
    }

    val outsideText = bracketRegex.replace(normalizedRaw, ",")
    splitFragment(outsideText).forEach(parts::add)

    return parts.toList()
}

fun splitIngredient(input: String): List<String> = splitIngredients(input)

private fun splitFragment(input: String): List<String> {
    return input
        .split(splitRegex)
        .asSequence()
        .map { fragment -> leadingNoiseRegex.replace(fragment.trim(), "") }
        .map(::cleanText)
        .filter { it.isNotBlank() }
        .toList()
}
