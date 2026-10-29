package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AuthState
import com.example.domain.model.SignUpResult
import com.example.domain.model.StudentProfile
import com.example.domain.model.UnverifiedEmailException
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val authState: AuthState = AuthState.Initializing,
    val isLoading: Boolean = false,
    val isCheckingVerification: Boolean = false,
    val isEmailVerified: Boolean = false,
    val verifiedProfile: StudentProfile? = null,
    val error: String? = null,
    val loginError: String? = null,
    val signUpError: String? = null,
    val otpError: String? = null,
    val isSuccess: Boolean = false,
    val isGoogleAuthStarting: Boolean = false,
    val verificationEmail: String? = null,
    val resendSuccessMessage: String? = null,
    val resendCooldownSeconds: Int = 0,
    val passwordResetSent: Boolean = false,
    val otpCode: String = "",
    val isOtpVerifying: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null
    private var pendingEmail: String? = null
    private var pendingPassword: String? = null

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Initializing
        )

    val currentUser: StateFlow<StudentProfile?> = authRepository.getCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Keep uiState.authState in sync with repository's continuous auth state stream
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                _uiState.update { current ->
                    // If the user was awaiting email verification and Supabase confirms the session
                    if ((current.authState is AuthState.VerificationRequired || current.isEmailVerified) &&
                        (state is AuthState.ProfileComplete || state is AuthState.ProfileIncomplete)) {
                        val profile = if (state is AuthState.ProfileComplete) state.profile else (state as AuthState.ProfileIncomplete).profile
                        current.copy(
                            isEmailVerified = true,
                            verifiedProfile = profile,
                            authState = AuthState.EmailVerified(profile),
                            error = null
                        )
                    } else if (state is AuthState.VerificationRequired) {
                        // Repository emitted VerificationRequired (e.g. from session refresh or unverified session)
                        current.copy(
                            authState = state,
                            verificationEmail = state.email.ifBlank { current.verificationEmail },
                            error = null
                        )
                    } else if (current.authState is AuthState.VerificationRequired && state is AuthState.SignedOut) {
                        // Do not override an active transient VerificationRequired with a transient SignedOut
                        current
                    } else if (current.authState is AuthState.EmailVerified && (state is AuthState.SignedOut || state is AuthState.Authenticating)) {
                        // Maintain the EmailVerified state until the success animation completes
                        current
                    } else {
                        current.copy(authState = state)
                    }
                }
            }
        }
    }

    fun login(email: String, pass: String, onSuccess: (() -> Unit)? = null) {
        if (_uiState.value.isLoading) return
        val cleanEmail = email.trim()
        pendingEmail = cleanEmail
        pendingPassword = pass
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    error = null, 
                    loginError = null, 
                    signUpError = null, 
                    otpError = null, 
                    authState = AuthState.Authenticating
                ) 
            }
            val result = authRepository.login(cleanEmail, pass)
            if (result.isSuccess) {
                val profile = result.getOrThrow()
                val nextState = if (profile.isProfileComplete) {
                    AuthState.ProfileComplete(profile)
                } else {
                    AuthState.ProfileIncomplete(profile)
                }
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        isSuccess = true, 
                        authState = nextState, 
                        error = null, 
                        loginError = null, 
                        signUpError = null, 
                        otpError = null
                    ) 
                }
                onSuccess?.invoke()
            } else {
                val err = result.exceptionOrNull()
                if (err is UnverifiedEmailException) {
                    val vEmail = err.email.ifBlank { cleanEmail }
                    val vMsg = err.message ?: "Please verify your email address. Enter the 8-digit verification code sent to your email."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            verificationEmail = vEmail,
                            otpCode = "",
                            error = vMsg,
                            loginError = vMsg,
                            signUpError = null,
                            otpError = null,
                            authState = AuthState.VerificationRequired(
                                email = vEmail,
                                message = vMsg
                            )
                        )
                    }
                } else {
                    val errMessage = err?.message ?: "Login failed. Please check your credentials."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = false,
                            error = errMessage,
                            loginError = errMessage,
                            signUpError = null,
                            otpError = null,
                            authState = AuthState.AuthError(errMessage)
                        )
                    }
                }
            }
        }
    }

    fun onOAuthCallbackError(errorMessage: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isGoogleAuthStarting = false,
                error = errorMessage,
                authState = AuthState.AuthError(errorMessage)
            )
        }
    }

    fun signUp(
        name: String,
        email: String,
        password: String,
        department: String,
        year: String,
        careerInterest: String,
        onSuccess: (() -> Unit)? = null
    ) {
        if (_uiState.value.isLoading) return
        pendingEmail = email.trim()
        pendingPassword = password
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    resendSuccessMessage = null,
                    authState = AuthState.SigningUp
                )
            }
            val result = authRepository.signUp(name, email, password, department, year, careerInterest)
            result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is SignUpResult.VerificationRequired -> {
                            val vState = AuthState.VerificationRequired(outcome.email, outcome.message)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    authState = vState,
                                    verificationEmail = outcome.email,
                                    otpCode = "",
                                    resendSuccessMessage = "An 8-digit verification code was sent to ${outcome.email}.",
                                    resendCooldownSeconds = 60,
                                    error = null
                                )
                            }
                            startCooldownTimer()
                            // Important: Never call onSuccess() if verification is pending!
                        }
                        is SignUpResult.Success -> {
                            val nextState = if (outcome.profile.isProfileComplete) {
                                AuthState.ProfileComplete(outcome.profile)
                            } else {
                                AuthState.ProfileIncomplete(outcome.profile)
                            }
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    authState = nextState,
                                    isSuccess = true,
                                    error = null
                                )
                            }
                            onSuccess?.invoke()
                        }
                    }
                },
                onFailure = { err ->
                    val errMessage = err.message ?: "Sign up failed. Please try again."
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errMessage,
                            signUpError = errMessage,
                            loginError = null,
                            otpError = null,
                            authState = AuthState.AuthError(errMessage)
                        )
                    }
                }
            )
        }
    }

    fun onOtpCodeChanged(code: String) {
        val digits = code.filter { it.isDigit() }.take(8)
        _uiState.update { it.copy(otpCode = digits, error = null, otpError = null) }
    }

    fun verifyEmailOtp(
        otp: String? = null,
        onVerificationSuccess: ((isProfileComplete: Boolean) -> Unit)? = null
    ) {
        val codeToVerify = (otp ?: _uiState.value.otpCode).trim()
        val targetEmail = _uiState.value.verificationEmail ?: pendingEmail

        if (targetEmail.isNullOrBlank()) {
            _uiState.update { it.copy(error = "No email address found for verification.", otpError = "No email address found for verification.") }
            return
        }

        if (codeToVerify.length != 8 || !codeToVerify.all { it.isDigit() }) {
            _uiState.update { it.copy(error = "Please enter the complete 8-digit verification code.", otpError = "Please enter the complete 8-digit verification code.") }
            return
        }

        if (_uiState.value.isOtpVerifying || _uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isOtpVerifying = true,
                    isLoading = true,
                    error = null,
                    otpError = null,
                    resendSuccessMessage = null
                )
            }
            val result = authRepository.verifyEmailOtp(targetEmail, codeToVerify)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isOtpVerifying = false,
                            isLoading = false,
                            isEmailVerified = true,
                            verifiedProfile = profile,
                            authState = AuthState.EmailVerified(profile),
                            error = null,
                            otpError = null,
                            loginError = null,
                            signUpError = null
                        )
                    }
                    onVerificationSuccess?.invoke(profile.isProfileComplete)
                },
                onFailure = { err ->
                    val message = err.message?.ifBlank { null }
                        ?: "Invalid verification code. Please check the code and try again."
                    _uiState.update {
                        it.copy(
                            isOtpVerifying = false,
                            isLoading = false,
                            isEmailVerified = false,
                            error = message,
                            otpError = message
                        )
                    }
                }
            )
        }
    }

    fun resendEmailOtp(email: String? = null) {
        val targetEmail = email ?: _uiState.value.verificationEmail ?: pendingEmail
        if (targetEmail.isNullOrBlank()) {
            _uiState.update { it.copy(error = "No email address found to resend.") }
            return
        }
        if (_uiState.value.resendCooldownSeconds > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, resendSuccessMessage = null) }
            val result = authRepository.sendEmailOtp(targetEmail)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        resendSuccessMessage = "A new 8-digit verification code has been sent to $targetEmail.",
                        resendCooldownSeconds = 60
                    )
                }
                startCooldownTimer()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to resend verification code."
                    )
                }
            }
        }
    }

    fun resendVerificationEmail(email: String? = null) {
        resendEmailOtp(email)
    }

    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (_uiState.value.resendCooldownSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(resendCooldownSeconds = (it.resendCooldownSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun completeProfile(
        department: String,
        year: String,
        careerInterest: String,
        name: String? = null,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.completeProfile(
                department = department,
                year = year,
                careerInterest = careerInterest,
                name = name
            )
            if (result.isSuccess) {
                val updated = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authState = AuthState.ProfileComplete(updated),
                        error = null
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Unable to save academic details."
                    )
                }
            }
        }
    }

    fun resetToSignedOut() {
        cooldownJob?.cancel()
        pendingEmail = null
        pendingPassword = null
        _uiState.update {
            it.copy(
                isLoading = false,
                isCheckingVerification = false,
                isEmailVerified = false,
                error = null,
                loginError = null,
                signUpError = null,
                otpError = null,
                isSuccess = false,
                isGoogleAuthStarting = false,
                verificationEmail = null,
                resendSuccessMessage = null,
                resendCooldownSeconds = 0,
                passwordResetSent = false,
                otpCode = "",
                isOtpVerifying = false,
                authState = AuthState.SignedOut
            )
        }
    }

    /**
     * Actively checks email verification status with Supabase.
     * Prevents duplicate requests by guarding against isCheckingVerification.
     * On success, transitions to AuthState.EmailVerified so UI can show animated tick.
     * On failure, keeps screen and displays user-facing error message.
     */
    fun verifyEmail(email: String? = null, onVerified: (() -> Unit)? = null) {
        if (_uiState.value.isCheckingVerification) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingVerification = true,
                    error = null,
                    resendSuccessMessage = null
                )
            }
            val targetEmail = email?.trim()
                ?: _uiState.value.verificationEmail
                ?: pendingEmail
            val result = authRepository.checkEmailVerification(targetEmail, pendingPassword)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isCheckingVerification = false,
                            isEmailVerified = true,
                            verifiedProfile = profile,
                            authState = AuthState.EmailVerified(profile),
                            error = null
                        )
                    }
                    onVerified?.invoke()
                },
                onFailure = { err ->
                    val message = err.message?.ifBlank { null }
                        ?: "Email is not verified yet. Please verify your email and try again."
                    _uiState.update {
                        it.copy(
                            isCheckingVerification = false,
                            isEmailVerified = false,
                            error = message
                        )
                    }
                }
            )
        }
    }

    /**
     * Invoked after the animated success tick has completed its display and delay period.
     * Transitions state to ProfileComplete / ProfileIncomplete and continues navigation.
     */
    fun completeEmailVerification(onNavigate: (isProfileComplete: Boolean) -> Unit) {
        val profile = _uiState.value.verifiedProfile
            ?: (currentUser.value)
            ?: (_uiState.value.authState as? AuthState.EmailVerified)?.profile
        val isComplete = profile?.isProfileComplete == true
        val nextState = if (profile != null) {
            if (isComplete) AuthState.ProfileComplete(profile) else AuthState.ProfileIncomplete(profile)
        } else {
            AuthState.SignedOut
        }
        _uiState.update {
            it.copy(
                authState = nextState,
                isEmailVerified = false
            )
        }
        onNavigate(isComplete)
    }

    fun refreshSession() {
        if (_uiState.value.authState is AuthState.VerificationRequired || _uiState.value.verificationEmail != null) {
            verifyEmail()
        } else {
            viewModelScope.launch {
                val state = authRepository.refreshSession()
                _uiState.update { it.copy(authState = state) }
            }
        }
    }

    fun signInWithGoogle(onSuccess: (() -> Unit)? = null) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isGoogleAuthStarting = true,
                    error = null,
                    authState = AuthState.Authenticating
                )
            }
            val result = authRepository.signInWithGoogle()
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, isGoogleAuthStarting = false) }
                onSuccess?.invoke()
            } else {
                val errMessage = result.exceptionOrNull()?.message ?: "Google authentication failed."
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isGoogleAuthStarting = false,
                        error = errMessage,
                        authState = AuthState.AuthError(errMessage)
                    )
                }
            }
        }
    }

    fun resetPassword(email: String, onResult: ((Result<Unit>) -> Unit)? = null) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, passwordResetSent = false) }
            val result = authRepository.resetPassword(email)
            if (result.isSuccess) {
                _uiState.update { it.copy(isLoading = false, passwordResetSent = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        passwordResetSent = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to send reset link."
                    )
                }
            }
            onResult?.invoke(result)
        }
    }

    fun clearError() {
        _uiState.update { 
            it.copy(
                error = null,
                loginError = null,
                signUpError = null,
                otpError = null
            ) 
        }
    }

    fun clearLoginError() {
        _uiState.update { it.copy(loginError = null, error = if (it.signUpError == null && it.otpError == null) null else it.error) }
    }

    fun clearSignUpError() {
        _uiState.update { it.copy(signUpError = null, error = if (it.loginError == null && it.otpError == null) null else it.error) }
    }

    fun clearOtpError() {
        _uiState.update { it.copy(otpError = null, error = if (it.loginError == null && it.signUpError == null) null else it.error) }
    }

    fun logout() {
        viewModelScope.launch {
            cooldownJob?.cancel()
            authRepository.logout()
            _uiState.value = AuthUiState(authState = AuthState.SignedOut)
        }
    }
}
