package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
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
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SkillChip
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.CareerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareerDetailScreen(
    careerId: String,
    careerViewModel: CareerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRoadmap: (String) -> Unit
) {
    LaunchedEffect(careerId) {
        careerViewModel.selectCareerDetail(careerId)
    }

    val selectedCareer by careerViewModel.selectedCareer.collectAsState()
    val career = selectedCareer ?: careerViewModel.latestRecommendation.value
    val tokens = LocalCGATokens.current

    Scaffold(
        topBar = {
            CgaTopBar(
                title = career?.title ?: "Career Details",
                onBackClick = onNavigateBack,
                testTag = "career_details_top_bar"
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
                .testTag("career_detail_screen")
        ) {
            if (career != null) {
                Spacer(modifier = Modifier.height(16.dp))

                // Hero Career Match Card (24dp radius, 2dp border, 6dp bottom border)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.infoContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = tokens.onInfoContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            TagPill(
                                text = "${career.matchPercentage}% COMPATIBILITY",
                                backgroundColor = tokens.successContainer,
                                textColor = tokens.onSuccessContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = career.title,
                            color = tokens.textHeading,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = career.roleOverview.ifBlank {
                                "Design, implement, and optimize mission-critical software systems and intelligent data pipelines."
                            },
                            color = tokens.textBody,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats: Salary & Market Growth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "SALARY RANGE",
                        value = career.averageSalary,
                        icon = Icons.Default.AttachMoney,
                        accentColor = CGAColors.GamificationOrange,
                        modifier = Modifier.weight(1f),
                        testTag = "career_salary_card"
                    )
                    MetricStatCard(
                        title = "JOB DEMAND",
                        value = career.jobGrowth,
                        icon = Icons.Default.TrendingUp,
                        accentColor = CGAColors.ActionBlue,
                        modifier = Modifier.weight(1f),
                        testTag = "career_growth_card"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Core Skills Required
                Text(
                    text = "Core Competencies & Technologies",
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
                    career.recommendedSkills.forEach { skill ->
                        SkillChip(skill = skill, accentColor = CGAColors.Primary)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Campus Placement Strategy Note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.infoContainer)
                        .border(2.dp, tokens.onInfoContainer.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = tokens.onInfoContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "College Placement Strategy",
                                color = tokens.onInfoContainer,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Focus on building 2 standout end-to-end projects, practicing medium-level algorithmic coding, and maintaining a high-impact GitHub portfolio.",
                                color = if (tokens.isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                CgaPrimaryButton(
                    text = "START LEARNING PATH",
                    onClick = { onNavigateToRoadmap(career.careerId) },
                    testTag = "detail_open_roadmap_btn"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
