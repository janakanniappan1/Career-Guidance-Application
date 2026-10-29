package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.adaptive.ConfidenceLevel
import com.example.domain.model.adaptive.MasteryCategory
import com.example.domain.model.adaptive.TopicMastery
import com.example.ui.components.CgaButtonVariant
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.CGADesignTokens
import com.example.viewmodel.LearningCompanionUiState
import com.example.viewmodel.LearningCompanionViewModel

/**
 * My Mastery Dashboard View
 * Personalized topic-level competency breakdown using the Weighted Evidence Mastery Model.
 */
@Composable
fun MasteryDashboardView(
    state: LearningCompanionUiState,
    viewModel: LearningCompanionViewModel,
    tokens: CGADesignTokens,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.closeMasteryScreen()
    }

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "My Mastery",
                onBackClick = { viewModel.closeMasteryScreen() },
                testTag = "my_mastery_top_bar"
            )
        },
        containerColor = tokens.contentBackground,
        modifier = modifier.testTag("my_mastery_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surface)
                    .border(1.5.dp, tokens.borderMain, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CGAColors.LightPurpleIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = null,
                                    tint = CGAColors.ElectricPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Overall Competency",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.textHeading
                                )
                                Text(
                                    text = "Weighted Evidence Mastery Model",
                                    fontSize = 11.sp,
                                    color = tokens.textMuted
                                )
                            }
                        }

                        Text(
                            text = "${state.overallMastery}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (state.overallMastery >= 60) CGAColors.Primary else tokens.textHeading
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    CgaProgressBar(
                        progress = state.overallMastery / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!state.hasMasteryHistory || state.topicMasteries.isEmpty()) {
                // Empty State for New Student
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(tokens.surface)
                        .border(1.dp, tokens.borderMain, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CGAColors.LightPurpleIconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = CGAColors.ElectricPurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Your learning profile is being built.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textHeading,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "No mastery data yet. Take your first assessment to analyze your topic-specific strengths and gaps.",
                            fontSize = 12.sp,
                            color = tokens.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        CgaPrimaryButton(
                            text = "Take Your First Assessment",
                            onClick = {
                                viewModel.closeMasteryScreen()
                                viewModel.openAssessmentConfig()
                            },
                            leadingIcon = Icons.Default.Quiz,
                            variant = CgaButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Recommended Focus Section
                val recommended = state.recommendedTopicMastery
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(tokens.surface)
                        .border(1.5.dp, tokens.borderMain, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = CGAColors.GamificationOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "🎯 Recommended Focus",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tokens.textHeading
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (recommended != null) {
                            Text(
                                text = recommended.topic,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = tokens.textHeading
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your current mastery is ${recommended.masteryScore}%. Continue learning this topic.",
                                fontSize = 12.sp,
                                color = tokens.textMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CgaPrimaryButton(
                                    text = "Ask AI Tutor",
                                    onClick = { viewModel.askTutorFromMastery(recommended.topic) },
                                    leadingIcon = Icons.Default.Psychology,
                                    modifier = Modifier.weight(1f)
                                )

                                CgaSecondaryButton(
                                    text = "Practice Topic",
                                    onClick = { viewModel.practiceTopicFromMastery(recommended.topic) },
                                    leadingIcon = Icons.Default.Quiz,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Text(
                                text = "No immediate weak topic. Continue mixed practice.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = tokens.textHeading
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Topic Mastery Breakdown
                Text(
                    text = "Topic Competency Breakdown",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textHeading
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.topicMasteries.forEach { mastery ->
                        TopicMasteryCard(
                            mastery = mastery,
                            onPracticeClick = { viewModel.practiceTopicFromMastery(mastery.topic) },
                            onTutorClick = { viewModel.askTutorFromMastery(mastery.topic) },
                            tokens = tokens
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicMasteryCard(
    mastery: TopicMastery,
    onPracticeClick: () -> Unit,
    onTutorClick: () -> Unit,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.surface)
            .border(1.dp, tokens.borderMain, RoundedCornerShape(14.dp))
            .padding(16.dp)
            .testTag("topic_mastery_card_${mastery.topic}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mastery.topic,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textHeading
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge (Strong, Developing, Needs Practice)
                    val (badgeBg, badgeText) = when (mastery.category) {
                        MasteryCategory.STRONG -> tokens.successContainer to tokens.onSuccessContainer
                        MasteryCategory.DEVELOPING -> tokens.contentBackground to tokens.textHeading
                        MasteryCategory.NEEDS_PRACTICE -> tokens.warningContainer to tokens.onWarningContainer
                    }

                    TagPill(
                        text = mastery.category.displayName,
                        backgroundColor = badgeBg,
                        textColor = badgeText
                    )

                    // Confidence Level Pill
                    val confColor = when (mastery.confidenceLevel) {
                        ConfidenceLevel.HIGH -> tokens.onSuccessContainer
                        ConfidenceLevel.MEDIUM -> CGAColors.ElectricPurple
                        ConfidenceLevel.LOW -> tokens.onWarningContainer
                    }
                    TagPill(
                        text = "${mastery.confidenceLevel.displayName} Conf",
                        backgroundColor = tokens.contentBackground,
                        textColor = confColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score and Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${mastery.masteryScore}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = when (mastery.category) {
                        MasteryCategory.STRONG -> CGAColors.Primary
                        MasteryCategory.DEVELOPING -> CGAColors.ElectricPurple
                        MasteryCategory.NEEDS_PRACTICE -> CGAColors.GamificationOrange
                    }
                )

                Text(
                    text = "${mastery.questionsAttempted} questions • ${mastery.recentAccuracy}% recent accuracy",
                    fontSize = 11.sp,
                    color = tokens.textMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            CgaProgressBar(
                progress = mastery.masteryScore / 100f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CgaSecondaryButton(
                    text = "Practice",
                    onClick = onPracticeClick,
                    leadingIcon = Icons.Default.Quiz,
                    modifier = Modifier.padding(end = 8.dp)
                )

                CgaPrimaryButton(
                    text = "Ask Tutor",
                    onClick = onTutorClick,
                    leadingIcon = Icons.Default.Psychology
                )
            }
        }
    }
}
