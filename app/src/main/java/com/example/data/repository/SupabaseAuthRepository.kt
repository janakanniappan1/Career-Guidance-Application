package com.example.data.repository

import android.util.Log
import com.example.data.model.ProfileDto
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.AuthState
import com.example.domain.model.SignUpResult
import com.example.domain.model.StudentProfile
import com.example.domain.model.UnverifiedEmailException
import com.example.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Production-ready implementation of AuthRepository backed by Supabase Auth and PostgREST.
 *
 * Security guarantees:
 * - Uses anon/publishable key only.
 * - Profile operations enforce auth.users.id alignment matching auth.uid() in PostgreSQL RLS.
 * - Never logs credentials, tokens, or secret keys.
 * - Adheres strictly to native Android cga://auth-callback deep link flow.
 */
class SupabaseAuthRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthRepository {

    private val _cachedProfile = MutableStateFlow<StudentProfile?>(null)
    private val profileLock = Mutex()

    companion object {
        private const val TAG = "SupabaseAuthRepo"
    }

    /**
     * Determines if a Supabase user account has completed email verification.
     * Google OAuth accounts are automatically verified by Google Identity provider.
     */
    private fun isUserEmailVerified(user: UserInfo?): Boolean {
        if (user == null) return false
        // 1. Supabase explicit confirmation timestamp
        if (user.emailConfirmedAt != null || user.confirmedAt != null) {
            return true
        }
        // 2. OAuth provider accounts (e.g. Google OAuth)
        val isOAuthProvider = user.identities?.any {
            it.provider.equals("google", ignoreCase = true)
        } == true || user.appMetadata?.get("provider")?.toString()?.contains("google", ignoreCase = true) == true
        return isOAuthProvider
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val authState: Flow<AuthState> = client.auth.sessionStatus
        .flatMapLatest { status: SessionStatus ->
            when (status) {
                is SessionStatus.Authenticated -> flow<AuthState> {
                    val user = status.session.user
                    val userId = user?.id
                    if (userId != null) {
                        // CRITICAL: Block unverified users from reaching authenticated states
                        val isConfirmed = isUserEmailVerified(user)
                        if (!isConfirmed) {
                            val email = user.email ?: ""
                            _cachedProfile.value = null
                            Log.d(TAG, "Unverified session detected in authState for $email; requiring OTP verification")
                            emit(AuthState.VerificationRequired(email, "Please verify your email before accessing CGA."))
                            return@flow
                        }

                        // Protect user isolation: clear stale cache if user ID changed
                        if (_cachedProfile.value != null && _cachedProfile.value?.id != userId) {
                            _cachedProfile.value = null
                        }

                        if (_cachedProfile.value == null) {
                            try {
                                val profile = loadOrCreateProfile(
                                    userId = userId,
                                    email = user.email ?: "",
                                    metadata = user.userMetadata
                                )
                                _cachedProfile.value = profile
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                Log.e(TAG, "Failed to load profile for $userId during session restoration", e)
                                val fallback = buildFallbackProfile(userId, user.email ?: "", user.userMetadata)
                                _cachedProfile.value = fallback
                            }
                        }

                        emitAll(_cachedProfile.map { profile ->
                            if (profile != null && profile.id == userId) {
                                if (profile.isProfileComplete) {
                                    AuthState.ProfileComplete(profile)
                                } else {
                                    AuthState.ProfileIncomplete(profile)
                                }
                            } else {
                                AuthState.SignedOut
                            }
                        })
                    } else {
                        _cachedProfile.value = null
                        emit(AuthState.SignedOut)
                    }
                }
                is SessionStatus.NotAuthenticated -> {
                    _cachedProfile.value = null
                    flowOf<AuthState>(AuthState.SignedOut)
                }
                is SessionStatus.Initializing -> {
                    flowOf<AuthState>(AuthState.Initializing)
                }
                else -> {
                    _cachedProfile.map { profile ->
                        if (profile != null) {
                            if (profile.isProfileComplete) {
                                AuthState.ProfileComplete(profile)
                            } else {
                                AuthState.ProfileIncomplete(profile)
                            }
                        } else {
                            AuthState.SignedOut
                        }
                    }
                }
            }
        }
        .flowOn(ioDispatcher)

    override suspend fun login(email: String, password: String): Result<StudentProfile> = withContext(ioDispatcher) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your email address."))
        }
        if (password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your password."))
        }

