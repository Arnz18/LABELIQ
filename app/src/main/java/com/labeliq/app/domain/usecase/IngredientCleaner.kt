package com.labeliq.app.domain.usecase

import java.util.Locale

fun cleanText(input: String): String {
    return input
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun cleanIngredients(raw: List<String>): List<String> {
    return raw
        .asSequence()
        .flatMap { splitIngredients(it).asSequence() }
        .map(::cleanText)
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}
