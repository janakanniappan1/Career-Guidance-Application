package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CareerCard
import com.example.ui.components.CgaBottomNavigation
import com.example.ui.components.CgaHeader
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.FeatureCard
import com.example.ui.components.FeatureColorType
import com.example.ui.components.HeroCard
import com.example.ui.components.MentorCard
import com.example.ui.components.SkillChip
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel,
    authViewModel: AuthViewModel,
    onNavigateToAssessment: () -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToRoadmap: (String) -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToResume: () -> Unit,
    onNavigateToPortfolio: () -> Unit,
    onNavigateToLeaderboard: () -> Unit = {},
    onNavigateToMentorChat: () -> Unit = {},
    onNavigateToLearningCompanion: () -> Unit = {},
    onLogout: () -> Unit
) {
    val state by dashboardViewModel.uiState.collectAsState()
    val tokens = LocalCGATokens.current

    Scaffold(
        topBar = {
            CgaHeader(
                studentName = state.studentName,
                greeting = "WELCOME BACK",
                streakDays = state.currentStreak,
                xp = state.xp,
                onAvatarClick = onNavigateToProfile
            )
        },
        bottomBar = {
            CgaBottomNavigation(
                currentRoute = "dashboard",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> { /* Current */ }
                        "roadmap" -> onNavigateToRoadmap(state.recommendedCareerId)
                        "practice" -> onNavigateToPractice()
                        "explore" -> onNavigateToExplore()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToMentorChat,
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Open CGA AI Mentor",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "AI Mentor",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                },
                containerColor = CGAColors.Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("floating_mentor_button")
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = tokens.contentBackground,
        modifier = Modifier.testTag("dashboard_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // HERO CARD (Section 9: 2dp border, 6dp bottom border, 24dp radius, 20dp padding)
            HeroCard(
                tagText = if (state.isAssessmentCompleted) "CAREER MATCH" else "AI POWERED",
                heading = if (state.isAssessmentCompleted) {
                    "Your path to ${state.recommendedCareer} is ${state.placementReadinessPercentage}% aligned!"
                } else {
                    "Discover roles mapping your true professional capability"
                },
                progressLabel = "Placement Readiness",
                progress = state.placementReadinessPercentage / 100f,
                ctaText = if (state.isAssessmentCompleted) "VIEW YOUR ROADMAP" else "START CAREER ASSESSMENT",
                onCtaClick = {
                    if (state.isAssessmentCompleted) {
                        onNavigateToRoadmap(state.recommendedCareerId)
                    } else {
                        onNavigateToAssessment()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: 2-COLUMN FEATURE GRID (Section 12 & 13)
            Text(
                text = "Core Learning Modules",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row 1: Purple (Assessment) & Blue (Roadmap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FeatureCard(
                    title = "Career Assessment",
                    description = "15 AI questions mapping skills to roles",
                    icon = Icons.Default.Psychology,
                    colorType = FeatureColorType.PURPLE,
                    onClick = {
                        if (state.isAssessmentCompleted) onNavigateToResults() else onNavigateToAssessment()
                    },
                    badgeText = if (state.isAssessmentCompleted) "COMPLETED" else "AI PICK",
                    modifier = Modifier.weight(1f)
                )

                FeatureCard(
                    title = "Skill Roadmaps",
                    description = "Step-by-step career milestone guide",
                    icon = Icons.Default.Map,
                    colorType = FeatureColorType.BLUE,
                    onClick = { onNavigateToRoadmap(state.recommendedCareerId) },
                    badgeText = if (state.isRoadmapProgressAvailable) "${state.roadmapProgress}%" else "Unavailable",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2: Green (Daily Practice) & Orange (Speed Challenge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FeatureCard(
                    title = "Daily Practice",
                    description = "DSA, Aptitude & Technical Coding",
                    icon = Icons.Default.Code,
                    colorType = FeatureColorType.GREEN,
                    onClick = onNavigateToPractice,
                    badgeText = "+50 XP",
                    modifier = Modifier.weight(1f)
                )

                FeatureCard(
                    title = "Speed Challenge",
                    description = "Gamified placement speed quiz",
                    icon = Icons.Default.Bolt,
                    colorType = FeatureColorType.ORANGE,
                    onClick = onNavigateToQuiz,
                    badgeText = "STREAK",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 3: Leaderboard Feature Card
            FeatureCard(
                title = "Domain Leaderboard",
                description = "Rank by XP, Course Completion & Streak across peers",
                icon = Icons.Default.EmojiEvents,
                colorType = FeatureColorType.ORANGE,
                onClick = onNavigateToLeaderboard,
                badgeText = "RANKINGS",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_leaderboard_card")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Row 4: AI Learning Companion Feature Card (Track D: Personalized Tutoring & Adaptive Learning)
            FeatureCard(
                title = "AI Learning Companion",
                description = "Learn from your study materials with source-grounded tutoring & adaptive mastery",
                icon = Icons.Default.School,
                colorType = FeatureColorType.PURPLE,
                onClick = onNavigateToLearningCompanion,
                badgeText = "TRACK D • NEW",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_learning_companion_card")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: CAREER RECOMMENDATION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recommended Career",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    text = "See All",
                    color = tokens.onInfoContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onNavigateToExplore() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            CareerCard(
                title = if (state.isAssessmentCompleted) state.recommendedCareer else "AI / Machine Learning Engineer",
                description = "Design autonomous algorithms, machine learning models, and scalable predictive systems.",
                matchPercentage = if (state.matchPercentage > 0) state.matchPercentage else 92,
                salaryRange = "₹8 - ₹24 LPA",
                icon = Icons.Default.AutoAwesome,
                onViewRoadmap = { onNavigateToRoadmap(state.recommendedCareerId) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: SKILL PROGRESS (Section 29)
            Text(
                text = "Skill Progress",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tokens.surface)
                    .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                if (state.skillBreakdown.isEmpty()) {
                    Text(
                        text = if (!state.isAssessmentCompleted) "Complete career assessment to unlock personalized skill tracking." else "No skill metrics available yet.",
                        color = tokens.textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        state.skillBreakdown.take(5).forEach { skill ->
                            CgaProgressBar(
                                progress = skill.percentage / 100f,
                                label = skill.skillName,
                                showPercentage = true,
                                progressColor = CGAColors.Primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: DAILY MISSION (Motivational daily challenge card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tokens.streakBg)
                    .border(2.dp, tokens.streakBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CGAColors.GamificationOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "DAILY MISSION",
                                color = tokens.streakText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TagPill(
                                text = "+30 XP",
                                backgroundColor = tokens.warningContainer,
                                textColor = tokens.onWarningContainer,
                                modifier = Modifier.wrapContentSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.currentStreak > 0) {
                                "Solve 3 Binary Search questions today to maintain your ${state.currentStreak}-day study streak!"
                            } else {
                                "Solve 3 Binary Search questions today to start your study streak!"
                            },
                            color = if (tokens.isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: AI MENTOR / CONSULTATION (Section 17)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alumni & Industry Mentors",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Consult",
                    color = tokens.onInfoContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MentorCard(
                    name = "Priya Raman",
                    role = "Senior ML Engineer",
                    company = "Google",
                    initials = "PR",
                    isGreenAvatar = false,
                    rating = "4.9 (124 reviews)"
                )

                MentorCard(
                    name = "Karthik Iyer",
                    role = "SDE II",
                    company = "Microsoft",
                    initials = "KI",
                    isGreenAvatar = true,
                    rating = "5.0 (89 reviews)"
                )

                MentorCard(
                    name = "Ananya Desai",
                    role = "Lead Data Scientist",
                    company = "Amazon",
                    initials = "AD",
                    isGreenAvatar = false,
                    rating = "4.8 (95 reviews)"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION: PLACEMENT TOOLS (Resume, Portfolio, Placement Admin)
            Text(
                text = "Placement Acceleration",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(132.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                        .clickable { onNavigateToResume() }
                        .padding(16.dp)
                        .testTag("resume_tool_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tokens.successContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Resume",
                                tint = tokens.onSuccessContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ATS Resume",
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Coming Soon",
                                color = CGAColors.Primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(132.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                        .clickable { onNavigateToPortfolio() }
                        .padding(16.dp)
                        .testTag("portfolio_tool_card")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CGAColors.LightPurpleIconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = "Portfolio",
                                tint = CGAColors.ElectricPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Portfolio",
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Add Projects",
                                color = tokens.textMuted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
