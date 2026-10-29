package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CgaLogo
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val tokens = LocalCGATokens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.contentBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
            .testTag("welcome_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))

            // Official CGA Logo
            CgaLogo(
                modifier = Modifier
                    .size(160.dp)
                    .testTag("cga_welcome_logo")
            )

            Spacer(modifier = Modifier.height(20.dp))

            TagPill(
                text = "CAREER GUIDANCE APPLICATION",
                backgroundColor = tokens.successContainer,
                textColor = tokens.onSuccessContainer
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Empowering Your Career Journey",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Interactive milestones and high-impact career clarity. Navigate skill assessments, campus roadmaps, and mock tests.",
                color = tokens.textMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Feature Highlights with CGA 20dp rounded cards
            WelcomeFeatureItem(
                icon = Icons.Default.AutoAwesome,
                title = "15-Point AI Assessment",
                description = "Maps problem-solving instincts to modern high-impact tech roles.",
                color = CGAColors.ElectricPurple,
                bgColor = CGAColors.LightPurpleIconBg
            )
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeFeatureItem(
                icon = Icons.Default.TrendingUp,
                title = "Milestone Roadmaps",
                description = "Step-by-step campus placement milestones with daily streak targets.",
                color = CGAColors.Primary,
                bgColor = CGAColors.LightGreenIconBg
            )
            Spacer(modifier = Modifier.height(12.dp))
            WelcomeFeatureItem(
                icon = Icons.Default.CheckCircle,
                title = "Live Mock Practice",
                description = "DSA, Aptitude challenges and instant mentor consultations.",
                color = CGAColors.GamificationOrange,
                bgColor = CGAColors.LightOrangeIconBg
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        ) {
            CgaPrimaryButton(
                text = "GET STARTED",
                onClick = onNavigateToSignUp,
                testTag = "welcome_signup_btn"
            )

            Spacer(modifier = Modifier.height(14.dp))

            CgaSecondaryButton(
                text = "I ALREADY HAVE AN ACCOUNT",
                onClick = onNavigateToLogin,
                testTag = "welcome_login_btn"
            )
        }
    }
}

@Composable
private fun WelcomeFeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    bgColor: Color
) {
    val tokens = LocalCGATokens.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = tokens.textMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
