package com.labeliq.app.data.remote.gemini

data class GeminiResponse(
    val candidates: List<Candidate> = emptyList()
)

data class Candidate(
    val content: Content? = null
)
