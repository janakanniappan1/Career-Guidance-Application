package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CgaLogo
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    val tokens = LocalCGATokens.current
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        authViewModel.clearSignUpError()
        authViewModel.clearOtpError()
    }

    val displayLoginError = uiState.loginError ?: if (uiState.signUpError == null && uiState.otpError == null) uiState.error else null

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Sign In",
                onBackClick = onNavigateBack,
                testTag = "login_top_bar"
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .testTag("login_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Official CGA Logo
            CgaLogo(
                modifier = Modifier
                    .size(110.dp)
                    .testTag("cga_login_logo")
            )

            Spacer(modifier = Modifier.height(16.dp))

            TagPill(
                text = "STUDENT PORTAL",
                backgroundColor = CGAColors.LightGreenIconBg,
                textColor = CGAColors.Primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome Back",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sign in to resume your learning path & placement streak",
                color = tokens.textMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("College Email Address", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = if (email.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input"),
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Password", tint = if (password.isNotEmpty()) CGAColors.ActionBlue else tokens.textMuted)
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot password?",
                    color = CGAColors.ActionBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        if (email.isBlank()) {
                            resetMessage = "Please enter your college email address above first."
                        } else {
                            authViewModel.resetPassword(email.trim()) { result ->
                                resetMessage = if (result.isSuccess) {
                                    "Password reset link sent to your email."
                                } else {
                                    result.exceptionOrNull()?.message ?: "Failed to send reset link."
                                }
                            }
                        }
                    }
                )
            }

            if (!resetMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = resetMessage ?: "",
                    color = if (resetMessage?.contains("sent", ignoreCase = true) == true) CGAColors.SuccessGreen else CGAColors.ActionBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!displayLoginError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = displayLoginError,
                    color = androidx.compose.ui.graphics.Color(0xFFDC2626),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CgaPrimaryButton(
                text = if (uiState.isLoading && !uiState.isGoogleAuthStarting) "SIGNING IN..." else "SIGN IN",
                enabled = !uiState.isLoading,
                onClick = {
                    val loginEmail = email.trim()
                    if (loginEmail.isNotBlank() && password.isNotBlank()) {
                        authViewModel.login(loginEmail, password)
                    } else if (loginEmail.isBlank()) {
                        resetMessage = "Please enter your email address."
                    } else {
                        resetMessage = "Please enter your password."
                    }
                },
                testTag = "login_submit_btn"
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
                testTag = "login_google_btn"
            )

            Spacer(modifier = Modifier.height(14.dp))

            CgaSecondaryButton(
                text = "CREATE NEW ACCOUNT",
                onClick = {
                    authViewModel.clearError()
                    onNavigateToSignUp()
                },
                testTag = "login_to_signup_btn"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
