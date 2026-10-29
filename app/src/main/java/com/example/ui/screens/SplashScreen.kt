package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AuthState
import com.example.ui.components.CgaLogo
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SplashScreen(
    authState: AuthState,
    onNavigate: (destination: String) -> Unit
) {
    val tokens = LocalCGATokens.current
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.96f) }

    LaunchedEffect(Unit) {
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            )
        }
    }

    // Resolve startup destination smoothly according to actual Supabase session status
    LaunchedEffect(authState) {
        // Allow the splash animation at least 350ms to settle for visual intent
        delay(350)
        when (authState) {
            is AuthState.ProfileComplete -> {
                onNavigate("dashboard")
            }
            is AuthState.ProfileIncomplete -> {
                onNavigate("complete_profile")
            }
            is AuthState.SignedOut -> {
                onNavigate("welcome")
            }
            is AuthState.VerificationRequired -> {
                onNavigate("sign_up")
            }
            is AuthState.Initializing, is AuthState.Authenticating -> {
                // Wait for session restoration up to 2 seconds, then safely fall back to welcome
                val resolved = withTimeoutOrNull(2000L) {
                    while (true) {
                        delay(100)
                    }
                }
                if (resolved == null && (authState is AuthState.Initializing || authState is AuthState.Authenticating)) {
                    onNavigate("welcome")
                }
            }
            else -> {
                onNavigate("welcome")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.contentBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .alpha(alphaAnim.value)
        ) {
            // Full original CGA logo with responsive sizing and subtle entry scale
            CgaLogo(
                modifier = Modifier
                    .sizeIn(minWidth = 180.dp, maxWidth = 260.dp, minHeight = 180.dp, maxHeight = 260.dp)
                    .fillMaxWidth(0.72f)
                    .scale(scaleAnim.value)
                    .testTag("cga_splash_logo")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle Tag Badge
            TagPill(
                text = "CAREER GUIDANCE APPLICATION",
                backgroundColor = Color(0xFF065F46),
                textColor = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Headline
            Text(
                text = "Empowering Your Career\nJourney",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = tokens.textHeading,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Supporting description
            Text(
                text = "Interactive milestones and high-impact career clarity.\nNavigate skill assessments, campus roadmaps, and mock tests.",
                color = tokens.textMuted,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            val isCheckingSession = authState is AuthState.Initializing || authState is AuthState.Authenticating

            AnimatedVisibility(
                visible = isCheckingSession,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = CGAColors.Primary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Restoring session...",
                        color = tokens.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedVisibility(
                visible = !isCheckingSession && authState is AuthState.SignedOut,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                CgaPrimaryButton(
                    text = "GET STARTED",
                    onClick = { onNavigate("welcome") },
                    testTag = "splash_get_started_btn"
                )
            }
        }
    }
}
