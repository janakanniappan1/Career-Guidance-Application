package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DefaultPracticeCatalog
import com.example.domain.model.PracticeConstants
import com.example.domain.model.PracticeProblem
import com.example.ui.components.CgaButtonVariant
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.StreakBadge
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.PracticeViewModel

@Composable
fun PracticeProblemDetailScreen(
    problem: PracticeProblem,
    practiceViewModel: PracticeViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by practiceViewModel.uiState.collectAsState()
    val tokens = LocalCGATokens.current

    BackHandler {
        onNavigateBack()
    }

    // Resolve fallback to DefaultPracticeCatalog if problem options were not loaded from server
    val resolvedProblem = remember(problem, uiState.problems) {
        val inList = uiState.problems.find { it.id == problem.id }
            ?: uiState.problems.find { it.title.equals(problem.title, ignoreCase = true) }
        val base = inList ?: problem
        val catalogItem = DefaultPracticeCatalog.getProblemById(base.id)
            ?: DefaultPracticeCatalog.getProblemById(base.title)
            ?: DefaultPracticeCatalog.getProblemByQuery(base.title, base.description, base.topic)
        val isSolved = inList?.isSolved ?: problem.isSolved
        if (catalogItem != null) {
            catalogItem.copy(
                id = catalogItem.id,
                isSolved = isSolved,
                xpReward = if (base.xpReward > 0) base.xpReward else catalogItem.xpReward,
                title = if (base.title.isNotBlank()) base.title else catalogItem.title,
                category = if (base.category.isNotBlank()) base.category else catalogItem.category,
                difficulty = if (base.difficulty.isNotBlank()) base.difficulty else catalogItem.difficulty,
                description = if (base.description.isNotBlank()) base.description else catalogItem.description,
                options = if (base.options.size == 4) base.options else catalogItem.options,
                correctIndex = if (base.options.size == 4) base.correctIndex else catalogItem.correctIndex,
                hint = base.hint?.ifBlank { null } ?: catalogItem.hint,
                explanation = base.explanation?.ifBlank { null } ?: catalogItem.explanation,
                exampleInput = base.exampleInput?.ifBlank { null } ?: catalogItem.exampleInput,
                exampleOutput = base.exampleOutput?.ifBlank { null } ?: catalogItem.exampleOutput,
                constraints = base.constraints?.ifBlank { null } ?: catalogItem.constraints
            )
        } else {
            base.copy(
                id = base.id,
                isSolved = isSolved,
                xpReward = if (base.xpReward > 0) base.xpReward else 20
            )
        }
    }

    LaunchedEffect(resolvedProblem.id) {
        practiceViewModel.selectProblem(resolvedProblem)
    }

    var showHint by remember { mutableStateOf(false) }

    val difficultyColor = when (resolvedProblem.difficulty.lowercase()) {
        "easy" -> CGAColors.SuccessGreen
        "medium" -> CGAColors.GamificationOrange
        "hard" -> Color(0xFFEF4444)
        else -> tokens.textMuted
    }

    val isCoding = resolvedProblem.category.equals("Coding", ignoreCase = true)
    val isAptitude = resolvedProblem.category.equals("Aptitude", ignoreCase = true)
    val isTechnical = resolvedProblem.category.equals("Technical", ignoreCase = true)
    val isInterview = resolvedProblem.category.equals("Interview", ignoreCase = true)

    val detailsSectionTitle = when {
        isCoding -> "Example & Constraints"
        isAptitude -> "Key Details"
        isTechnical -> "Key Concept"
        isInterview -> "Scenario Details"
        else -> "Key Details"
    }

    val questionSectionTitle = when {
        isCoding -> "Problem Statement & Task"
        isAptitude -> "Aptitude Question"
        isTechnical -> "Technical Concept Question"
        isInterview -> "Interview Scenario"
        else -> "Question / Problem Statement"
    }

    val exampleContent = resolvedProblem.example ?: buildString {
        if (isCoding) {
            if (!resolvedProblem.exampleInput.isNullOrBlank()) {
                append("Input: ${resolvedProblem.exampleInput}\n")
            }
            if (!resolvedProblem.exampleOutput.isNullOrBlank()) {
                append("Output: ${resolvedProblem.exampleOutput}\n")
            }
            if (!resolvedProblem.constraints.isNullOrBlank()) {
                append("Constraints: ${resolvedProblem.constraints}")
            }
        } else {
            // For Aptitude / Technical / Interview: natural bullet points / readable text
            val lines = mutableListOf<String>()
            if (!resolvedProblem.exampleInput.isNullOrBlank()) {
                val inText = resolvedProblem.exampleInput
                if (inText.startsWith("•") || inText.startsWith("-")) {
                    lines.add(inText)
                } else {
                    lines.add("• $inText")
                }
            }
            if (!resolvedProblem.exampleOutput.isNullOrBlank()) {
                val outText = resolvedProblem.exampleOutput
                if (outText.startsWith("•") || outText.startsWith("-")) {
                    lines.add(outText)
                } else {
                    lines.add("• Objective: $outText")
                }
            }
            if (!resolvedProblem.constraints.isNullOrBlank()) {
                val conText = resolvedProblem.constraints
                if (conText.startsWith("•") || conText.startsWith("-")) {
                    lines.add(conText)
                } else {
                    lines.add("• Scope: $conText")
                }
            }
            append(lines.joinToString("\n"))
        }
    }.trim()

    val hintContent = resolvedProblem.hint?.trim()?.ifBlank { null }

    val explanationContent = resolvedProblem.explanation?.trim()?.ifBlank { null }
        ?: "Review the problem statement and options to understand the key concept."

    val options = resolvedProblem.options

    val isSolved = resolvedProblem.isSolved || uiState.problems.find { 
        it.id == resolvedProblem.id || it.title.equals(resolvedProblem.title, ignoreCase = true) 
    }?.isSolved == true

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Practice Problem",
                onBackClick = onNavigateBack,
                testTag = "problem_detail_top_bar",
                actions = {
                    StreakBadge(days = uiState.currentStreak)
                }
            )
        },
        containerColor = tokens.contentBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .testTag("practice_problem_detail_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(1.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TagPill(
                                    text = resolvedProblem.difficulty.uppercase(),
                                    backgroundColor = difficultyColor.copy(alpha = 0.15f),
                                    textColor = difficultyColor
                                )
                                TagPill(
                                    text = resolvedProblem.category,
                                    backgroundColor = tokens.streakBg,
                                    textColor = tokens.streakText
                                )
                            }

                            TagPill(
                                text = "+${resolvedProblem.xpReward} XP",
                                backgroundColor = tokens.warningContainer,
                                textColor = tokens.onWarningContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = resolvedProblem.title,
                            color = tokens.textHeading,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 26.sp
                        )

                        if (!resolvedProblem.topic.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Topic: ${resolvedProblem.topic}",
                                color = tokens.textMuted,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }

                        if (isSolved) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(tokens.successContainer)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Solved",
                                    tint = tokens.onSuccessContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Problem Solved (+${resolvedProblem.xpReward} XP)",
                                    color = tokens.onSuccessContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Problem Statement Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(1.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCoding) Icons.Default.Code else Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CGAColors.ActionBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = questionSectionTitle,
                                color = tokens.textHeading,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = resolvedProblem.description,
                            color = tokens.textBody,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Examples / Key Details Card
                if (exampleContent.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surface)
                            .border(1.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCoding) Icons.Default.Code else Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = CGAColors.GamificationOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = detailsSectionTitle,
                                    color = tokens.textHeading,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (tokens.isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = exampleContent,
                                    color = if (tokens.isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                    fontFamily = if (isCoding) FontFamily.Monospace else FontFamily.Default,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Hint Card (Expandable)
                if (!hintContent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surface)
                            .border(1.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                            .clickable { showHint = !showHint }
                            .padding(20.dp)
                            .testTag("toggle_problem_hint_btn")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = CGAColors.GamificationOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (showHint) "Hint (Tap to hide)" else "💡 Need a Hint? (Tap to reveal)",
                                        color = tokens.textHeading,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = showHint,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CGAColors.GamificationOrange.copy(alpha = 0.1f))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = hintContent,
                                            color = tokens.textBody,
                                            fontSize = 13.sp,
                                            lineHeight = 19.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MCQ Options Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(1.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Choose the Correct Answer",
                            color = tokens.textHeading,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Select one of the 4 options below and submit to test your understanding.",
                            color = tokens.textMuted,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        options.forEachIndexed { index, optionText ->
                            val isSelected = uiState.selectedOptionIndex == index
                            val optionLetter = ('A' + index).toString()

                            // Border and background state based on selection and submission result
                            val optionBorderColor = when {
                                uiState.problemSubmissionResult == true && isSelected -> CGAColors.SuccessGreen
                                uiState.problemSubmissionResult == false && isSelected -> Color(0xFFEF4444)
                                isSelected -> CGAColors.ActionBlue
                                else -> tokens.borderMain
                            }

                            val optionBgColor = when {
                                uiState.problemSubmissionResult == true && isSelected -> tokens.successContainer
                                uiState.problemSubmissionResult == false && isSelected -> if (tokens.isDark) Color(0xFF3B1219) else Color(0xFFFEF2F2)
                                isSelected -> tokens.infoContainer
                                else -> if (tokens.isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                            }

                            val badgeBgColor = when {
                                uiState.problemSubmissionResult == true && isSelected -> CGAColors.SuccessGreen
                                uiState.problemSubmissionResult == false && isSelected -> Color(0xFFEF4444)
                                isSelected -> CGAColors.ActionBlue
                                else -> tokens.borderMain.copy(alpha = 0.5f)
                            }

                            val optionTextColor = when {
                                uiState.problemSubmissionResult == false && isSelected -> if (tokens.isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
                                uiState.problemSubmissionResult == true && isSelected -> if (tokens.isDark) Color(0xFFBBF7D0) else Color(0xFF166534)
                                isSelected -> if (tokens.isDark) Color(0xFFBFDBFE) else Color(0xFF1E40AF)
                                else -> tokens.textHeading
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(optionBgColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = optionBorderColor,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        practiceViewModel.selectProblemOption(index)
                                    }
                                    .padding(14.dp)
                                    .testTag("problem_option_$index")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Option Letter Badge
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(badgeBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = optionLetter,
                                            color = if (isSelected) Color.White else tokens.textHeading,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = optionText,
                                        color = optionTextColor,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (uiState.problemSubmissionResult == false) Icons.Default.Close else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (uiState.problemSubmissionResult == false) Color(0xFFEF4444) else CGAColors.Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (index < options.lastIndex) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                // Feedback Banner (Shown after submission)
                if (uiState.problemSubmissionResult != null || !uiState.feedbackMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    val isCorrect = uiState.problemSubmissionResult == true
                    val isSaveFailed = uiState.isSaveFailed

                    val feedbackBg = when {
                        isCorrect && !isSaveFailed -> tokens.successContainer
                        isCorrect && isSaveFailed -> Color(0xFFFEF3C7).copy(alpha = 0.5f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.12f)
                    }
                    val feedbackTextColor = when {
                        isCorrect && !isSaveFailed -> tokens.onSuccessContainer
                        isCorrect && isSaveFailed -> Color(0xFFD97706)
                        else -> Color(0xFFDC2626)
                    }
                    val feedbackBorderColor = when {
                        isCorrect && !isSaveFailed -> tokens.onSuccessContainer.copy(alpha = 0.3f)
                        isCorrect && isSaveFailed -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.3f)
                    }
                    val feedbackIcon = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(feedbackBg)
                            .border(
                                width = 1.dp,
                                color = feedbackBorderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                            .testTag("submission_feedback_banner")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = feedbackIcon,
                                contentDescription = null,
                                tint = feedbackTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val isAlreadySolvedNotice = uiState.feedbackMessage?.contains("Already completed", ignoreCase = true) == true ||
                                        uiState.feedbackMessage?.contains("+0 XP") == true ||
                                        uiState.feedbackMessage?.contains("already solved", ignoreCase = true) == true

                                val bannerTitle = when {
                                    !isCorrect -> "Incorrect Answer (0 XP)"
                                    isSaveFailed -> "Correct Answer"
                                    isAlreadySolvedNotice -> "Correct! Already Solved (+0 XP)"
                                    else -> "Correct! +${resolvedProblem.xpReward} XP Earned"
                                }

                                Text(
                                    text = bannerTitle,
                                    color = feedbackTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.feedbackMessage
                                        ?: if (isCorrect && isSaveFailed) "Could not save your progress. Please try again."
                                        else if (isCorrect) "Great job! Your answer is correct."
                                        else "That's not quite right. Review the explanation or hint and try another option!",
                                    color = tokens.textBody,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Explanation Section (Revealed when solved or submitted)
                if (isSolved || uiState.problemSubmissionResult != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surface)
                            .border(1.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                            .padding(20.dp)
                            .testTag("problem_explanation_card")
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = CGAColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Explanation & Key Concept",
                                    color = tokens.textHeading,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = explanationContent,
                                color = tokens.textBody,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button: SUBMIT ANSWER / RETRY
                if (uiState.isSubmittingProblem) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CGAColors.Primary)
                    }
                } else {
                    val canSubmit = uiState.selectedOptionIndex != null
                    val isRetestState = isSolved || uiState.problemSubmissionResult == false
                    val buttonText = when {
                        uiState.isSaveFailed -> "RETRY SAVE"
                        isSolved && uiState.selectedOptionIndex == null -> "SOLVED • SELECT AN OPTION TO RETEST"
                        isSolved && uiState.selectedOptionIndex != null -> "RETEST"
                        uiState.problemSubmissionResult == false -> "RETEST"
                        canSubmit -> "SUBMIT ANSWER"
                        else -> "SELECT AN OPTION ABOVE"
                    }

                    CgaPrimaryButton(
                        text = buttonText,
                        enabled = canSubmit,
                        variant = if (isRetestState) CgaButtonVariant.Retest else CgaButtonVariant.Primary,
                        onClick = {
                            practiceViewModel.submitProblemAnswer(resolvedProblem.id)
                        },
                        testTag = "submit_problem_btn"
                    )
                }


                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
