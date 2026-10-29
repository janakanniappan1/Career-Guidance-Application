package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Interceptor
import okhttp3.Response

import kotlin.time.Duration.Companion.seconds
import java.util.concurrent.TimeUnit

/**
 * OkHttp Interceptor that automatically mitigates distributed clock skew
 * when Supabase Auth generates a JWT slightly ahead of PostgREST's clock ("JWT issued at future").
 */
class JwtClockSkewInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response = chain.proceed(chain.request())
        if (response.code == 401) {
            val isFutureJwt = try {
                val peek = response.peekBody(1024).string()
                peek.contains("JWT issued at future", ignoreCase = true) ||
                peek.contains("issued at future", ignoreCase = true)
            } catch (_: Exception) {
                false
            }
            if (isFutureJwt) {
                Log.w("SupabaseClient", "Detected JWT clock skew ('JWT issued at future'). Retrying after brief delay...")
                response.close()
                try {
                    Thread.sleep(1000)
                } catch (_: InterruptedException) { }
                response = chain.proceed(chain.request())
                if (response.code == 401) {
                    val isStillFuture = try {
                        val peek = response.peekBody(1024).string()
                        peek.contains("issued at future", ignoreCase = true)
                    } catch (_: Exception) {
                        false
                    }
                    if (isStillFuture) {
                        Log.w("SupabaseClient", "JWT clock skew persisting. Retrying second time...")
                        response.close()
                        try {
                            Thread.sleep(1500)
                        } catch (_: InterruptedException) { }
                        response = chain.proceed(chain.request())
                    }
                }
            }
        }
        return response
    }
}

/**
 * Centralized Supabase Client Provider for CGA (Career Guidance Application).
 *
 * Security Rules:
 * - Uses only the public/client anon key.
 * - Never uses sb_secret or service_role credentials.
 * - Never includes database passwords.
 * - Does not print credentials to logs.
 * - Works via BuildConfig / .env with safe fallback to configuration constants.
 */
object SupabaseClientProvider {
    private const val TAG = "SupabaseClient"

    const val SUPABASE_URL = "https://kwyqbcufhxtkxkguzmup.supabase.co"
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_aeFS_IJ6YzL5B_2dPpjqTA_2H7sX-Ck"

    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
            Log.d(TAG, "Application context registered for Supabase session persistence")
        }
    }

    fun getAppContext(): Context? = appContext

    val effectiveUrl: String
        get() {
            val fromConfig = try { BuildConfig.SUPABASE_URL.trim() } catch (_: Throwable) { "" }
            return if (fromConfig.isNotBlank() &&
                fromConfig.startsWith("http") &&
                !fromConfig.contains("DEFAULT_SUPABASE_URL", ignoreCase = true)
            ) {
                fromConfig
            } else {
                SUPABASE_URL
            }
        }

    val effectivePublishableKey: String
        get() {
            val fromConfig = try { BuildConfig.SUPABASE_ANON_KEY.trim() } catch (_: Throwable) { "" }
            return if (fromConfig.isNotBlank() &&
                !fromConfig.contains("DEFAULT_SUPABASE_ANON_KEY", ignoreCase = true)
            ) {
                fromConfig
            } else {
                SUPABASE_PUBLISHABLE_KEY
            }
        }

    val client: SupabaseClient by lazy {
        Log.d(TAG, "Initializing Supabase client with host ${effectiveUrl.substringAfter("://").substringBefore("/")}")

        createSupabaseClient(
            supabaseUrl = effectiveUrl,
            supabaseKey = effectivePublishableKey
        ) {
            requestTimeout = 30.seconds
            httpEngine = OkHttp.create {
                config {
                    connectTimeout(30, TimeUnit.SECONDS)
                    readTimeout(30, TimeUnit.SECONDS)
                    writeTimeout(30, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                    addInterceptor(JwtClockSkewInterceptor())
                }
            }

            install(Auth) {
                sessionManager = appContext?.let { ctx ->
                    AndroidPreferencesSessionManager(ctx)
                } ?: MemorySessionManager()
                codeVerifierCache = appContext?.let { ctx ->
                    AndroidPreferencesCodeVerifierCache(ctx)
                } ?: MemoryCodeVerifierCache()
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
                autoSaveToStorage = true
                flowType = FlowType.PKCE
                scheme = "cga"
                host = "auth-callback"
                defaultRedirectUrl = "cga://auth-callback"
            }
            install(Postgrest) {
                defaultSchema = "public"
            }
            install(Realtime)
        }
    }
}
