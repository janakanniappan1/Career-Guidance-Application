package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AssessmentOption
import com.example.domain.model.CareerRecommendation
import com.example.ui.components.AssessmentOption as CgaAssessmentOption
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.AssessmentViewModel

@Composable
fun AssessmentScreen(
    assessmentViewModel: AssessmentViewModel,
    onNavigateBack: () -> Unit,
    onAssessmentComplete: (CareerRecommendation) -> Unit
) {
    val state by assessmentViewModel.uiState.collectAsState()
    val currentQuestion = state.currentQuestion
    val tokens = LocalCGATokens.current

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Career Assessment",
                onBackClick = onNavigateBack,
                testTag = "assessment_top_bar"
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        if (state.isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CGAColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing your responses...",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = tokens.textHeading
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Computing career compatibility scores",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = tokens.textMuted
                    )
                }
            }
        } else if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CGAColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading assessment questions...",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = tokens.textHeading
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Fetching career evaluation modules",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = tokens.textMuted
                    )
                }
            }
        } else if (state.error != null && currentQuestion == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.error ?: "Failed to load assessment questions.",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CgaPrimaryButton(
                        text = "RETRY",
                        onClick = { assessmentViewModel.retryLoadingQuestions() },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        testTag = "assessment_retry_button"
                    )
                }
            }
        } else if (currentQuestion != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("assessment_screen"),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Question progress & category tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUESTION ${state.currentQuestionIndex + 1} OF ${state.totalQuestions}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = CGAColors.ActionBlue,
                            letterSpacing = 0.5.sp
                        )

                        TagPill(
                            text = currentQuestion.category.uppercase(),
                            backgroundColor = tokens.purpleContainer,
                            textColor = CGAColors.ElectricPurple
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val progressFraction = (state.currentQuestionIndex + 1).toFloat() / state.totalQuestions.toFloat()
                    CgaProgressBar(
                        progress = progressFraction,
                        showPercentage = false
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    // Question Text
                    Text(
                        text = currentQuestion.questionText,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Tactile Answer Options
                    currentQuestion.options.forEach { option ->
                        val isSelected = state.selectedOptionId == option.id
                        CgaAssessmentOption(
                            text = option.text,
                            isSelected = isSelected,
                            onClick = { assessmentViewModel.selectOption(option.id) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                // Bottom Action Buttons
                Column(modifier = Modifier.padding(top = 28.dp, bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.canGoBack) {
                            CgaSecondaryButton(
                                text = "BACK",
                                onClick = { assessmentViewModel.previousQuestion() },
                                modifier = Modifier.weight(1f),
                                testTag = "assessment_back_button"
                            )
                        }

                        if (state.isLastQuestion) {
                            CgaPrimaryButton(
                                text = "SUBMIT",
                                onClick = {
                                    assessmentViewModel.submitAssessment { recommendation ->
                                        onAssessmentComplete(recommendation)
                                    }
                                },
                                enabled = state.canGoNext,
                                modifier = Modifier.weight(if (state.canGoBack) 2f else 1f),
                                testTag = "assessment_submit_button"
                            )
                        } else {
                            CgaPrimaryButton(
                                text = "CONTINUE",
                                onClick = { assessmentViewModel.nextQuestion() },
                                enabled = state.canGoNext,
                                modifier = Modifier.weight(if (state.canGoBack) 2f else 1f),
                                testTag = "assessment_next_button"
                            )
                        }
                    }
                }
            }
        }
    }
}
