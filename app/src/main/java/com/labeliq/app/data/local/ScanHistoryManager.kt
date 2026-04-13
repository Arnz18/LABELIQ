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
    val imagePath: String = "",
    // Multi-user support — empty string for legacy records (treated as current user's).
    val userId: String = ""
)

// ── Constants ─────────────────────────────────────────────────────────────────
private const val PREFS_FILE   = "scan_history"
private const val KEY_HISTORY  = "history_json"
private const val MAX_HISTORY  = 20

private val gson = Gson()

// ── Save ──────────────────────────────────────────────────────────────────────
/**
 * Prepends [result] to the ALL-users history list (trimmed to [MAX_HISTORY] per user),
 * after stamping it with the currently logged-in user's ID.
 */
fun saveScanResult(context: Context, result: ScanResult) {
    // Stamp with current user — fall back to empty string if no session (legacy path).
    val uid = getCurrentUserId(context) ?: ""
    val stamped = result.copy(userId = uid)

    val allScans = loadAllScanHistory(context).toMutableList()
    allScans.add(0, stamped)                          // newest first

    // Trim overall list to avoid unbounded growth.
    val trimmed = if (allScans.size > MAX_HISTORY * 10)
        allScans.subList(0, MAX_HISTORY * 10)
    else allScans

    val json = gson.toJson(trimmed)
    context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_HISTORY, json)
        .apply()

    Log.d("SAVE", "Saved scan for user='$uid' at ${stamped.timestamp}")
}

// ── Load (all users — internal) ───────────────────────────────────────────────
/**
 * Returns the raw, unfiltered list of every scan across all users.
 * Prefer [loadScanHistory] for UI use — it scopes to the current user.
 */
fun loadAllScanHistory(context: Context): List<ScanResult> {
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

// ── Load (current user) ────────────────────────────────────────────────────────
/**
 * Returns scans belonging to the currently logged-in user (newest first).
 *
 * Migration safety: legacy records stored with [userId] == "" are treated as
 * owned by whoever is currently signed in, so pre-multi-user history is preserved.
 */
fun loadScanHistory(context: Context): List<ScanResult> {
    val uid = getCurrentUserId(context) ?: ""
    return loadAllScanHistory(context).filter { scan ->
        scan.userId == uid || scan.userId.isEmpty()
    }
}
