package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CareerRecommendation
import com.example.domain.model.OtherCareerMatch
import com.example.ui.components.CgaButtonVariant
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.SkillChip
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AiGuidanceUiState
import com.example.viewmodel.AssessmentResultUiState
import com.example.viewmodel.CareerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssessmentResultScreen(
    careerViewModel: CareerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRoadmap: (String) -> Unit,
    onNavigateToCareerDetails: (String) -> Unit
) {
    val uiState by careerViewModel.assessmentResultUiState.collectAsState()
    val fallbackRecommendation by careerViewModel.latestRecommendation.collectAsState()
    val effectiveRecommendation = (uiState as? AssessmentResultUiState.Success)?.recommendation ?: fallbackRecommendation

    val aiGuidanceState by careerViewModel.aiGuidanceState.collectAsState()
    val tokens = LocalCGATokens.current

    // Trigger loading if no recommendation is present
    LaunchedEffect(Unit) {
        careerViewModel.loadAssessmentResult()
    }

    // Request AI personalized guidance for the recommended career (cached per careerId, does not re-request on recompositions)
    LaunchedEffect(effectiveRecommendation?.careerId) {
        effectiveRecommendation?.let { careerViewModel.loadAiGuidance(it) }
    }

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Assessment Results",
                onBackClick = onNavigateBack,
                testTag = "results_top_bar"
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        when {
            effectiveRecommendation != null -> {
                val recommendation = effectiveRecommendation
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .testTag("assessment_results_screen")
                ) {
            // Congratulatory Header with Badge
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
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CGAColors.GamificationOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "ASSESSMENT COMPLETED! +100 XP",
                            color = tokens.streakText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.testTag("assessment_xp_banner")
                        )
                        Text(
                            text = "AI-Driven Career Compatibility Analysis",
                            color = tokens.textHeading,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Career Match Card (24dp radius, 2dp border, 6dp bottom border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(tokens.surface)
                    .border(2.dp, tokens.borderMain, RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .testTag("primary_recommendation_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TagPill(
                            text = "TOP CAREER MATCH",
                            backgroundColor = tokens.successContainer,
                            textColor = tokens.onSuccessContainer
                        )

                        TagPill(
                            text = "${recommendation.matchPercentage}% MATCH",
                            backgroundColor = tokens.infoContainer,
                            textColor = tokens.onInfoContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = recommendation.title,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = recommendation.reasonForRecommendation,
                        color = tokens.textBody,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CgaProgressBar(
                        progress = recommendation.matchPercentage / 100f,
                        label = "Compatibility Score",
                        showPercentage = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // AI-Powered Personalized Guidance Section (Small, polished addition adhering to CGA Theme)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(tokens.surface)
                    .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                    .padding(18.dp)
                    .testTag("ai_guidance_card")
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
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
                            text = "Personalized Guidance",
                            color = tokens.textHeading,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        TagPill(
                            text = "AI INSIGHT",
                            backgroundColor = CGAColors.LightPurpleIconBg,
                            textColor = CGAColors.ElectricPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = aiGuidanceState) {
                        is AiGuidanceUiState.Loading,
                        is AiGuidanceUiState.Idle -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("ai_guidance_loading")
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.2.dp,
                                    color = CGAColors.ElectricPurple
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Preparing your personalized guidance...",
                                    color = tokens.textMuted,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        is AiGuidanceUiState.Success -> {
                            Text(
                                text = state.guidance.displayGuidance,
                                color = tokens.textBody,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.5.sp,
                                lineHeight = 21.sp,
                                modifier = Modifier.testTag("ai_guidance_explanation")
                            )
                        }
                        is AiGuidanceUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_guidance_error")
                            ) {
                                Text(
                                    text = "Personalized guidance is temporarily unavailable. Your career recommendation is still available.",
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
                                        .clickable { careerViewModel.retryAiGuidance(recommendation) }
                                        .padding(horizontal = 4.dp, vertical = 4.dp)
                                        .testTag("ai_guidance_retry_button")
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

            Spacer(modifier = Modifier.height(24.dp))

            // Skills Breakdown Section
            Text(
                text = "Skill Breakdown & Fit",
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
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CgaProgressBar(progress = 0.94f, label = "Problem Decomposition & Logic", showPercentage = true, progressColor = CGAColors.Primary)
                    CgaProgressBar(progress = 0.88f, label = "Algorithms & Mathematical Modeling", showPercentage = true, progressColor = CGAColors.Primary)
                    CgaProgressBar(progress = 0.82f, label = "Python & System Scripting", showPercentage = true, progressColor = CGAColors.Primary)
                    CgaProgressBar(progress = 0.76f, label = "Data Wrangling & Statistical Intuition", showPercentage = true, progressColor = CGAColors.Primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recommended Skills Tags
            Text(
                text = "Key Competencies to Master",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recommendation.recommendedSkills.forEach { skill ->
                    SkillChip(skill = skill, accentColor = CGAColors.Primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Other Compatible Careers
            Text(
                text = "Alternative Career Matches",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            recommendation.otherSuitableCareers.forEach { match ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(18.dp))
                        .clickable { onNavigateToCareerDetails(match.careerId) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = match.title,
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "High placement demand",
                                color = tokens.textMuted,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TagPill(
                                text = "${match.matchPercentage}%",
                                backgroundColor = tokens.infoContainer,
                                textColor = tokens.onInfoContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = tokens.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTAs (Section 26)
            CgaPrimaryButton(
                text = "EXPLORE ROADMAP",
                onClick = { onNavigateToRoadmap(recommendation.careerId) },
                variant = CgaButtonVariant.Blue,
                testTag = "results_explore_roadmap_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

            CgaSecondaryButton(
                text = "RETAKE ASSESSMENT",
                onClick = onNavigateBack,
                testTag = "results_retake_button"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    uiState is AssessmentResultUiState.Error -> {
        val errorMsg = (uiState as AssessmentResultUiState.Error).message
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("results_error_container")
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(tokens.warningContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = tokens.onWarningContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Unable to Load Recommendation",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = tokens.textHeading
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg,
                    fontSize = 14.sp,
                    color = tokens.textMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                CgaPrimaryButton(
                    text = "RETRY",
                    onClick = { careerViewModel.retryLoadAssessmentResult() },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    testTag = "results_retry_button"
                )
                Spacer(modifier = Modifier.height(12.dp))
                CgaSecondaryButton(
                    text = "BACK TO DASHBOARD",
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    testTag = "results_error_back_button"
                )
            }
        }
    }
    uiState is AssessmentResultUiState.Empty -> {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("results_empty_container")
            ) {
                Text(
                    text = "No Assessment Found",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = tokens.textHeading
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Complete the 15-question career assessment to discover your personalized career recommendation.",
                    fontSize = 14.sp,
                    color = tokens.textMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                CgaPrimaryButton(
                    text = "BACK TO DASHBOARD",
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    testTag = "results_empty_back_button"
                )
            }
        }
    }
    else -> {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.testTag("results_loading_container")
            ) {
                CircularProgressIndicator(color = CGAColors.Primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading career recommendation...",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = tokens.textHeading
                )
            }
        }
    }
}
}
}
