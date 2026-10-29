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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaTopBar
import com.example.ui.components.SkillChip
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.PracticeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumeScreen(
    practiceViewModel: PracticeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by practiceViewModel.uiState.collectAsState()
    val analysis = uiState.resumeAnalysis
    var isScanning by remember { mutableStateOf(false) }
    var scanSuccessMessage by remember { mutableStateOf<String?>(null) }
    val tokens = LocalCGATokens.current

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "ATS Resume Analyzer",
                onBackClick = onNavigateBack,
                testTag = "resume_top_bar"
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
                .testTag("resume_screen")
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Score Banner Card (24dp radius, 2dp border, 6dp bottom border)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(tokens.surface)
                    .border(2.dp, tokens.borderMain, RoundedCornerShape(24.dp))
                    .padding(20.dp)
                    .testTag("ats_score_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TagPill(
                            text = "ATS PARSER SCORE",
                            backgroundColor = tokens.successContainer,
                            textColor = tokens.onSuccessContainer
                        )

                        TagPill(
                            text = "${analysis.atsScore}/100",
                            backgroundColor = tokens.infoContainer,
                            textColor = tokens.onInfoContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Strong Candidate Match",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Target Role: ${analysis.targetCareer}",
                        color = tokens.onInfoContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CgaProgressBar(
                        progress = analysis.atsScore / 100f,
                        label = "Recruiter Keyword Match",
                        showPercentage = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Matched Keywords
            Text(
                text = "Matched Industry Keywords (${analysis.matchedSkills.size})",
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
                analysis.matchedSkills.forEach { skill ->
                    SkillChip(skill = "✓ $skill", accentColor = CGAColors.SuccessGreen)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Missing Keywords
            Text(
                text = "Missing High-Impact Keywords (${analysis.missingSkills.size})",
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
                analysis.missingSkills.forEach { skill ->
                    SkillChip(skill = "+ $skill", accentColor = CGAColors.GamificationOrange)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ATS Optimization Tips
            Text(
                text = "ATS Optimization Recommendations",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            analysis.tips.forEach { tip ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tokens.warningContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = tokens.onWarningContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = tip,
                            color = tokens.textHeading,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            CgaPrimaryButton(
                text = if (isScanning) "SCANNING RESUME..." else "UPLOAD & RE-SCAN RESUME",
                onClick = {
                    isScanning = true
                    scanSuccessMessage = "Resume re-analyzed: Score updated to 88/100!"
                    isScanning = false
                },
                leadingIcon = Icons.Default.UploadFile,
                testTag = "upload_resume_btn"
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
