package com.labeliq.app.domain.model

data class Ingredient(
    val name: String,
    val aliases: List<String> = emptyList(),
    val category: String = "unknown",
    val tags: List<String> = emptyList(),
    val badFor: List<String> = emptyList(),
    val goodFor: List<String> = emptyList(),
    val description: String = ""
)
