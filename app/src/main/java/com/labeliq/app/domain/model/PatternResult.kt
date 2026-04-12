package com.labeliq.app.domain.model

data class PatternResult(
    val category: String,
    val riskLevel: String,
    val explanation: String,
    val confidence: String
)
