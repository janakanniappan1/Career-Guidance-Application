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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaTopBar
import com.example.ui.components.MetricStatCard
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.PracticeViewModel

@Composable
fun AdminScreen(
    practiceViewModel: PracticeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by practiceViewModel.uiState.collectAsState()
    val adminData = uiState.adminOverview
    val tokens = LocalCGATokens.current

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Placement Cell Analytics",
                onBackClick = onNavigateBack,
                testTag = "admin_top_bar"
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .testTag("admin_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                TagPill(
                    text = "CAMPUS PLACEMENT HUB",
                    backgroundColor = tokens.infoContainer,
                    textColor = tokens.onInfoContainer
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Institutional Analytics",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Student assessment benchmarks & placement readiness metrics",
                    color = tokens.textMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Grid (Chunky 20dp radius, 2dp border, 4dp bottom border)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "TOTAL STUDENTS",
                        value = "${adminData.totalStudents}",
                        icon = Icons.Default.Group,
                        accentColor = CGAColors.ActionBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "PLACEMENT RATE",
                        value = "${adminData.placementRate}%",
                        icon = Icons.Default.TrendingUp,
                        accentColor = CGAColors.SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "AVG READINESS",
                        value = "${adminData.averageReadiness}%",
                        icon = Icons.Default.Assessment,
                        accentColor = CGAColors.GamificationOrange,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "ASSESSMENTS",
                        value = "${adminData.totalAssessmentsCompleted}",
                        icon = Icons.Default.School,
                        accentColor = CGAColors.ElectricPurple,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Career Distribution Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Student Career Choices",
                            color = tokens.textHeading,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        adminData.careerDistribution.forEach { (career, percentage) ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = career,
                                        color = tokens.textHeading,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "$percentage%",
                                        color = CGAColors.Primary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                CgaProgressBar(
                                    progress = percentage / 100f,
                                    showPercentage = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Department Breakdown
                Text(
                    text = "Department Readiness",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                DepartmentItem("Computer Science & Engineering", 88, 140, tokens)
                Spacer(modifier = Modifier.height(10.dp))
                DepartmentItem("Information Technology", 84, 115, tokens)
                Spacer(modifier = Modifier.height(10.dp))
                DepartmentItem("AI & Data Science", 91, 95, tokens)
                Spacer(modifier = Modifier.height(10.dp))
                DepartmentItem("Electronics & Communication", 79, 135, tokens)

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun DepartmentItem(dept: String, rate: Int, count: Int, tokens: com.example.ui.theme.CGATokens) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dept,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$count Registered Students",
                    color = tokens.textMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            TagPill(
                text = "$rate% PLACED",
                backgroundColor = CGAColors.LightGreenIconBg,
                textColor = CGAColors.Primary
            )
        }
    }
}
