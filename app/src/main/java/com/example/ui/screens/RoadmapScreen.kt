package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RoadmapStage
import com.example.domain.model.RoadmapTask
import com.example.domain.model.SkillProgressItem
import com.example.ui.components.CgaBottomNavigation
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaTopBar
import com.example.ui.components.RoadmapItem
import com.example.ui.components.StreakBadge
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AiRoadmapGuidanceUiState
import com.example.viewmodel.RoadmapViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoadmapScreen(
    careerId: String? = null,
    roadmapViewModel: RoadmapViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAssessment: () -> Unit = {}
) {
    LaunchedEffect(careerId) {
        roadmapViewModel.loadRoadmapForCareer(careerId)
    }

    val state by roadmapViewModel.uiState.collectAsState()
    val aiGuidanceState by roadmapViewModel.aiGuidanceState.collectAsState()
    val roadmap = state.roadmap
    val tokens = LocalCGATokens.current

    val expandedMap = remember {
        mutableStateMapOf(
            "stage_1" to true,
            "stage_2" to true,
            "s1" to true,
            "s2" to true,
            "da1" to true,
            "da2" to true
        )
    }

    Scaffold(
        topBar = {
            val topBarTitle = roadmap?.careerTitle?.let { "$it Roadmap" }
                ?: if (state.activeCareerId.isNotBlank()) "${com.example.util.CareerCanonicalUtil.toCanonicalCareerTitle(state.activeCareerId)} Roadmap"
                else "Career Roadmap"

            CgaTopBar(
                title = topBarTitle,
                onBackClick = onNavigateBack,
                testTag = "roadmap_top_bar",
                actions = {
                    StreakBadge(days = state.currentStreak)
                }
            )
        },
        bottomBar = {
            CgaBottomNavigation(
                currentRoute = "roadmap",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> onNavigateToHome()
                        "roadmap" -> { /* Current */ }
                        "practice" -> onNavigateToPractice()
                        "explore" -> onNavigateToExplore()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .testTag("roadmap_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Streak connection card (Section 27: Complete today to maintain streak)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.streakBg)
                        .border(2.dp, tokens.streakBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CGAColors.GamificationOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DAILY STREAK TARGET",
                                color = tokens.streakText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (state.currentStreak > 0) {
                                    "Complete 1 roadmap checkpoint today to maintain your ${state.currentStreak}-day streak!"
                                } else {
                                    "Complete 1 roadmap checkpoint today to start your daily streak!"
                                },
                                color = if (tokens.isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Overall Roadmap Progress Card (Hero Style: 24dp radius, 2dp border, 6dp bottom border)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                        .testTag("overall_progress_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TagPill(
                                text = "PATH PROGRESS",
                                backgroundColor = tokens.infoContainer,
                                textColor = tokens.onInfoContainer
                            )

                            Text(
                                text = "${state.overallProgress}% DONE",
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = roadmap?.careerTitle
                                ?: (if (state.activeCareerId.isNotBlank()) com.example.util.CareerCanonicalUtil.toCanonicalCareerTitle(state.activeCareerId) else "Personalized Career Path"),
                            color = tokens.textHeading,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Interactive Path: Tap checkpoints to complete milestones, master skills, and prepare for campus placement interviews.",
                            color = tokens.textBody,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CgaProgressBar(
                            progress = state.overallProgress / 100f,
                            label = "Curriculum Completion",
                            showPercentage = true,
                            progressColor = CGAColors.Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Personalized Roadmap Guidance Card (AI Feature 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(1.5.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                        .padding(18.dp)
                        .testTag("ai_roadmap_guidance_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CGAColors.LightPurpleIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = CGAColors.ElectricPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "Personalized Roadmap Guidance",
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            TagPill(
                                text = "AI ADVISOR",
                                backgroundColor = CGAColors.LightPurpleIconBg,
                                textColor = CGAColors.ElectricPurple
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (val aiState = aiGuidanceState) {
                            is AiRoadmapGuidanceUiState.Loading,
                            is AiRoadmapGuidanceUiState.Idle -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .testTag("ai_roadmap_guidance_loading")
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.2.dp,
                                        color = CGAColors.ElectricPurple
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Analyzing your roadmap progress...",
                                        color = tokens.textMuted,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            is AiRoadmapGuidanceUiState.Success -> {
                                Text(
                                    text = aiState.guidance.guidance,
                                    color = tokens.textBody,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.5.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.testTag("ai_roadmap_guidance_text")
                                )
                            }
                            is AiRoadmapGuidanceUiState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_roadmap_guidance_error")
                                ) {
                                    Text(
                                        text = "Personalized guidance is temporarily unavailable.",
                                        color = tokens.textMuted,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { roadmapViewModel.retryAiGuidance() }
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                            .testTag("ai_roadmap_guidance_retry_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Try Again",
                                            tint = CGAColors.ElectricPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Try Again",
                                            color = CGAColors.ElectricPurple,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Live Skill Readiness Badges
                Text(
                    text = "Skill Readiness Matrix",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.skillBreakdown.forEach { skillItem ->
                        SkillProgressCard(skill = skillItem)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Milestone Stages",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Unassessed State / Assessment Required Notice
            if (state.isUnassessed) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surface)
                            .border(1.5.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                            .testTag("roadmap_unassessed_card")
                    ) {
                        Column {
                            Text(
                                text = "Career Assessment Required",
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Complete your career assessment to identify your recommended career path and unlock your personalized curriculum roadmap.",
                                color = tokens.textBody,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToAssessment,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CGAColors.Primary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("roadmap_start_assessment_button")
                            ) {
                                Text(
                                    text = "START CAREER ASSESSMENT",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Error Notice with Retry
            if (state.error != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surface)
                            .border(1.5.dp, CGAColors.GamificationOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(18.dp)
                            .testTag("roadmap_error_card")
                    ) {
                        Column {
                            Text(
                                text = "Roadmap Unavailable",
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.error ?: "Unable to load roadmap from Supabase.",
                                color = tokens.textBody,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        roadmapViewModel.loadRoadmapForCareer(state.activeCareerId, forceReload = true)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .testTag("roadmap_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Try Again",
                                    tint = CGAColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Try Again",
                                    color = CGAColors.Primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Loading Indicator
            if (state.isLoading && roadmap == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = CGAColors.Primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Loading roadmap from Supabase...",
                                color = tokens.textMuted,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }

            // Roadmap Stages
            if (roadmap != null) {
                items(roadmap.stages, key = { it.id }) { stage ->
                    val isExpanded = expandedMap[stage.id] ?: (stage.order <= 2)
                    StageAccordionCard(
                        stage = stage,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedMap[stage.id] = !isExpanded
                        },
                        onToggleTask = { taskId ->
                            roadmapViewModel.toggleTask(stage.id, taskId)
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun SkillProgressCard(
    skill: SkillProgressItem
) {
    val tokens = LocalCGATokens.current

    // Data-driven status color based on skill completion state
    val statusColor = when {
        skill.totalCount > 0 && (skill.percentage >= 100 || skill.completedCount >= skill.totalCount) -> Color(0xFF58CC02)
        skill.completedCount > 0 -> Color(0xFFF59E0B)
        else -> Color(0xFF2563EB)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("skill_badge_${skill.skillName.lowercase().replace(' ', '_')}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = skill.skillName,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
                Text(
                    text = "${skill.percentage}% (${skill.completedCount}/${skill.totalCount})",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StageAccordionCard(
    stage: RoadmapStage,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleTask: (String) -> Unit
) {
    val isCompleted = stage.progressPercentage == 100
    val tokens = LocalCGATokens.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, if (isCompleted) CGAColors.Primary else tokens.borderMain, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("stage_card_${stage.id}")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) CGAColors.LightGreenIconBg
                                else tokens.surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CGAColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stage.stageName,
                            color = tokens.textHeading,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = stage.subtitle,
                            color = tokens.textMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagPill(
                        text = "${stage.progressPercentage}%",
                        backgroundColor = if (isCompleted) CGAColors.LightGreenIconBg else tokens.infoContainer,
                        textColor = if (isCompleted) CGAColors.Primary else tokens.onInfoContainer
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = tokens.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CgaProgressBar(
                progress = stage.progressPercentage / 100f,
                showPercentage = false,
                progressColor = CGAColors.Primary
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    stage.tasks.forEach { task ->
                        RoadmapItem(
                            title = task.title,
                            description = "${task.description} • ~${task.estimatedHours} hrs",
                            isCompleted = task.isCompleted,
                            isCurrent = !task.isCompleted,
                            isLocked = false,
                            onToggle = { onToggleTask(task.id) },
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
