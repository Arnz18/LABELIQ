package com.labeliq.app.domain.model

/**
 * Represents a registered local user.
 *
 * [id]            — UUID generated at registration time.
 * [name]          — Display name chosen by the user.
 * [email]         — Unique identifier used for login lookup.
 * [password]      — Stored as plain-text locally (no server, no PII network transfer).
 * [isDiabetic]    — Drives high-glycemic ingredient warnings.
 * [isVegan]       — Drives animal/dairy ingredient warnings.
 * [hasNutAllergy] — Drives nut ingredient warnings.
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val isDiabetic: Boolean,
    val isVegan: Boolean,
    val hasNutAllergy: Boolean
)
