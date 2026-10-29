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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CgaBottomNavigation
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.StreakBadge
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.DashboardViewModel

@Composable
fun ProfileScreen(
    dashboardViewModel: DashboardViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToRoadmap: (String) -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToResume: () -> Unit,
    onNavigateToPortfolio: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToLearningCompanion: () -> Unit = {},
    onLogout: () -> Unit
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val tokens = LocalCGATokens.current

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Student Profile",
                onBackClick = onNavigateBack,
                testTag = "profile_top_bar",
                actions = {
                    StreakBadge(days = state.currentStreak)
                }
            )
        },
        bottomBar = {
            CgaBottomNavigation(
                currentRoute = "profile",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> onNavigateToHome()
                        "roadmap" -> onNavigateToRoadmap("")
                        "practice" -> onNavigateToPractice()
                        "explore" -> onNavigateToExplore()
                        "profile" -> { /* Current */ }
                    }
                }
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
                .testTag("profile_screen")
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Student Card (24dp radius, 2dp border, 6dp bottom border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(tokens.surface)
                    .border(2.dp, tokens.borderMain, RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .testTag("student_profile_card")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF2563EB))
                            .border(3.dp, Color.White, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.studentName.firstOrNull()?.uppercase() ?: "A",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = state.studentName,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${state.department} • Year ${state.yearOfStudy}",
                        color = tokens.textMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TagPill(
                        text = "GOAL: ${state.recommendedCareer.uppercase()}",
                        backgroundColor = if (tokens.isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                        textColor = if (tokens.isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Streak and XP Stats Row with semantic accents
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🔥 ${state.currentStreak} Days",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Daily Streak",
                                color = tokens.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(tokens.borderMain)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "⭐ ${state.xp} XP",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier.testTag("profile_total_xp")
                            )
                            Text(
                                text = "Total XP",
                                color = tokens.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(36.dp)
                                .background(tokens.borderMain)
                        )

                        // Data-driven status color for milestones
                        val milestoneProgressColor = when {
                            state.roadmapProgress >= 100 -> Color(0xFF58CC02)
                            state.roadmapProgress > 0 -> Color(0xFFF59E0B)
                            else -> Color(0xFF0891B2)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (state.isRoadmapProgressAvailable) "🎯 ${state.roadmapProgress}%" else "🎯 Unavailable",
                                color = if (state.isRoadmapProgressAvailable) milestoneProgressColor else tokens.textMuted,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Milestones",
                                color = tokens.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Placement Tools & Integration with modern semantic colors
            Text(
                text = "Placement Acceleration",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileNavigationRow(
                icon = Icons.Default.Description,
                iconBg = if (tokens.isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                iconTint = Color(0xFF2563EB),
                title = "ATS Resume Reviewer",
                subtitle = "Resume analysis • Coming soon",
                onClick = onNavigateToResume
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileNavigationRow(
                icon = Icons.Default.Work,
                iconBg = if (tokens.isDark) Color(0xFF1E293B) else Color(0xFFF5F3FF),
                iconTint = Color(0xFF7C3AED),
                title = "Project Portfolio Builder",
                subtitle = "Add and showcase your projects",
                onClick = onNavigateToPortfolio
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileNavigationRow(
                icon = Icons.Default.EmojiEvents,
                iconBg = if (tokens.isDark) Color(0xFF1E293B) else Color(0xFFFEF3C7),
                iconTint = Color(0xFFD97706),
                title = "Domain Leaderboard",
                subtitle = "Compete with peers in XP, completion & streak",
                onClick = onNavigateToLeaderboard
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileNavigationRow(
                icon = Icons.Default.School,
                iconBg = if (tokens.isDark) Color(0xFF1E293B) else Color(0xFFF3E8FF),
                iconTint = CGAColors.ElectricPurple,
                title = "AI Learning Companion",
                subtitle = "Multimodal materials, tutor & adaptive learning",
                onClick = onNavigateToLearningCompanion
            )

            Spacer(modifier = Modifier.height(10.dp))

            ProfileNavigationRow(
                icon = Icons.Default.AdminPanelSettings,
                iconBg = if (tokens.isDark) Color(0xFF1E293B) else Color(0xFFFEF3C7),
                iconTint = Color(0xFFD97706),
                title = "Placement Cell Admin Panel",
                subtitle = "Cohort statistics, test banks & analytics",
                onClick = onNavigateToAdmin
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Logout Action Button (Subtle Red Destructive styling)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (tokens.isDark) Color(0xFF2D1515) else Color(0xFFFEF2F2))
                    .border(1.5.dp, if (tokens.isDark) Color(0xFF7F1D1D) else Color(0xFFFCA5A5), RoundedCornerShape(16.dp))
                    .clickable {
                        authViewModel.logout()
                        onLogout()
                    }
                    .padding(vertical = 14.dp)
                    .testTag("profile_logout_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = "SIGN OUT",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ProfileNavigationRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val tokens = LocalCGATokens.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = subtitle,
                        color = tokens.textMuted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
