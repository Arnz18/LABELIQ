package com.labeliq.app.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.labeliq.app.domain.model.User
import java.util.UUID

// ── Constants ─────────────────────────────────────────────────────────────────
private const val PREFS_USERS        = "user_store"
private const val KEY_USERS_JSON     = "users"
private const val KEY_CURRENT_USER   = "current_user_id"

private val userGson = Gson()

// ── Save a new / updated user ──────────────────────────────────────────────────
/**
 * Persists [user] into the users list.
 * If a user with the same [User.id] already exists, it is replaced.
 */
fun saveUser(context: Context, user: User) {
    val all = getAllUsers(context).toMutableList()
    val idx = all.indexOfFirst { it.id == user.id }
    if (idx >= 0) all[idx] = user else all.add(user)

    context.getSharedPreferences(PREFS_USERS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_USERS_JSON, userGson.toJson(all))
        .apply()
}

// ── Register helper ────────────────────────────────────────────────────────────
/**
 * Convenience function: creates a new [User] with a generated UUID and saves it.
 *
 * @return the newly created [User], or `null` if the email is already taken.
 */
fun createUser(
    context: Context,
    name: String,
    email: String,
    password: String,
    isDiabetic: Boolean = false,
    isVegan: Boolean = false,
    hasNutAllergy: Boolean = false
): User? {
    if (getUserByEmail(context, email) != null) return null   // email taken

    val user = User(
        id           = UUID.randomUUID().toString(),
        name         = name.trim(),
        email        = email.trim().lowercase(),
        password     = password,
        isDiabetic   = isDiabetic,
        isVegan      = isVegan,
        hasNutAllergy = hasNutAllergy
    )
    saveUser(context, user)
    return user
}

// ── Retrieve all users ─────────────────────────────────────────────────────────
fun getAllUsers(context: Context): List<User> {
    val json = context.getSharedPreferences(PREFS_USERS, Context.MODE_PRIVATE)
        .getString(KEY_USERS_JSON, null)
        ?: return emptyList()

    val type = object : TypeToken<List<User>>() {}.type
    return runCatching { userGson.fromJson<List<User>>(json, type) ?: emptyList() }
        .getOrDefault(emptyList())
}

// ── Lookup by email ────────────────────────────────────────────────────────────
fun getUserByEmail(context: Context, email: String): User? =
    getAllUsers(context).firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }

// ── Session management ─────────────────────────────────────────────────────────
/**
 * Marks [userId] as the active session.
 * Call this after a successful login / registration.
 */
fun setCurrentUser(context: Context, userId: String) {
    context.getSharedPreferences(PREFS_USERS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CURRENT_USER, userId)
        .apply()
}

/**
 * Returns the ID of the currently logged-in user, or `null` if no session is active.
 */
fun getCurrentUserId(context: Context): String? =
    context.getSharedPreferences(PREFS_USERS, Context.MODE_PRIVATE)
        .getString(KEY_CURRENT_USER, null)

/**
 * Returns the full [User] object for the active session, or `null` if not logged in.
 */
fun getCurrentUser(context: Context): User? {
    val id = getCurrentUserId(context) ?: return null
    return getAllUsers(context).firstOrNull { it.id == id }
}

// ── Logout ─────────────────────────────────────────────────────────────────────
/**
 * Clears the active session. The user list is NOT affected.
 */
fun logoutUser(context: Context) {
    context.getSharedPreferences(PREFS_USERS, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_CURRENT_USER)
        .apply()
}

// ── Auth helpers (Boolean API) ─────────────────────────────────────────────────
/**
 * Registers a new user and sets them as the current session.
 *
 * @return `true` if registration succeeded; `false` if [email] is already taken.
 */
fun registerAndLoginUser(
    context: Context,
    name: String,
    email: String,
    password: String,
    isDiabetic: Boolean,
    isVegan: Boolean,
    hasNutAllergy: Boolean
): Boolean {
    if (getUserByEmail(context, email) != null) return false   // email already exists

    val user = User(
        id            = UUID.randomUUID().toString(),
        name          = name.trim(),
        email         = email.trim().lowercase(),
        password      = password,
        isDiabetic    = isDiabetic,
        isVegan       = isVegan,
        hasNutAllergy = hasNutAllergy
    )
    saveUser(context, user)
    setCurrentUser(context, user.id)
    return true
}

/**
 * Validates credentials and starts a session for the matching user.
 *
 * @return `true` if [email] and [password] match a stored user; `false` otherwise.
 */
fun loginUser(
    context: Context,
    email: String,
    password: String
): Boolean {
    val user = getUserByEmail(context, email) ?: return false
    if (user.password != password) return false
    setCurrentUser(context, user.id)
    return true
}

// ── Update an existing user ────────────────────────────────────────────────────
/**
 * Finds the user matching [updatedUser.id] in the persisted list, replaces it,
 * and writes the list back to SharedPreferences.
 *
 * If no user with that id exists the updated user is appended (same as [saveUser]).
 */
fun updateUser(context: Context, updatedUser: User) {
    val all = getAllUsers(context).toMutableList()
    val idx = all.indexOfFirst { it.id == updatedUser.id }
    if (idx >= 0) all[idx] = updatedUser else all.add(updatedUser)

    context.getSharedPreferences(PREFS_USERS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_USERS_JSON, userGson.toJson(all))
        .apply()
}
