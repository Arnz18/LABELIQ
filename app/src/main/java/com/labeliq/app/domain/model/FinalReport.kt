package com.labeliq.app.domain.model

data class FinalReport(
    val concerns: List<String>,
    val benefits: List<String>,
    val neutral: List<String>,
    val unknowns: List<String>,
    val score: Int,
    val verdict: String,
    val advice: String
)
