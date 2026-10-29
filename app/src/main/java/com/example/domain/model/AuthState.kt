package com.example.domain.model

/**
 * Authentication state model for CGA (Career Guidance Application).
 * Formally distinguishes between all phases of user registration, verification,
 * session authentication, and profile completeness.
 */
sealed interface AuthState {
    /** Startup state: checking Supabase session persistence and restoring credentials. */
    data object Initializing : AuthState

    /** User is signed out. No active session. */
    data object SignedOut : AuthState

    /** Signup operation in progress. */
    data object SigningUp : AuthState

    /**
     * Supabase account has been created, but email confirmation is pending.
     * The user does NOT have an active session yet.
     */
    data class VerificationRequired(
        val email: String,
        val message: String = "Your CGA account has been created. Please verify your email to activate it."
    ) : AuthState

    /** Active authentication in progress (sign in, OAuth exchange, or session refresh). */
    data object Authenticating : AuthState

    /** Active Supabase session confirmed, profile loaded, but evaluation in progress. */
    data class Authenticated(val profile: StudentProfile) : AuthState

    /**
     * Supabase email verification has been confirmed.
     * Displays the animated success tick before continuing to the authenticated flow.
     */
    data class EmailVerified(val profile: StudentProfile) : AuthState

    /** Authenticated user whose required academic details (department, year of study) are incomplete. */
    data class ProfileIncomplete(val profile: StudentProfile) : AuthState

    /** Authenticated user with complete profile ready for Home / Dashboard. */
    data class ProfileComplete(val profile: StudentProfile) : AuthState

    /** Authentication failure or user-facing error state. */
    data class AuthError(val message: String) : AuthState
}

/**
 * Result outcome of a signup operation.
 * Distinguishes whether email verification is required or if an immediate active session was created.
 */
sealed interface SignUpResult {
    data class VerificationRequired(val email: String, val message: String) : SignUpResult
    data class Success(val profile: StudentProfile) : SignUpResult
}

/**
 * Exception thrown when a user attempts to sign in or access the application
 * with an account whose email has not been verified in Supabase Auth.
 */
class UnverifiedEmailException(
    val email: String,
    override val message: String = "Please verify your email address. Enter the 8-digit verification code sent to your email."
) : Exception(message)
