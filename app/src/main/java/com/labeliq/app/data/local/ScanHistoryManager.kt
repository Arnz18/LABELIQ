package com.labeliq.app.data.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ── Data class ────────────────────────────────────────────────────────────────
data class ScanResult(
    val status: String,
    val summary: String,
    val highRisk: List<String>,
    val moderate: List<String>,
    val safe: List<String>,
    val score: Int = 0,
    val advice: String = "",
    val timestamp: Long,
    val imagePath: String = ""
)

// ── Constants ─────────────────────────────────────────────────────────────────
private const val PREFS_FILE   = "scan_history"
private const val KEY_HISTORY  = "history_json"
private const val MAX_HISTORY  = 20

private val gson = Gson()

// ── Save ──────────────────────────────────────────────────────────────────────
/**
 * Prepends [result] to the stored list and trims to the last [MAX_HISTORY] items.
 */
fun saveScanResult(context: Context, result: ScanResult) {
    val existing = loadScanHistory(context).toMutableList()

    existing.add(0, result)                           // newest first
    val trimmed = if (existing.size > MAX_HISTORY)
        existing.subList(0, MAX_HISTORY)
    else existing

    val json = gson.toJson(trimmed)
    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_HISTORY, json)
        .apply()

    Log.d("SAVE", "Saved scan at ${result.timestamp}")
}

// ── Load ──────────────────────────────────────────────────────────────────────
/**
 * Returns the stored scan history (newest first), or an empty list if none exists.
 */
fun loadScanHistory(context: Context): List<ScanResult> {
    val json = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        .getString(KEY_HISTORY, null)
        ?: return emptyList()

    val type = object : TypeToken<List<ScanResult>>() {}.type
    return try {
        gson.fromJson(json, type) ?: emptyList()
    } catch (e: Exception) {
        Log.e("HISTORY", "Failed to parse scan history: ${e.message}")
        emptyList()
    }
}
