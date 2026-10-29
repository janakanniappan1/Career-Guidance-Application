package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Android SharedPreferences implementation of Supabase's SessionManager.
 * Persists the user's active session, access tokens, and refresh tokens across app restarts.
 */
class AndroidPreferencesSessionManager(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
) : SessionManager {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun saveSession(session: UserSession) {
        try {
            val serialized = json.encodeToString(session)
            prefs.edit().putString(KEY_SESSION, serialized).apply()
            Log.d(TAG, "Supabase session saved successfully for user: ${session.user?.id}")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to persist Supabase session", e)
        }
    }

    override suspend fun loadSession(): UserSession? {
        val serialized = try {
            prefs.getString(KEY_SESSION, null)
        } catch (e: Throwable) {
            Log.e(TAG, "Error accessing SharedPreferences", e)
            null
        }

        if (serialized.isNullOrBlank()) {
            Log.d(TAG, "No stored Supabase session found in SharedPreferences")
            return null
        }

        return try {
            val session = json.decodeFromString<UserSession>(serialized)
            Log.d(TAG, "Loaded stored Supabase session for user: ${session.user?.id}")
            session
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to decode stored Supabase session, clearing invalid cache", e)
            deleteSession()
            null
        }
    }

    override suspend fun deleteSession() {
        try {
            prefs.edit().remove(KEY_SESSION).apply()
            Log.d(TAG, "Cleared stored Supabase session from SharedPreferences")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to clear session", e)
        }
    }

    companion object {
        private const val TAG = "CgaSessionManager"
        private const val PREFS_NAME = "cga_supabase_session_prefs"
        private const val KEY_SESSION = "cga_user_session_v1"
    }
}
