package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentQuestionType
import com.example.domain.model.adaptive.QuestionResult
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
 * Dialog to configure and generate a new source-grounded Adaptive Assessment.
 */
@Composable
fun AssessmentConfigDialog(
    state: LearningCompanionUiState,
    viewModel: LearningCompanionViewModel,
    tokens: CGADesignTokens
) {
    AlertDialog(
        onDismissRequest = {
            if (!state.isGeneratingAssessment) {
                viewModel.closeAssessmentConfig()
            }
        },
        containerColor = tokens.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CGAColors.LightPurpleIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = CGAColors.ElectricPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Adaptive Assessment",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textHeading
                    )
                    Text(
                        text = "Source-grounded concept evaluation",
                        fontSize = 11.sp,
                        color = tokens.textMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (state.isGeneratingAssessment) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = CGAColors.ElectricPurple,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.assessmentGenerationMessage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.textHeading,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Synthesizing questions from your Knowledge Base...",
                            fontSize = 11.sp,
                            color = tokens.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // 1. Select Material
                    Text(
                        text = "Select Learning Material",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textHeading
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.materials.forEach { material ->
                            val isSelected = state.selectedAssessmentMaterial?.id == material.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) CGAColors.Primary.copy(alpha = 0.12f) else tokens.contentBackground)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) CGAColors.Primary else tokens.borderMain,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.selectAssessmentMaterial(material) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (isSelected) CGAColors.Primary else tokens.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = material.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CGAColors.Primary else tokens.textHeading,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Select Topic Scope
                    Text(
                        text = "Topic Scope",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textHeading
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.availableAssessmentTopics.forEach { topic ->
                            val isSelected = state.selectedAssessmentTopic == topic
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CGAColors.ElectricPurple else tokens.contentBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CGAColors.ElectricPurple else tokens.borderMain,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectAssessmentTopic(topic) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = topic,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else tokens.textHeading
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Difficulty Level
                    Text(
                        text = "Difficulty",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textHeading
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssessmentDifficulty.entries.forEach { diff ->
                            val isSelected = state.selectedAssessmentDifficulty == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CGAColors.Primary else tokens.contentBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CGAColors.Primary else tokens.borderMain,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectAssessmentDifficulty(diff) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = diff.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else tokens.textHeading
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Number of Questions (5, 10)
                    Text(
                        text = "Question Count",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textHeading
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10).forEach { count ->
                            val isSelected = state.selectedQuestionCount == count
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CGAColors.ElectricPurple else tokens.contentBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CGAColors.ElectricPurple else tokens.borderMain,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.selectAssessmentQuestionCount(count) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$count Questions",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else tokens.textHeading
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!state.isGeneratingAssessment) {
                CgaPrimaryButton(
                    text = "Generate Assessment",
                    onClick = { viewModel.generateAssessment() },
                    leadingIcon = Icons.Default.AutoAwesome,
                    variant = CgaButtonVariant.Primary,
                    modifier = Modifier.testTag("generate_assessment_confirm_button")
                )
            }
        },
        dismissButton = {
            if (!state.isGeneratingAssessment) {
                CgaSecondaryButton(
                    text = "Cancel",
                    onClick = { viewModel.closeAssessmentConfig() }
                )
            }
        }
    )
}

/**
 * Active Quiz Screen for taking the Adaptive Assessment.
 * Strict rule: Does NOT reveal the correct answer prior to submission.
 */
@Composable
fun AdaptiveAssessmentQuizView(
    state: LearningCompanionUiState,
    viewModel: LearningCompanionViewModel,
    tokens: CGADesignTokens,
    modifier: Modifier = Modifier
) {
    val assessment = state.activeAssessment ?: return
    val questions = assessment.questions
    val currentIndex = state.currentQuestionIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
    val currentQuestion = questions.getOrNull(currentIndex) ?: return
    val selectedAnswer = state.studentAnswers[currentQuestion.id].orEmpty()

    BackHandler {
        viewModel.closeAssessmentQuiz()
    }

    Scaffold(
        topBar = {
            CgaTopBar(
                title = assessment.title,
                onBackClick = { viewModel.closeAssessmentQuiz() },
                testTag = "adaptive_assessment_quiz_top_bar"
            )
        },
        containerColor = tokens.contentBackground,
        modifier = modifier.testTag("adaptive_assessment_quiz_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Progress and Index Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${currentIndex + 1} of ${questions.size}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = tokens.textHeading
                )
                Text(
                    text = "${((currentIndex + 1) * 100) / questions.size}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CGAColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            CgaProgressBar(
                progress = (currentIndex + 1).toFloat() / questions.size.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Card: Topic & Difficulty
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tokens.surface)
                    .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Topic:",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                        TagPill(
                            text = currentQuestion.topic,
                            backgroundColor = CGAColors.LightPurpleIconBg,
                            textColor = CGAColors.ElectricPurple
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Difficulty:",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                        TagPill(
                            text = currentQuestion.difficulty.displayName,
                            backgroundColor = tokens.contentBackground,
                            textColor = tokens.textHeading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Question Text Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surface)
                    .border(1.5.dp, tokens.borderMain, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = currentQuestion.question,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textHeading,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Concept: ${currentQuestion.concept}",
                        fontSize = 11.sp,
                        color = tokens.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Options / Answer Input Section
            if (currentQuestion.type == AssessmentQuestionType.MCQ) {
                Text(
                    text = "Select an option:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.textHeading
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val letters = listOf("A", "B", "C", "D")
                    currentQuestion.options.forEachIndexed { idx, option ->
                        val isSelected = selectedAnswer.equals(option, ignoreCase = true)
                        val letter = letters.getOrElse(idx) { "${idx + 1}" }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CGAColors.Primary.copy(alpha = 0.08f) else tokens.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CGAColors.Primary else tokens.borderMain,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.selectAnswer(currentQuestion.id, option)
                                }
                                .padding(14.dp)
                                .testTag("mcq_option_${currentQuestion.id}_$idx")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) CGAColors.Primary else tokens.contentBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else tokens.textHeading
                                    )
                                }

                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    color = if (isSelected) tokens.textHeading else tokens.textHeading.copy(alpha = 0.9f),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                // SHORT_ANSWER input
                Text(
                    text = "Type your answer:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.textHeading
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.currentShortAnswerInput,
                    onValueChange = { viewModel.updateShortAnswerInput(it) },
                    placeholder = {
                        Text(
                            text = "Enter concise explanation or definition...",
                            fontSize = 13.sp,
                            color = tokens.textMuted
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CGAColors.Primary,
                        unfocusedBorderColor = tokens.borderMain,
                        focusedContainerColor = tokens.surface,
                        unfocusedContainerColor = tokens.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("short_answer_input_field")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Navigation Buttons (Previous, Next, Submit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentIndex > 0) {
                    CgaSecondaryButton(
                        text = "Previous",
                        onClick = { viewModel.previousQuestion() },
                        leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiz_previous_button")
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (currentIndex < questions.size - 1) {
                    CgaPrimaryButton(
                        text = "Next",
                        onClick = { viewModel.nextQuestion() },
                        leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiz_next_button")
                    )
                } else {
                    CgaPrimaryButton(
                        text = "Submit Assessment",
                        onClick = { viewModel.submitAssessment() },
                        leadingIcon = Icons.Default.CheckCircle,
                        variant = CgaButtonVariant.Primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quiz_submit_button")
                    )
                }
            }
        }
    }
}

/**
 * Assessment Results Screen:
 * - Overall Score & Percentage
 * - Topic Performance Breakdown
 * - Weak-Topic Analysis (< 60%)
 * - Personalized Next Action ("Review [Topic]", "Ask AI Tutor", "Practice Weak Topic")
 * - Question-by-Question Review with Clickable Page Citations
 */
@Composable
fun AdaptiveAssessmentResultView(
    state: LearningCompanionUiState,
    viewModel: LearningCompanionViewModel,
    tokens: CGADesignTokens,
    modifier: Modifier = Modifier
) {
    val result = state.assessmentResult ?: return

    BackHandler {
        viewModel.closeAssessmentResult()
    }

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Assessment Results",
                onBackClick = { viewModel.closeAssessmentResult() },
                testTag = "adaptive_assessment_result_top_bar"
            )
        },
        containerColor = tokens.contentBackground,
        modifier = modifier.testTag("adaptive_assessment_result_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Score Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.surface)
                    .border(1.5.dp, tokens.borderMain, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (result.scorePercentage >= 70) CGAColors.LightPurpleIconBg else CGAColors.LightPink
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (result.scorePercentage >= 70) Icons.Default.School else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (result.scorePercentage >= 70) CGAColors.ElectricPurple else CGAColors.GamificationOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Assessment Complete",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.textHeading
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Score: ${result.correctAnswers} / ${result.totalQuestions}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CGAColors.Primary
                    )

                    Text(
                        text = "${result.scorePercentage}%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = tokens.textHeading
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    CgaProgressBar(
                        progress = result.scorePercentage / 100f,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Topic Performance Breakdown
            Text(
                text = "Topic Performance",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textHeading
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.topicPerformance.forEach { topicPerf ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.surface)
                            .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topicPerf.topic,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tokens.textHeading
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${topicPerf.correctAnswers} / ${topicPerf.totalQuestions} correct",
                                    fontSize = 11.sp,
                                    color = tokens.textMuted
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${topicPerf.accuracyPercentage}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (topicPerf.isWeak) CGAColors.GamificationOrange else CGAColors.ElectricPurple
                                )

                                TagPill(
                                    text = if (topicPerf.isWeak) "Weak (<60%)" else "Proficient",
                                    backgroundColor = if (topicPerf.isWeak) tokens.warningContainer else tokens.successContainer,
                                    textColor = if (topicPerf.isWeak) tokens.onWarningContainer else tokens.onSuccessContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Weak Topics & Recommended Next Step Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (result.hasWeakTopics) tokens.warningContainer.copy(alpha = 0.4f) else tokens.successContainer.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        if (result.hasWeakTopics) tokens.onWarningContainer.copy(alpha = 0.3f) else tokens.onSuccessContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (result.hasWeakTopics) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (result.hasWeakTopics) tokens.onWarningContainer else tokens.onSuccessContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Recommended Next Step",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.textHeading
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (result.hasWeakTopics) {
                        val firstWeak = result.weakTopics.first()
                        Text(
                            text = "Review $firstWeak using your source material.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.textHeading
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CgaPrimaryButton(
                                text = "Ask AI Tutor",
                                onClick = { viewModel.askTutorAboutWeakTopic(firstWeak) },
                                leadingIcon = Icons.Default.Psychology,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ask_tutor_weak_topic_button")
                            )

                            CgaSecondaryButton(
                                text = "Practice Weak Topic",
                                onClick = { viewModel.practiceWeakTopic(firstWeak) },
                                leadingIcon = Icons.Default.Quiz,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("practice_weak_topic_button")
                            )
                        }
                    } else {
                        Text(
                            text = "Great! No major weak topics detected.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.textHeading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question-by-Question Review with Source Citations
            Text(
                text = "Question Review",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textHeading
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                result.questionResults.forEachIndexed { idx, qResult ->
                    QuestionFeedbackCard(
                        index = idx + 1,
                        result = qResult,
                        onSourceClick = { viewModel.openQuestionResultSource(qResult) },
                        tokens = tokens
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Actions
            CgaSecondaryButton(
                text = "Back to Learning Companion",
                onClick = { viewModel.closeAssessmentResult() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("close_assessment_result_button")
            )
        }
    }
}

@Composable
private fun QuestionFeedbackCard(
    index: Int,
    result: QuestionResult,
    onSourceClick: () -> Unit,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.surface)
            .border(
                width = 1.dp,
                color = if (result.correct) tokens.successContainer else tokens.warningContainer,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
            .testTag("question_feedback_card_$index")
    ) {
        Column {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (result.correct) "✅ Correct" else "❌ Incorrect",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (result.correct) tokens.onSuccessContainer else tokens.onWarningContainer
                )

                TagPill(
                    text = result.topic,
                    backgroundColor = tokens.contentBackground,
                    textColor = tokens.textHeading
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question
            Text(
                text = "Q$index: ${result.questionText}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tokens.textHeading
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Selected vs Correct Answer
            if (!result.correct) {
                Text(
                    text = "Your answer: ${result.selectedAnswer}",
                    fontSize = 12.sp,
                    color = tokens.textMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = "Correct answer: ${result.correctAnswer}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (result.correct) tokens.textHeading else CGAColors.Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation
            Text(
                text = "Explanation: ${result.explanation}",
                fontSize = 12.sp,
                color = tokens.textHeading,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Clickable Source Citation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(tokens.contentBackground)
                    .border(1.dp, tokens.borderMain, RoundedCornerShape(8.dp))
                    .clickable { onSourceClick() }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("feedback_source_citation_$index"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = CGAColors.ElectricPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "📚 Source: ${result.sourceCitation}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.textHeading,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "View Source >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CGAColors.Primary
                )
            }
        }
    }
}
