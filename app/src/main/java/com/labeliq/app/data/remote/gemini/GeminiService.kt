package com.labeliq.app.data.remote.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {

    suspend fun generateVerdictText(apiKey: String, structuredData: String): String? {
        if (apiKey.isBlank()) return null

        return try {
            val prompt = """
You are a food analysis assistant.

Convert the following structured data into a short, natural explanation.

Rules:
- Do NOT change verdict
- Do NOT add new facts
- Keep it under 5 lines
- Avoid repetition

Data:
$structuredData
"""

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt)
                        )
                    )
                )
            )

            withContext(Dispatchers.IO) {
                val response = RetrofitClient.api.generateContent(apiKey, request)
                response.candidates
                    .firstOrNull()
                    ?.content
                    ?.parts
                    .orEmpty()
                    .mapNotNull { it.text?.trim() }
                    .firstOrNull { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }
}
