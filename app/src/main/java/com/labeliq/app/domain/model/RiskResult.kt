package com.labeliq.app.domain.model

data class RiskResult(
    val highRisk: List<String>,
    val moderateRisk: List<String>,
    val safe: List<String>,
    val overallStatus: String
)
