package com.example.data.repository

import com.example.domain.model.AuthState
import com.example.domain.model.SignUpResult
import com.example.domain.model.StudentProfile
import com.example.domain.model.UnverifiedEmailException
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class MockAuthRepository : AuthRepository {

    val unverifiedUsers: MutableSet<String> = mutableSetOf()

    // Simulates an active authenticated student session mapped to auth.users.id
    private val _currentUser = MutableStateFlow<StudentProfile?>(
        StudentProfile(
            id = "usr_student_demo",
            name = "Student",
            email = "student@college.edu",
            department = "Computer Science & Engineering",
            yearOfStudy = "3rd Year",
            careerInterest = "Artificial Intelligence & Data Science",
            currentStreak = 3,
            isAssessmentCompleted = false,
            recommendedCareerId = null,
            dailyMotivationalMessage = "Small daily habits compound into monumental career breakthroughs."
        )
    )

    override val authState: Flow<AuthState> = _currentUser.map { profile ->
        when {
            profile == null -> AuthState.SignedOut
            unverifiedUsers.contains(profile.email.lowercase()) ->
                AuthState.VerificationRequired(profile.email, "Please verify your email before accessing CGA.")
            profile.isProfileComplete -> AuthState.ProfileComplete(profile)
            else -> AuthState.ProfileIncomplete(profile)
        }
    }

    override fun getCurrentUser(): Flow<StudentProfile?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<StudentProfile> {
        val safeEmail = email.trim().ifBlank { "student@college.edu" }
        if (unverifiedUsers.contains(safeEmail.lowercase())) {
            return Result.failure(
                UnverifiedEmailException(
                    email = safeEmail,
                    message = "Please verify your email address. Enter the 8-digit verification code sent to your email."
                )
            )
        }

        val inferredName = safeEmail.substringBefore("@")
            .replace(".", " ")
            .split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            .ifBlank { "Student" }

        val profile = _currentUser.value?.copy(
            email = safeEmail,
            name = inferredName
        ) ?: StudentProfile(
            id = "usr_${safeEmail.hashCode().toUInt()}",
            name = inferredName,
            email = safeEmail,
            department = "Computer Science & Engineering",
            yearOfStudy = "1st Year",
            careerInterest = "Technology",
            currentStreak = 0,
            isAssessmentCompleted = false,
            recommendedCareerId = null,
            dailyMotivationalMessage = "Welcome back! Keep progressing through your milestones."
        )
        _currentUser.value = profile
        return Result.success(profile)
    }

    var requireVerificationOnSignUp: Boolean = false
    private var pendingSignUpProfile: StudentProfile? = null

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
        department: String,
        year: String,
        careerInterest: String
    ): Result<SignUpResult> {
        val safeEmail = email.trim().ifBlank { "student@college.edu" }
        val newProfile = StudentProfile(
            id = "usr_${safeEmail.hashCode().toUInt()}",
            name = name.ifBlank { "New Student" },
            email = safeEmail,
            department = department,
            yearOfStudy = year,
            careerInterest = careerInterest,
            currentStreak = 0,
            isAssessmentCompleted = false,
            recommendedCareerId = null,
            dailyMotivationalMessage = "Welcome to CGA! Begin your career assessment to discover tailored roadmaps."
        )
        if (requireVerificationOnSignUp) {
            unverifiedUsers.add(safeEmail.lowercase())
            pendingSignUpProfile = newProfile
            _currentUser.value = null
            return Result.success(
                SignUpResult.VerificationRequired(
                    email = safeEmail,
                    message = "An 8-digit verification code has been sent to your email."
                )
            )
        }
        _currentUser.value = newProfile
        return Result.success(SignUpResult.Success(newProfile))
    }

    var mockOtpCode: String = ""
    var sendOtpCount: Int = 0

    override suspend fun sendEmailOtp(email: String): Result<Unit> {
        sendOtpCount++
        return Result.success(Unit)
    }

    override suspend fun verifyEmailOtp(email: String, otp: String): Result<StudentProfile> {
        val cleanOtp = otp.trim()
        if (cleanOtp.length != 8 || !cleanOtp.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Please enter a valid 8-digit verification code."))
        }
        if (mockOtpCode.isNotBlank() && cleanOtp != mockOtpCode) {
            return Result.failure(IllegalArgumentException("Invalid verification code. Please check the code and try again."))
        }
        val safeEmail = email.trim().lowercase()
        unverifiedUsers.remove(safeEmail)
        val pending = pendingSignUpProfile
        val profile = if (pending != null) {
            _currentUser.value = pending
            pendingSignUpProfile = null
            pending
        } else {
            val current = _currentUser.value
            if (current != null) {
                val updated = if (email.isNotBlank()) current.copy(email = email) else current
                _currentUser.value = updated
                updated
            } else {
                val resolvedEmail = email.ifBlank { "student@college.edu" }
                val newProfile = StudentProfile(
                    id = "usr_${resolvedEmail.hashCode().toUInt()}",
                    name = "Student",
                    email = resolvedEmail,
                    department = "Computer Science & Engineering",
                    yearOfStudy = "1st Year",
                    careerInterest = "Technology",
                    currentStreak = 0,
                    isAssessmentCompleted = false
                )
                _currentUser.value = newProfile
                newProfile
            }
        }
        return Result.success(profile)
    }

    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return sendEmailOtp(email)
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return Result.success(Unit)
    }

    fun setMockUser(profile: StudentProfile?) {
        _currentUser.value = profile
    }

    var nextGoogleSignInProfile: StudentProfile? = null

    override suspend fun signInWithGoogle(): Result<Unit> {
        val googleProfile = nextGoogleSignInProfile ?: StudentProfile(
            id = "google_user_mock",
            name = "Google Student",
            email = "student@gmail.com",
            department = "Computer Science & Engineering",
            yearOfStudy = "1st Year",
            careerInterest = "Technology",
            currentStreak = 0,
            isAssessmentCompleted = false
        )
        _currentUser.value = googleProfile
        return Result.success(Unit)
    }

    override suspend fun updateProfile(profile: StudentProfile) {
        _currentUser.value = profile
    }

    override fun updateCachedXp(userId: String, newXp: Int) {
        val current = _currentUser.value
        if (current != null && current.id == userId) {
            _currentUser.value = current.copy(xp = newXp)
        }
    }

    override fun updateCachedStreak(userId: String, newStreak: Int) {
        val current = _currentUser.value
        if (current != null && current.id == userId) {
            _currentUser.value = current.copy(currentStreak = newStreak)
        }
    }

    override suspend fun completeProfile(
        department: String,
        year: String,
        careerInterest: String,
        name: String?
    ): Result<StudentProfile> {
        val current = _currentUser.value ?: StudentProfile(
            id = "usr_completed",
            name = name?.ifBlank { "Student" } ?: "Student",
            email = "student@college.edu"
        )
        val updated = current.copy(
            name = if (!name.isNullOrBlank()) name else current.name,
            department = department,
            yearOfStudy = year,
            careerInterest = careerInterest
        )
        _currentUser.value = updated
        return Result.success(updated)
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override suspend fun refreshSession(): AuthState {
        val current = _currentUser.value
        return when {
            current == null -> AuthState.SignedOut
            unverifiedUsers.contains(current.email.lowercase()) ->
                AuthState.VerificationRequired(current.email, "Please verify your email before accessing CGA.")
            current.isProfileComplete -> AuthState.ProfileComplete(current)
            else -> AuthState.ProfileIncomplete(current)
        }
    }

    override suspend fun checkEmailVerification(email: String?, password: String?): Result<StudentProfile> {
        val safeEmail = email?.trim()?.lowercase() ?: ""
        if (unverifiedUsers.contains(safeEmail)) {
            return Result.failure(Exception("Email is not verified yet. Please verify your email and try again."))
        }
        val current = _currentUser.value
        return if (current != null) {
            val updated = if (!email.isNullOrBlank()) current.copy(email = email) else current
            _currentUser.value = updated
            Result.success(updated)
        } else {
            val resolvedEmail = email?.ifBlank { "student@college.edu" } ?: "student@college.edu"
            val profile = StudentProfile(
                id = "usr_${resolvedEmail.hashCode().toUInt()}",
                name = "Student",
                email = resolvedEmail,
                department = "Computer Science & Engineering",
                yearOfStudy = "1st Year",
                careerInterest = "Technology"
            )
            _currentUser.value = profile
            Result.success(profile)
        }
    }
}

