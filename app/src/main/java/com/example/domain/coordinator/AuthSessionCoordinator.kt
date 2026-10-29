package com.example.domain.coordinator

import android.util.Log
import com.example.domain.model.AuthState
import com.example.domain.model.StudentProfile
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Authentication navigation destinations resolved by the Central Auth Coordinator.
 */
sealed interface AuthDestination {
    data object Splash : AuthDestination
    data object Welcome : AuthDestination
    data object Login : AuthDestination
    data object SignUp : AuthDestination
    data class OtpVerification(val email: String) : AuthDestination
    data class CompleteProfile(val profile: StudentProfile) : AuthDestination
    data class Dashboard(val profile: StudentProfile) : AuthDestination
}

/**
 * Central Auth Coordinator for CGA (Career Guidance Application).
 *
 * Implements the mandatory single authentication/session pipeline:
 *
 *                CGA AUTH SYSTEM
 *                       |
 *    +------------------+------------------+
 *    |                  |                  |
 *    v                  v                  v
 * Email + Password  Email OTP       Google OAuth
 *    |                  |                  |
 *    |                  |           Android Browser
 *    |                  |                  |
 *    |                  |            Google Login
 *    |                  |                  |
 *    |                  |           OAuth Callback
 *    |                  |                  |
 *    +------------------+------------------+
 *                       |
 *                       v
 *                 Supabase Auth
 *                       |
 *                       v
 *                Authenticated Session
 *                       |
 *                       v
 *              Central Auth Coordinator
 *                       |
 *                       v
 *                Load User Profile
 *                       |
 *                +------+------+
 *                |             |
 *                v             v
 *            Complete     Incomplete
 *                |             |
 *                v             v
 *            Dashboard   Complete Profile
 *
 * Responsibilities:
 * 1. Observes the continuous Supabase session state.
 * 2. Determines whether the user is authenticated.
 * 3. Resolves profile completeness using authenticated Supabase identity.
 * 4. Determines the one true destination screen across all auth methods.
 */
class AuthSessionCoordinator(
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "AuthCoordinator"
    }

    private fun log(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Throwable) {
            // Safe fallback for local JVM unit tests
        }
    }

    val authState: Flow<AuthState> = authRepository.authState

    /**
     * Resolves the target destination for any given AuthState.
     */
    fun resolveDestination(state: AuthState): AuthDestination {
        return when (state) {
            is AuthState.Initializing -> AuthDestination.Splash
            is AuthState.SignedOut -> AuthDestination.Welcome
            is AuthState.VerificationRequired -> AuthDestination.OtpVerification(state.email)
            is AuthState.SigningUp, is AuthState.Authenticating -> AuthDestination.Splash
            is AuthState.EmailVerified -> {
                if (state.profile.isProfileComplete) {
                    AuthDestination.Dashboard(state.profile)
                } else {
                    AuthDestination.CompleteProfile(state.profile)
                }
            }
            is AuthState.Authenticated -> {
                if (state.profile.isProfileComplete) {
                    AuthDestination.Dashboard(state.profile)
                } else {
                    AuthDestination.CompleteProfile(state.profile)
                }
            }
            is AuthState.ProfileComplete -> AuthDestination.Dashboard(state.profile)
            is AuthState.ProfileIncomplete -> AuthDestination.CompleteProfile(state.profile)
            is AuthState.AuthError -> AuthDestination.Welcome
        }
    }

    /**
     * Computes screen routing dynamically when auth state changes.
     * Guarantees consistent post-authentication routing for:
     * - Email/Password login
     * - Email OTP verification
     * - Google OAuth
     * - Session restoration on app launch
     */
    fun computeScreenTransition(
        currentScreen: String,
        state: AuthState,
        isEmailVerifiedAnimationActive: Boolean
    ): String? {
        log("Evaluating transition: currentScreen=$currentScreen, state=${state::class.simpleName}")

        // Do not interrupt the splash screen during its initial animation window
        if (currentScreen == "splash") {
            return null
        }

        return when (state) {
            is AuthState.ProfileComplete -> {
                if (currentScreen == "welcome" || currentScreen == "login" || currentScreen == "complete_profile") {
                    "dashboard"
                } else if (currentScreen == "sign_up" && !isEmailVerifiedAnimationActive) {
                    "dashboard"
                } else {
                    null
                }
            }
            is AuthState.ProfileIncomplete -> {
                if (currentScreen == "welcome" || currentScreen == "login") {
                    "complete_profile"
                } else if (currentScreen == "sign_up" && !isEmailVerifiedAnimationActive) {
                    "complete_profile"
                } else {
                    null
                }
            }
            is AuthState.SignedOut -> {
                if (currentScreen != "welcome" && currentScreen != "login" && currentScreen != "sign_up") {
                    "welcome"
                } else {
                    null
                }
            }
            is AuthState.VerificationRequired -> {
                if (currentScreen != "sign_up") {
                    "sign_up"
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
