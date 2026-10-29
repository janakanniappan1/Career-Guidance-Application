package com.example.ui.screens

import android.util.Patterns
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AuthState
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit = onNavigateToDashboard,
    onNavigateToLogin: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var yearOfStudy by remember { mutableStateOf("") }
    var careerInterest by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var departmentError by remember { mutableStateOf<String?>(null) }
    var yearError by remember { mutableStateOf<String?>(null) }

    val tokens = LocalCGATokens.current
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val isEmailVerified = uiState.authState is AuthState.EmailVerified || uiState.isEmailVerified
    val isVerificationRequired = (uiState.authState is AuthState.VerificationRequired) || isEmailVerified
    val targetEmail = (uiState.authState as? AuthState.VerificationRequired)?.email
        ?: uiState.verificationEmail
        ?: email

    LaunchedEffect(Unit) {
        if (!isVerificationRequired) {
            authViewModel.clearLoginError()
            authViewModel.clearOtpError()
        }
    }

    val displaySignUpError = uiState.signUpError ?: if (uiState.loginError == null && uiState.otpError == null) uiState.error else null
    val displayOtpError = uiState.otpError ?: if (uiState.loginError == null && uiState.signUpError == null) uiState.error else null

    Scaffold(
        topBar = {
            CgaTopBar(
                title = when {
                    isEmailVerified -> "Email Verification"
                    isVerificationRequired -> "Verify OTP Code"
                    else -> "Student Onboarding"
                },
                onBackClick = {
                    authViewModel.clearError()
                    if (isEmailVerified) {
                        // Do not interrupt the success animation transition
                    } else if (isVerificationRequired) {
                        authViewModel.resetToSignedOut()
                        onNavigateToLogin()
                    } else {
                        onNavigateBack()
                    }
                },
                testTag = "signup_top_bar"
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        if (isEmailVerified) {
            EmailVerificationSuccessView(
                onAnimationComplete = {
                    authViewModel.completeEmailVerification { isProfileComplete ->
                        if (isProfileComplete) {
                            onNavigateToDashboard()
                        } else {
                            onNavigateToCompleteProfile()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("signup_screen"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (isVerificationRequired) {
                // ==========================================
                // SUPABASE EMAIL OTP VERIFICATION VIEW
                // ==========================================
                TagPill(
                    text = "STEP 2: EMAIL VERIFICATION",
                    backgroundColor = CGAColors.LightGreenIconBg,
                    textColor = CGAColors.Primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(CGAColors.LightGreenIconBg, CircleShape)
                        .border(2.dp, CGAColors.Primary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Email OTP Verification",
                        tint = CGAColors.Primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Enter Verification Code",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "An 8-digit verification code has been sent to:",
                    color = tokens.textMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = targetEmail,
                    color = CGAColors.Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("otp_target_email")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 8-digit OTP Input
                OtpCodeInputField(
                    otpValue = uiState.otpCode,
                    onOtpChange = { authViewModel.onOtpCodeChanged(it) },
                    enabled = !uiState.isOtpVerifying && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!uiState.resendSuccessMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.resendSuccessMessage ?: "",
                                color = Color(0xFF065F46),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (!displayOtpError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = displayOtpError,
                        color = Color(0xFFDC2626),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("otp_error_text")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 1. Primary Action: VERIFY OTP button
                val canVerify = uiState.otpCode.length == 8 && !uiState.isOtpVerifying && !uiState.isLoading
                CgaPrimaryButton(
                    text = if (uiState.isOtpVerifying || uiState.isLoading) "VERIFYING CODE..." else "VERIFY CODE",
                    enabled = canVerify,
                    onClick = {
                        authViewModel.verifyEmailOtp()
                    },
                    testTag = "verify_otp_btn"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Secondary Action: RESEND CODE (with countdown cooldown)
                val isCooldown = uiState.resendCooldownSeconds > 0
                CgaSecondaryButton(
                    text = when {
                        uiState.isLoading -> "SENDING CODE..."
                        isCooldown -> "RESEND CODE IN ${uiState.resendCooldownSeconds}s"
                        else -> "RESEND CODE"
                    },
                    leadingIcon = Icons.Default.Refresh,
                    enabled = !uiState.isLoading && !isCooldown && !uiState.isOtpVerifying,
                    onClick = {
                        authViewModel.resendEmailOtp(targetEmail)
                    },
                    testTag = "resend_otp_btn"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Edit details / Back to Sign Up
                CgaSecondaryButton(
                    text = "WRONG EMAIL? BACK TO SIGN UP",
                    enabled = !uiState.isOtpVerifying,
                    onClick = {
                        authViewModel.resetToSignedOut()
                    },
                    testTag = "otp_back_to_signup_btn"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Return to Login
                CgaSecondaryButton(
                    text = "ALREADY HAVE AN ACCOUNT? SIGN IN",
                    enabled = !uiState.isOtpVerifying,
                    onClick = {
                        authViewModel.resetToSignedOut()
                        onNavigateToLogin()
                    },
                    testTag = "otp_to_login_btn"
                )

                Spacer(modifier = Modifier.height(28.dp))

            } else {
                // ==========================================
                // REGULAR SIGN UP / PROFILE CREATION VIEW
                // ==========================================
                TagPill(
                    text = "ONBOARDING STEP 1 OF 2",
                    backgroundColor = CGAColors.LightGreenIconBg,
                    textColor = CGAColors.Primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Create Your CGA Profile",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Fields marked with * are required to personalize your career roadmaps",
                    color = tokens.textMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Full Name *
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Full Name *", fontSize = 13.sp) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Name", tint = if (name.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("signup_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surface,
                        unfocusedContainerColor = tokens.surface,
                        focusedBorderColor = CGAColors.ActionBlue,
                        focusedLabelColor = CGAColors.ActionBlue,
                        unfocusedBorderColor = tokens.borderMain
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // College Email ID *
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (it.isNotBlank()) emailError = null
                    },
                    label = { Text("College Email ID *", fontSize = 13.sp) },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = if (email.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("signup_email_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surface,
                        unfocusedContainerColor = tokens.surface,
                        focusedBorderColor = CGAColors.ActionBlue,
                        focusedLabelColor = CGAColors.ActionBlue,
                        unfocusedBorderColor = tokens.borderMain
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Create Password *
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (it.length >= 6) passwordError = null
                    },
                    label = { Text("Create Password * (min 6 characters)", fontSize = 13.sp) },
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = if (password.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("signup_password_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surface,
                        unfocusedContainerColor = tokens.surface,
                        focusedBorderColor = CGAColors.ActionBlue,
                        focusedLabelColor = CGAColors.ActionBlue,
                        unfocusedBorderColor = tokens.borderMain
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Academic Department *
                OutlinedTextField(
                    value = department,
                    onValueChange = {
                        department = it
                        if (it.isNotBlank()) departmentError = null
                    },
                    label = { Text("Academic Department * (e.g. CSE, IT, ECE)", fontSize = 13.sp) },
                    isError = departmentError != null,
                    supportingText = departmentError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Business, contentDescription = "Department", tint = if (department.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("signup_department_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surface,
                        unfocusedContainerColor = tokens.surface,
                        focusedBorderColor = CGAColors.ActionBlue,
                        focusedLabelColor = CGAColors.ActionBlue,
                        unfocusedBorderColor = tokens.borderMain
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Year of Study *
                OutlinedTextField(
                    value = yearOfStudy,
                    onValueChange = {
                        yearOfStudy = it
                        if (it.isNotBlank()) yearError = null
                    },
                    label = { Text("Year of Study * (e.g. 1st Year, 2nd Year, 3rd Year)", fontSize = 13.sp) },
                    isError = yearError != null,
                    supportingText = yearError?.let { { Text(it, color = Color(0xFFDC2626), fontSize = 11.sp) } },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.School, contentDescription = "Year", tint = if (yearOfStudy.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("signup_year_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surface,
                        unfocusedContainerColor = tokens.surface,
                        focusedBorderColor = CGAColors.ActionBlue,
                        focusedLabelColor = CGAColors.ActionBlue,
                        unfocusedBorderColor = tokens.borderMain
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Target Career Interest (Optional)
                OutlinedTextField(
                    value = careerInterest,
                    onValueChange = { careerInterest = it },
                    label = { Text("Target Career Interest (Optional)", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Interest", tint = if (careerInterest.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("signup_interest_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = tokens.surface,
                        unfocusedContainerColor = tokens.surface,
                        focusedBorderColor = CGAColors.ActionBlue,
                        focusedLabelColor = CGAColors.ActionBlue,
                        unfocusedBorderColor = tokens.borderMain
                    )
                )

                if (!displaySignUpError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = displaySignUpError,
                            color = Color(0xFFB91C1C),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                CgaPrimaryButton(
                    text = if (uiState.isLoading && !uiState.isGoogleAuthStarting) "CREATING ACCOUNT..." else "COMPLETE ONBOARDING & START",
                    enabled = !uiState.isLoading,
                    onClick = {
                        // Pre-submission client-side validation for all mandatory fields
                        var hasError = false

                        if (name.isBlank()) {
                            nameError = "Please enter your full name."
                            hasError = true
                        } else {
                            nameError = null
                        }

                        val cleanEmail = email.trim()
                        if (cleanEmail.isBlank()) {
                            emailError = "Please enter your college email address."
                            hasError = true
                        } else if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                            emailError = "Please enter a valid email address."
                            hasError = true
                        } else {
                            emailError = null
                        }

                        if (password.isBlank()) {
                            passwordError = "Please create a password."
                            hasError = true
                        } else if (password.length < 6) {
                            passwordError = "Password must be at least 6 characters."
                            hasError = true
                        } else {
                            passwordError = null
                        }

                        if (department.isBlank()) {
                            departmentError = "Please enter or select your department."
                            hasError = true
                        } else {
                            departmentError = null
                        }

                        if (yearOfStudy.isBlank()) {
                            yearError = "Please enter your year of study."
                            hasError = true
                        } else {
                            yearError = null
                        }

                        if (!hasError) {
                            authViewModel.signUp(
                                name = name.trim(),
                                email = cleanEmail,
                                password = password,
                                department = department.trim(),
                                year = yearOfStudy.trim(),
                                careerInterest = careerInterest.trim().ifBlank { "Technology" }
                            ) {
                                onNavigateToDashboard()
                            }
                        }
                    },
                    testTag = "signup_submit_btn"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = tokens.borderMain)
                    Text(
                        text = "  OR  ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textMuted
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = tokens.borderMain)
                }

                Spacer(modifier = Modifier.height(14.dp))

                CgaSecondaryButton(
                    text = if (uiState.isGoogleAuthStarting) "OPENING GOOGLE..." else "CONTINUE WITH GOOGLE",
                    enabled = !uiState.isLoading,
                    leadingIcon = Icons.Default.AccountCircle,
                    onClick = {
                        authViewModel.signInWithGoogle()
                    },
                    testTag = "signup_google_btn"
                )

                Spacer(modifier = Modifier.height(12.dp))

                CgaSecondaryButton(
                    text = "ALREADY HAVE AN ACCOUNT? SIGN IN",
                    onClick = {
                        authViewModel.clearError()
                        onNavigateToLogin()
                    },
                    testTag = "signup_to_login_btn"
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
}

/**
 * 8-digit OTP Input field rendering 8 distinct boxed character cells.
 */
@Composable
fun OtpCodeInputField(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val tokens = LocalCGATokens.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    BasicTextField(
        value = otpValue,
        onValueChange = { newValue ->
            val digits = newValue.filter { it.isDigit() }.take(8)
            onOtpChange(digits)
        },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        ),
        modifier = modifier
            .testTag("otp_code_input")
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        try {
                            focusRequester.requestFocus()
                        } catch (_: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                // Ensure innerTextField is composed so Compose text input connection is active
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .graphicsLayer { alpha = 0f }
                ) {
                    innerTextField()
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 0 until 8) {
                        val char = otpValue.getOrNull(i)?.toString() ?: ""
                        val isCurrent = (otpValue.length == i)
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.surface)
                                .border(
                                    width = if (isCurrent) 2.dp else if (char.isNotEmpty()) 1.5.dp else 1.dp,
                                    color = when {
                                        char.isNotEmpty() -> CGAColors.ActionBlue
                                        isCurrent -> CGAColors.ActionBlue.copy(alpha = 0.8f)
                                        else -> tokens.borderMain
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = char,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tokens.textHeading,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    )
}

/**
 * Polished animated success state shown when Supabase email verification succeeds.
 *
 * Requirements:
 * - Centered layout with generous spacing and clean hierarchy
 * - Animated check/tick in center:
 *     a. Circle appears
 *     b. Scale/pop animation
 *     c. Smooth vector checkmark stroke
 * - Uses existing CGA theme colors (CGAColors.Primary green and LightGreenIconBg)
 * - Success text: "Email Verified!"
 * - Supporting text: "Your email has been successfully verified."
 * - Waits 1-2 seconds (1500ms) after animation completes before navigating forward
 */
@Composable
fun EmailVerificationSuccessView(
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current
    val circleScale = remember { Animatable(0f) }
    val circleAlpha = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentTranslationY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // Step a: Circle scale/pop + subtle fade-in
        launch {
            circleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 250, easing = LinearEasing)
            )
        }
        circleScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // Step b: Smooth vector checkmark draw
        checkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )

        // Step c: Text fade-in + subtle upward slide
        launch {
            contentTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )

        // Step d: Wait approximately 1.5 seconds after animation completes
        delay(1500L)

        // Step e: Trigger navigation to authenticated destination
        onAnimationComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("email_verified_success_container"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Centered large animated check/tick icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    scaleX = circleScale.value
                    scaleY = circleScale.value
                    alpha = circleAlpha.value
                }
                .semantics { contentDescription = "Email Verified" }
                .testTag("email_verified_tick"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Background circular fill
                drawCircle(
                    color = Color(0xFFE8F5E9), // CGAColors.LightGreenIconBg
                    radius = radius,
                    center = centerOffset
                )

                // Circular border
                drawCircle(
                    color = Color(0xFF0F9D58), // CGAColors.Primary
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Vector checkmark path: start -> vertex -> end
                val start = Offset(size.width * 0.30f, size.height * 0.51f)
                val vertex = Offset(size.width * 0.44f, size.height * 0.66f)
                val end = Offset(size.width * 0.71f, size.height * 0.38f)

                val len1 = (vertex - start).getDistance()
                val len2 = (end - vertex).getDistance()
                val totalLen = len1 + len2
                val p1 = len1 / totalLen

                val progress = checkProgress.value
                if (progress > 0f) {
                    val path = Path()
                    path.moveTo(start.x, start.y)

                    if (progress <= p1) {
                        val segmentFrac = progress / p1
                        val point = start + (vertex - start) * segmentFrac
                        path.lineTo(point.x, point.y)
                    } else {
                        path.lineTo(vertex.x, vertex.y)
                        val segmentFrac = (progress - p1) / (1f - p1)
                        val point = vertex + (end - vertex) * segmentFrac
                        path.lineTo(point.x, point.y)
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFF0F9D58),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Success text and supporting text
        Column(
            modifier = Modifier
                .graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = contentTranslationY.value
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Email Verified!",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("email_verified_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your email has been successfully verified.",
                color = tokens.textMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("email_verified_message")
            )
        }
    }
}