        try {
            client.auth.signInWith(Email) {
                this.email = cleanEmail
                this.password = password
            }

            val user = client.auth.currentUserOrNull()
                ?: return@withContext Result.failure(Exception("Incorrect email or password."))

            // CRITICAL: Block unverified account from logging in or accessing Home
            val isVerified = isUserEmailVerified(user)
            if (!isVerified) {
                try {
                    client.auth.signOut()
                } catch (e: Exception) {
                    Log.w(TAG, "Sign out after unverified login attempt: ${e.message}")
                }
                _cachedProfile.value = null
                Log.w(TAG, "Login rejected for unverified account: $cleanEmail")
                return@withContext Result.failure(
                    UnverifiedEmailException(
                        email = cleanEmail,
                        message = "Please verify your email address. Enter the 8-digit verification code sent to your email."
                    )
                )
            }

            val profile = loadOrCreateProfile(user.id, cleanEmail, user.userMetadata)
            _cachedProfile.value = profile
            Result.success(profile)
        } catch (e: Throwable) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("email not confirmed") || msg.contains("confirm your email") || msg.contains("email_not_confirmed")) {
                try {
                    client.auth.signOut()
                } catch (_: Exception) {}
                _cachedProfile.value = null
                Log.w(TAG, "Login caught unconfirmed email error from GoTrue for $cleanEmail")
                return@withContext Result.failure(
                    UnverifiedEmailException(
                        email = cleanEmail,
                        message = "Please verify your email address. Enter the 8-digit verification code sent to your email."
                    )
                )
            }
            Log.e(TAG, "Login failed for $cleanEmail: ${e.javaClass.simpleName}", e)
            Result.failure(mapAuthError(e, "Incorrect email or password."))
        }
    }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
        department: String,
        year: String,
        careerInterest: String
    ): Result<SignUpResult> = withContext(ioDispatcher) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanDept = department.trim()
        val cleanYear = year.trim()
        val cleanInterest = careerInterest.trim().ifBlank { "Technology" }

        // Field validations strictly prior to remote Supabase submission
        if (cleanName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (cleanDept.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter or select your department."))
        }
        if (cleanYear.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your year of study."))
        }

        try {
            val userMetadata = buildJsonObject {
                put("name", cleanName)
                put("full_name", cleanName)
                put("department", cleanDept)
                put("year", cleanYear)
                put("year_of_study", cleanYear)
                put("careerInterest", cleanInterest)
                put("career_interest", cleanInterest)
            }

            client.auth.signUpWith(
                provider = Email
            ) {
                this.email = cleanEmail
                this.password = password
                this.data = userMetadata
            }

            val currentSession = client.auth.currentSessionOrNull()
            val user = currentSession?.user ?: client.auth.currentUserOrNull()
            val isVerified = user != null && isUserEmailVerified(user)

            if (!isVerified) {
                // Email confirmation OTP is required by Supabase Auth configuration.
                // Clear any local unverified session so user cannot bypass verification
                try {
                    client.auth.signOut()
                } catch (e: Exception) {
                    Log.w(TAG, "Sign out unverified session after signUp: ${e.message}")
                }
                _cachedProfile.value = null
                Log.d(TAG, "Account created with email verification pending for $cleanEmail")
                Result.success(
                    SignUpResult.VerificationRequired(
                        email = cleanEmail,
                        message = "Your CGA account has been created. An 8-digit verification code has been sent to your email."
                    )
                )
            } else {
                val userId = user.id
                val profile = loadOrCreateProfile(userId, cleanEmail, userMetadata)
                _cachedProfile.value = profile
                Result.success(SignUpResult.Success(profile))
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Signup failed for $cleanEmail: ${e.javaClass.simpleName}", e)
            Result.failure(mapAuthError(e, "Unable to create account."))
        }
    }

    override suspend fun sendEmailOtp(email: String): Result<Unit> = withContext(ioDispatcher) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }

        try {
            try {
                client.auth.resendEmail(
                    type = OtpType.Email.SIGNUP,
                    email = cleanEmail
                )
            } catch (e: Throwable) {
                Log.d(TAG, "Resend with OtpType.Email.SIGNUP failed, trying OtpType.Email.EMAIL: ${e.message}")
                client.auth.resendEmail(
                    type = OtpType.Email.EMAIL,
                    email = cleanEmail
                )
            }
            Log.d(TAG, "Successfully sent email OTP code to $cleanEmail")
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to send email OTP to $cleanEmail: ${e.javaClass.simpleName}", e)
            Result.failure(mapAuthError(e, "Unable to send verification code."))
        }
    }

    override suspend fun verifyEmailOtp(email: String, otp: String): Result<StudentProfile> = withContext(ioDispatcher) {
        val cleanEmail = email.trim()
        val cleanOtp = otp.trim()

        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your email address."))
        }
        if (cleanOtp.length != 8 || !cleanOtp.all { it.isDigit() }) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid 8-digit verification code."))
        }

        try {
            try {
                client.auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP,
                    email = cleanEmail,
                    token = cleanOtp
                )
            } catch (e: Throwable) {
                Log.d(TAG, "verifyEmailOtp with SIGNUP type failed, trying EMAIL type: ${e.message}")
                client.auth.verifyEmailOtp(
                    type = OtpType.Email.EMAIL,
                    email = cleanEmail,
                    token = cleanOtp
                )
            }

            var session = client.auth.currentSessionOrNull()
            if (session == null) {
                try {
                    client.auth.retrieveUserForCurrentSession(updateSession = true)
                    session = client.auth.currentSessionOrNull()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed retrieving user after OTP verification: ${e.message}")
                }
            }

            val user = session?.user ?: client.auth.currentUserOrNull()
            val userId = user?.id ?: return@withContext Result.failure(
                Exception("Verification succeeded, but session could not be established. Please try signing in.")
            )

            val profile = loadOrCreateProfile(
                userId = userId,
                email = user.email ?: cleanEmail,
                metadata = user.userMetadata
            )
            _cachedProfile.value = profile
            Result.success(profile)
        } catch (e: Throwable) {
            Log.e(TAG, "verifyEmailOtp failed for $cleanEmail: ${e.javaClass.simpleName}", e)
            Result.failure(mapAuthError(e, "Invalid or expired verification code."))
        }
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return sendEmailOtp(email)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getCurrentUser(): Flow<StudentProfile?> {
        return client.auth.sessionStatus
            .flatMapLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val userId = user?.id
                        if (userId != null && isUserEmailVerified(user)) {
                            flow {
                                // Protect user isolation
                                if (_cachedProfile.value != null && _cachedProfile.value?.id != userId) {
                                    _cachedProfile.value = null
                                }

                                if (_cachedProfile.value == null) {
                                    try {
                                        val profile = loadOrCreateProfile(
                                            userId = userId,
                                            email = user.email ?: "",
                                            metadata = user.userMetadata
                                        )
                                        _cachedProfile.value = profile
                                    } catch (e: Exception) {
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        Log.e(TAG, "getCurrentUser: Failed to load profile for $userId: ${e.message}", e)
                                        val fallback = buildFallbackProfile(userId, user.email ?: "", user.userMetadata)
                                        _cachedProfile.value = fallback
                                    }
                                }
                                emitAll(_cachedProfile.map { profile ->
                                    val emitted = if (profile?.id == userId) profile else null
                                    Log.d("AUTH_PROFILE_DEBUG", "AUTH PROFILE DEBUG:\nuserId = $userId\nprofileXp = ${emitted?.xp}")
                                    emitted
                                })
                            }
                        } else {
                            _cachedProfile.value = null
                            Log.d("AUTH_PROFILE_DEBUG", "AUTH PROFILE DEBUG:\nuserId = null\nprofileXp = null")
                            flowOf(null)
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _cachedProfile.value = null
                        Log.d("AUTH_PROFILE_DEBUG", "AUTH PROFILE DEBUG:\nuserId = null\nprofileXp = null")
                        flowOf(null)
                    }
                    is SessionStatus.Initializing -> {
                        flowOf(_cachedProfile.value)
                    }
                    else -> {
                        flowOf(_cachedProfile.value)
                    }
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun updateProfile(profile: StudentProfile) {
        withContext(ioDispatcher) {
            val currentUserId = client.auth.currentUserOrNull()?.id ?: profile.id
            if (profile.id != currentUserId && currentUserId.isNotBlank()) {
                Log.e(TAG, "Security check: Attempted update by $currentUserId on different profile ${profile.id}")
                return@withContext
            }

            val dto = ProfileDto.fromDomain(profile)
            try {
                try {
                    client.from("profiles").update(dto) {
                        filter { eq("id", profile.id) }
                    }
                } catch (_: Exception) {
                    client.from("profiles").upsert(dto)
                }
                _cachedProfile.value = profile
                Log.d("AUTH_PROFILE_DEBUG", "AUTH PROFILE DEBUG:\nuserId = ${profile.id}\nprofileXp = ${profile.xp}")
                Log.d(TAG, "Successfully persisted and emitted updated profile for ${profile.id} (XP: ${profile.xp})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update profile remotely in Supabase", e)
                _cachedProfile.value = profile
                Log.d("AUTH_PROFILE_DEBUG", "AUTH PROFILE DEBUG:\nuserId = ${profile.id}\nprofileXp = ${profile.xp}")
                throw mapAuthError(e, "Unable to update profile.")
            }
        }
    }

    override fun updateCachedXp(userId: String, newXp: Int) {
        val current = _cachedProfile.value
        if (current != null && current.id == userId) {
            _cachedProfile.value = current.copy(xp = newXp)
            Log.d("AUTH_PROFILE_DEBUG", "AUTH PROFILE CACHED XP UPDATED LOCALLY:\nuserId = $userId\nnewCumulativeXp = $newXp")
        } else if (current == null) {
            Log.d(TAG, "updateCachedXp: No cached profile available for $userId; skipping local cache injection to prevent fake profile creation")
        } else {
            Log.w(TAG, "updateCachedXp: Current cached user ID (${current.id}) does not match target userId ($userId); ignoring cross-user update")
        }
    }

    override fun updateCachedStreak(userId: String, newStreak: Int) {
        val current = _cachedProfile.value
        if (current != null && current.id == userId) {
            _cachedProfile.value = current.copy(currentStreak = newStreak)
            Log.d("AUTH_PROFILE_DEBUG", "AUTH PROFILE CACHED STREAK UPDATED LOCALLY:\nuserId = $userId\nnewStreak = $newStreak")
        } else if (current == null) {
            Log.d(TAG, "updateCachedStreak: No cached profile available for $userId; skipping local cache injection to prevent fake profile creation")
        } else {
            Log.w(TAG, "updateCachedStreak: Current cached user ID (${current.id}) does not match target userId ($userId); ignoring cross-user update")
        }
    }

    override suspend fun completeProfile(
        department: String,
        year: String,
        careerInterest: String,
        name: String?
    ): Result<StudentProfile> = withContext(ioDispatcher) {
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: return@withContext Result.failure(Exception("No active session found. Please sign in."))

        val cleanDept = department.trim()
        val cleanYear = year.trim()
        val cleanInterest = careerInterest.trim()
        val cleanName = name?.trim()

        if (cleanDept.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter or select your department."))
        }
        if (cleanYear.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your year of study."))
        }
        if (cleanInterest.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please specify your target career interest."))
        }

        try {
            val currentProfile = _cachedProfile.value ?: loadOrCreateProfile(
                userId = currentUserId,
                email = client.auth.currentUserOrNull()?.email ?: "",
                metadata = client.auth.currentUserOrNull()?.userMetadata
            )
            val updated = currentProfile.copy(
                name = if (!cleanName.isNullOrBlank()) cleanName else currentProfile.name,
                department = cleanDept,
                yearOfStudy = cleanYear,
                careerInterest = cleanInterest
            )
            val dto = ProfileDto.fromDomain(updated)
            client.from("profiles").upsert(dto)
            _cachedProfile.value = updated
            Result.success(updated)
        } catch (e: Throwable) {
            Log.e(TAG, "Complete profile failed for $currentUserId", e)
            Result.failure(mapAuthError(e, "Unable to save academic details."))
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(ioDispatcher) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your registered email address."))
        }

        try {
            client.auth.resetPasswordForEmail(cleanEmail, redirectUrl = "cga://auth-callback")
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Password reset failed for $cleanEmail: ${e.javaClass.simpleName}", e)
            Result.failure(mapAuthError(e, "Unable to send password reset email."))
        }
    }

    override suspend fun signInWithGoogle(): Result<Unit> = withContext(ioDispatcher) {
        try {
            client.auth.signInWith(
                provider = Google,
                redirectUrl = "cga://auth-callback"
            )
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Google OAuth initiation failed: ${e.javaClass.simpleName}", e)
            Result.failure(mapAuthError(e, "Google authentication failed."))
        }
    }

    override suspend fun logout() {
        withContext(ioDispatcher) {
            try {
                client.auth.signOut()
            } catch (e: Throwable) {
                Log.w(TAG, "Sign out error", e)
            } finally {
                _cachedProfile.value = null
            }
        }
    }

    override suspend fun refreshSession(): AuthState = withContext(ioDispatcher) {
        try {
            var session = client.auth.currentSessionOrNull()
            if (session != null) {
                try {
                    client.auth.retrieveUserForCurrentSession(updateSession = true)
                    session = client.auth.currentSessionOrNull()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to refresh user in refreshSession: ${e.message}")
                }
            }
            if (session == null) {
                _cachedProfile.value = null
                return@withContext AuthState.SignedOut
            }
            val user = session.user
            val userId = user?.id ?: return@withContext AuthState.SignedOut
            val isConfirmed = isUserEmailVerified(user)
            if (!isConfirmed) {
                val email = user.email ?: ""
                _cachedProfile.value = null
                return@withContext AuthState.VerificationRequired(email, "Please verify your email before accessing CGA.")
            }
            val profile = loadOrCreateProfile(
                userId = userId,
                email = user.email ?: "",
                metadata = user.userMetadata
            )
            _cachedProfile.value = profile
            if (profile.isProfileComplete) {
                AuthState.ProfileComplete(profile)
            } else {
                AuthState.ProfileIncomplete(profile)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed refreshing session", e)
            AuthState.SignedOut
        }
    }

    override suspend fun checkEmailVerification(email: String?, password: String?): Result<StudentProfile> = withContext(ioDispatcher) {
        try {
            var session = client.auth.currentSessionOrNull()
            if (session != null) {
                try {
                    client.auth.retrieveUserForCurrentSession(updateSession = true)
                    session = client.auth.currentSessionOrNull()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to refresh user for current session: ${e.message}")
                }
            }

            // If no active session on device but user credentials provided, attempt sign in
            if (session == null && !email.isNullOrBlank() && !password.isNullOrBlank()) {
                try {
                    client.auth.signInWith(Email) {
                        this.email = email.trim()
                        this.password = password
                    }
                    session = client.auth.currentSessionOrNull()
                } catch (e: Exception) {
                    Log.w(TAG, "Sign in during verification check failed: ${e.message}")
                    return@withContext Result.failure(
                        Exception("Email is not verified yet. Please verify your email and try again.")
                    )
                }
            }

            if (session == null) {
                return@withContext Result.failure(
                    Exception("Email is not verified yet. Please verify your email and try again.")
                )
            }

            val user = session.user
            val isConfirmed = isUserEmailVerified(user)
            if (!isConfirmed) {
                return@withContext Result.failure(
                    Exception("Email is not verified yet. Please verify your email and try again.")
                )
            }

            val userId = user?.id ?: return@withContext Result.failure(
                Exception("Email is not verified yet. Please verify your email and try again.")
            )

            val profile = loadOrCreateProfile(
                userId = userId,
                email = user.email ?: email ?: "",
                metadata = user.userMetadata
            )
            _cachedProfile.value = profile
            Result.success(profile)
        } catch (e: Throwable) {
            Log.w(TAG, "checkEmailVerification failed", e)
            Result.failure(
                Exception("Email is not verified yet. Please verify your email and try again.")
            )
        }
    }

    private fun buildFallbackProfile(
        userId: String,
        email: String,
        metadata: JsonObject?
    ): StudentProfile {
        val inferredName = (metadata?.get("name") as? JsonPrimitive)?.content
            ?: (metadata?.get("full_name") as? JsonPrimitive)?.content
            ?: (metadata?.get("user_name") as? JsonPrimitive)?.content
            ?: email.substringBefore("@").replace(".", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                .ifBlank { "Student" }

        val inferredDept = (metadata?.get("department") as? JsonPrimitive)?.content ?: ""
        val inferredYear = (metadata?.get("year_of_study") as? JsonPrimitive)?.content
            ?: (metadata?.get("year") as? JsonPrimitive)?.content ?: ""
        val inferredInterest = (metadata?.get("career_interest") as? JsonPrimitive)?.content
            ?: (metadata?.get("careerInterest") as? JsonPrimitive)?.content ?: ""

        return StudentProfile(
            id = userId,
            name = inferredName,
            email = email,
            department = inferredDept,
            yearOfStudy = inferredYear,
            careerInterest = inferredInterest,
            currentStreak = 0,
            isAssessmentCompleted = false,
            xp = 0
        )
    }

    private suspend fun loadOrCreateProfile(
        userId: String,
        email: String,
        metadata: JsonObject?
    ): StudentProfile = profileLock.withLock {
        // Return existing cached profile if already populated for this user
        val currentCache = _cachedProfile.value
        if (currentCache != null && currentCache.id == userId) {
            return@withLock currentCache
        }

        var list: List<ProfileDto>? = null
        var fetchError: Exception? = null

        // Query profiles table with resilient retries for clock skew or transient network errors
        for (attempt in 1..3) {
            try {
                list = client.from("profiles").select {
                    filter {
                        eq("id", userId)
                    }
                }.decodeList<ProfileDto>()
                break
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                fetchError = e
                val isClockSkew = e.message?.contains("future", ignoreCase = true) == true
                Log.w(TAG, "loadOrCreateProfile: select attempt $attempt failed: ${e.message}")
                if (attempt < 3 && isClockSkew) {
                    kotlinx.coroutines.delay(800L * attempt)
                } else if (attempt < 3) {
                    kotlinx.coroutines.delay(400L)
                }
            }
        }

        val existing = list?.firstOrNull()
        if (existing != null) {
            // CASE A: Row exists in Supabase -> return real profile from Supabase
            return@withLock existing.toDomain()
        }

        val fallback = buildFallbackProfile(userId, email, metadata)
        val newDto = ProfileDto.fromDomain(fallback)

        // Try upserting initial profile row if query succeeded but row did not exist yet
        if (list != null) {
            try {
                client.from("profiles").upsert(newDto)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Initial profile creation upsert notice: ${e.message}")
            }
        } else if (fetchError != null) {
            Log.w(TAG, "Profiles fetch encountered error (${fetchError.message}), safely falling back to user metadata profile.")
        }
        fallback
    }

    private fun mapAuthError(e: Throwable, fallback: String): Exception {
        val message = e.message?.lowercase() ?: ""
        val userMessage = when {
            message.contains("invalid login credentials") ||
            message.contains("invalid_credentials") ||
            message.contains("invalid credentials") ||
            message.contains("wrong password") -> "Incorrect email or password."

            message.contains("user already registered") ||
            message.contains("already registered") ||
            message.contains("email already in use") ||
            message.contains("already exists") -> "An account with this email already exists."

            message.contains("password should be at least") ||
            message.contains("password is too short") ||
            message.contains("password does not meet") -> "Password must be at least 6 characters."

            message.contains("email not confirmed") ||
            message.contains("confirm your email") -> "Please verify your email before signing in."

            message.contains("token has expired") ||
            message.contains("token is expired") ||
            message.contains("otp expired") -> "Verification code has expired. Please request a new code."

            (message.contains("token") && (message.contains("invalid") || message.contains("not found"))) ||
            message.contains("invalid otp") ||
            message.contains("incorrect code") -> "Invalid verification code. Please check the 8-digit code and try again."

            message.contains("network") ||
            message.contains("connect") ||
            message.contains("timeout") ||
            message.contains("unable to resolve host") ||
            e is java.net.SocketTimeoutException ||
            e is java.net.UnknownHostException ||
            e is java.io.IOException -> "Unable to connect. Please check your internet connection."

            message.contains("profile") -> "Unable to load profile. Please try again."

            message.contains("reset") -> "Password reset failed. Please check the email address."

            message.contains("cancel") || message.contains("cancelled") || message.contains("canceled") ->
                "Google sign-in was cancelled."

            message.contains("google") || message.contains("oauth") ->
                "Google authentication failed. Please try again."

            message.contains("error sending confirmation email") ||
            message.contains("error sending") ||
            (message.contains("unexpected_failure") && (message.contains("email") || message.contains("confirmation"))) ->
                "Unable to send verification email. The Supabase email provider encountered an error or reached its hourly limit. Please try again shortly or contact your admin to verify the custom SMTP configuration."

            message.contains("rate limit") || message.contains("too many requests") ->
                "Too many attempts. Please try again in a few minutes."

            else -> fallback
        }
        return Exception(userMessage, e)
    }
}
