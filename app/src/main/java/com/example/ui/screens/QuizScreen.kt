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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.domain.model.PracticeConstants
import com.example.domain.model.QuizMode
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.StreakBadge
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.PracticeViewModel

@Composable
fun QuizScreen(
    practiceViewModel: PracticeViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        practiceViewModel.initializeQuizIfNeeded()
    }

    val state by practiceViewModel.quizState.collectAsState()
    val currentQuestion = state.questions.getOrNull(state.currentIndex)
    val tokens = LocalCGATokens.current

    Scaffold(
        topBar = {
            val title = if (state.mode == QuizMode.DAILY_SPEED_TEST) "Daily Speed Test" else "Speed Challenge Quiz"
            CgaTopBar(
                title = title,
                onBackClick = onNavigateBack,
                actions = {
                    StreakBadge(days = state.currentStreak)
                },
                testTag = "quiz_top_bar"
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        if (state.isQuizFinished) {
            // Completion Celebration
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .testTag("quiz_finish_screen"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(24.dp))
                        .padding(28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(CGAColors.GamificationOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Celebration",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val headerTitle = if (state.mode == QuizMode.DAILY_SPEED_TEST) {
                            "DAILY SPEED TEST COMPLETE!"
                        } else {
                            "CHALLENGE COMPLETE!"
                        }

                        Text(
                            text = headerTitle,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = tokens.textHeading
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "You answered ${state.score} of ${state.questions.size} questions correctly.",
                            color = tokens.textMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        val badgeText = if (state.mode == QuizMode.DAILY_SPEED_TEST && state.isAlreadyCompletedToday) {
                            "+0 XP (ALREADY COMPLETED TODAY)"
                        } else {
                            "+${state.xpEarned} XP EARNED"
                        }

                        TagPill(
                            text = badgeText,
                            backgroundColor = tokens.warningContainer,
                            textColor = tokens.onWarningContainer
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val primaryBtnText = if (state.mode == QuizMode.DAILY_SPEED_TEST) {
                            "RETRY DAILY SPEED TEST"
                        } else {
                            "START NEW QUIZ"
                        }

                        CgaPrimaryButton(
                            text = primaryBtnText,
                            onClick = {
                                if (state.mode == QuizMode.DAILY_SPEED_TEST) {
                                    practiceViewModel.startDailySpeedTest()
                                } else {
                                    practiceViewModel.startNewQuiz()
                                }
                            },
                            testTag = "quiz_start_new_btn"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CgaSecondaryButton(
                            text = "RETURN TO PRACTICE HUB",
                            onClick = onNavigateBack,
                            testTag = "quiz_finish_return_btn"
                        )
                    }
                }
            }
        } else if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("quiz_loading_screen"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = CGAColors.Primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading test questions...",
                        color = tokens.textMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (currentQuestion == null || state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
                    .testTag("quiz_empty_or_error_screen"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(tokens.surface)
                        .border(2.dp, tokens.borderMain, RoundedCornerShape(24.dp))
                        .padding(28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(tokens.warningContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Alert",
                                tint = tokens.onWarningContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Test Unavailable",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = tokens.textHeading
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.errorMessage ?: "Questions could not be loaded. Please check your connection and try again.",
                            color = tokens.textMuted,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CgaPrimaryButton(
                            text = "RETRY",
                            onClick = { practiceViewModel.retryQuiz() },
                            testTag = "quiz_retry_btn"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CgaSecondaryButton(
                            text = "RETURN TO PRACTICE HUB",
                            onClick = onNavigateBack,
                            testTag = "quiz_error_return_btn"
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("quiz_screen"),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUESTION ${state.currentIndex + 1} OF ${state.questions.size}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = CGAColors.ActionBlue,
                            letterSpacing = 0.5.sp
                        )

                        TagPill(
                            text = "SCORE: ${state.score}",
                            backgroundColor = tokens.infoContainer,
                            textColor = CGAColors.ActionBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val progress = (state.currentIndex + 1).toFloat() / state.questions.size.toFloat()
                    CgaProgressBar(progress = progress, showPercentage = false)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = currentQuestion.question,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Interactive option cards with dynamic validation feedback
                    currentQuestion.options.forEachIndexed { index, optionText ->
                        val isSelected = state.selectedOptionIndex == index
                        val optionLetter = ('A' + index).toString()

                        val isCorrectOption = index == currentQuestion.correctIndex
                        val borderColor = when {
                            state.isAnswerChecked && isCorrectOption -> CGAColors.SuccessGreen
                            state.isAnswerChecked && isSelected && !state.isCorrect -> CGAColors.Error
                            isSelected -> CGAColors.ActionBlue
                            else -> tokens.borderMain
                        }

                        val bgColor = when {
                            state.isAnswerChecked && isCorrectOption -> tokens.successContainer
                            state.isAnswerChecked && isSelected && !state.isCorrect -> tokens.warningContainer
                            isSelected -> tokens.infoContainer
                            else -> tokens.surface
                        }

                        val circleBg = when {
                            state.isAnswerChecked && isCorrectOption -> CGAColors.SuccessGreen
                            state.isAnswerChecked && isSelected && !state.isCorrect -> CGAColors.Error
                            isSelected -> CGAColors.ActionBlue
                            else -> tokens.progressTrack
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(bgColor)
                                .border(2.dp, borderColor, RoundedCornerShape(18.dp))
                                .clickable(enabled = !state.isAnswerChecked) {
                                    practiceViewModel.selectQuizOption(index)
                                }
                                .padding(16.dp)
                                .testTag("quiz_option_$index")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(circleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = optionLetter,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected || (state.isAnswerChecked && isCorrectOption)) Color.White else tokens.textHeading,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = optionText,
                                    color = tokens.textHeading,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Bottom Feedback Tray
                if (state.isAnswerChecked) {
                    val trayColor = if (state.isCorrect) tokens.successContainer else tokens.warningContainer
                    val titleColor = if (state.isCorrect) tokens.onSuccessContainer else tokens.onWarningContainer
                    val iconVector = if (state.isCorrect) Icons.Default.Check else Icons.Default.Close

                    Surface(
                        color = trayColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_feedback_tray")
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(titleColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (state.isCorrect) "Awesome! That's correct!" else "Correct Answer: ${currentQuestion.options[currentQuestion.correctIndex]}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = titleColor
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = currentQuestion.explanation,
                                color = tokens.textBody,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            CgaPrimaryButton(
                                text = if (state.currentIndex + 1 < state.questions.size) "CONTINUE" else "SEE FINAL RESULTS",
                                onClick = { practiceViewModel.nextQuizQuestion() },
                                testTag = "quiz_continue_btn"
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(20.dp)
                    ) {
                        CgaPrimaryButton(
                            text = "CHECK ANSWER",
                            onClick = { practiceViewModel.checkAnswer() },
                            enabled = state.selectedOptionIndex != null,
                            testTag = "quiz_check_btn"
                        )
                    }
                }
            }
        }
    }
}
