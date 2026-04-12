package com.labeliq.app.domain.usecase

private val fillerWords = setOf(
    "refined",
    "edible",
    "natural",
    "added",
    "iodised",
    "iodized",
    "permitted"
)

fun normalize(input: String): String {
    return cleanText(input)
        .split(" ")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { it in fillerWords }
        .joinToString(" ")
        .trim()
}
