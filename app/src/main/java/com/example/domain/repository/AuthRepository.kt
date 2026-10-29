package com.example.domain.repository

import com.example.domain.model.AuthState
import com.example.domain.model.SignUpResult
import com.example.domain.model.StudentProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthState>
    fun getCurrentUser(): Flow<StudentProfile?>
    suspend fun login(email: String, password: String): Result<StudentProfile>
    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        department: String,
        year: String,
        careerInterest: String
    ): Result<SignUpResult>
    suspend fun sendEmailOtp(email: String): Result<Unit>
    suspend fun verifyEmailOtp(email: String, otp: String): Result<StudentProfile>
    suspend fun resendVerificationEmail(email: String): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun signInWithGoogle(): Result<Unit>
    suspend fun updateProfile(profile: StudentProfile)
    fun updateCachedXp(userId: String, newXp: Int)
    fun updateCachedStreak(userId: String, newStreak: Int)
    suspend fun completeProfile(
        department: String,
        year: String,
        careerInterest: String,
        name: String? = null
    ): Result<StudentProfile>
    suspend fun logout()
    suspend fun refreshSession(): AuthState
    suspend fun checkEmailVerification(email: String? = null, password: String? = null): Result<StudentProfile>
}
