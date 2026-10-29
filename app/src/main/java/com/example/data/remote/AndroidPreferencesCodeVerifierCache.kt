package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.jan.supabase.auth.CodeVerifierCache

/**
 * Android SharedPreferences implementation of Supabase's CodeVerifierCache.
 * Ensures the OAuth PKCE code_verifier is preserved across browser switches
 * and process death during the Google OAuth login redirect flow.
 */
class AndroidPreferencesCodeVerifierCache(
    private val context: Context
) : CodeVerifierCache {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun saveCodeVerifier(codeVerifier: String) {
        try {
            prefs.edit().putString(KEY_VERIFIER, codeVerifier).apply()
            Log.d(TAG, "PKCE code verifier saved to SharedPreferences")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to save PKCE code verifier", e)
        }
    }

    override suspend fun loadCodeVerifier(): String? {
        return try {
            val verifier = prefs.getString(KEY_VERIFIER, null)
            Log.d(TAG, "PKCE code verifier loaded: ${verifier != null}")
            verifier
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load PKCE code verifier", e)
            null
        }
    }

    override suspend fun deleteCodeVerifier() {
        try {
            prefs.edit().remove(KEY_VERIFIER).apply()
            Log.d(TAG, "PKCE code verifier deleted from SharedPreferences")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to delete PKCE code verifier", e)
        }
    }

    companion object {
        private const val TAG = "CgaCodeVerifierCache"
        private const val PREFS_NAME = "cga_supabase_pkce_prefs"
        private const val KEY_VERIFIER = "cga_pkce_code_verifier_v1"
    }
}
