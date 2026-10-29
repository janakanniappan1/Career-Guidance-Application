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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterList
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PracticeCategories
import com.example.domain.model.PracticeProblem
import com.example.ui.components.CgaBottomNavigation
import com.example.ui.components.CgaButtonVariant
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.StreakBadge
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.PracticeCompletionFilter
import com.example.viewmodel.PracticeViewModel

@Composable
fun PracticeScreen(
    practiceViewModel: PracticeViewModel,
    careerId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToRoadmap: (String) -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToQuiz: () -> Unit
) {
    val uiState by practiceViewModel.uiState.collectAsState()
    val tokens = LocalCGATokens.current

    LaunchedEffect(careerId) {
        practiceViewModel.loadData(careerId)
    }

    if (uiState.selectedProblem != null) {
        PracticeProblemDetailScreen(
            problem = uiState.selectedProblem!!,
            practiceViewModel = practiceViewModel,
            onNavigateBack = { practiceViewModel.clearSelectedProblem() }
        )
        return
    }

    val tabs = listOf("Coding (DSA)", "Aptitude", "Technical")
    val selectedTab = uiState.activeTab

    val categoryProblems = remember(uiState.problems, selectedTab) {
        val rawList = when (selectedTab) {
            0 -> uiState.problems.filter { PracticeCategories.isCoding(it.category) }
            1 -> uiState.problems.filter { PracticeCategories.isAptitude(it.category) }
            2 -> uiState.problems.filter { PracticeCategories.isTechnical(it.category) }
            else -> emptyList()
        }
        val seen = mutableSetOf<String>()
        rawList.filter { prob ->
            val key = prob.title.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
            seen.add(key)
        }
    }

    val totalCount = categoryProblems.size
    val completedCount = categoryProblems.count { it.isSolved }
    val incompleteCount = categoryProblems.count { !it.isSolved }

    val filteredProblems = remember(categoryProblems, uiState.completionFilter) {
        when (uiState.completionFilter) {
            PracticeCompletionFilter.ALL -> categoryProblems
            PracticeCompletionFilter.COMPLETED -> categoryProblems.filter { it.isSolved }
            PracticeCompletionFilter.INCOMPLETE -> categoryProblems.filter { !it.isSolved }
        }
    }

    val activeCategoryName = when (selectedTab) {
        0 -> "Coding"
        1 -> "Aptitude"
        2 -> "Technical"
        else -> "Technical"
    }

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Practice Hub",
                onBackClick = onNavigateBack,
                testTag = "practice_top_bar",
                actions = {
                    StreakBadge(days = uiState.currentStreak)
                }
            )
        },
        bottomBar = {
            CgaBottomNavigation(
                currentRoute = "practice",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> onNavigateToHome()
                        "roadmap" -> onNavigateToRoadmap("")
                        "practice" -> { /* Current */ }
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
                .testTag("practice_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Streak and XP banner (Section 30)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.streakBg)
                        .border(2.dp, tokens.streakBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CGAColors.GamificationOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "DAILY SPEED TEST",
                                    color = tokens.streakText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Daily Placement Speed Quiz",
                                    color = if (tokens.isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            TagPill(
                                text = if (uiState.isDailyTestCompletedToday) "COMPLETE" else "+50 XP",
                                backgroundColor = if (uiState.isDailyTestCompletedToday) CGAColors.LightGreenIconBg else tokens.warningContainer,
                                textColor = if (uiState.isDailyTestCompletedToday) CGAColors.Primary else tokens.onWarningContainer,
                                modifier = Modifier.wrapContentWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CgaPrimaryButton(
                            text = if (uiState.isDailyTestCompletedToday) "RETRY DAILY SPEED TEST" else "START DAILY SPEED TEST",
                            onClick = {
                                practiceViewModel.startDailySpeedTest()
                                onNavigateToQuiz()
                            },
                            variant = CgaButtonVariant.Reward,
                            testTag = "start_daily_quiz_btn"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Category Selector (DSA, Aptitude, Technical)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) tokens.infoContainer else tokens.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CGAColors.ActionBlue else tokens.borderMain,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    practiceViewModel.setActiveTab(index)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("practice_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title.uppercase(),
                                color = if (isSelected) CGAColors.ActionBlue else tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.3.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Completion Status Filter (All, Completed, Incomplete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterList = listOf(
                        Triple(PracticeCompletionFilter.ALL, "All", totalCount),
                        Triple(PracticeCompletionFilter.COMPLETED, "Completed", completedCount),
                        Triple(PracticeCompletionFilter.INCOMPLETE, "Incomplete", incompleteCount)
                    )

                    filterList.forEach { (filterOption, label, count) ->
                        val isFilterSelected = uiState.completionFilter == filterOption
                        val testTag = when (filterOption) {
                            PracticeCompletionFilter.ALL -> "practice_filter_all"
                            PracticeCompletionFilter.COMPLETED -> "practice_filter_completed"
                            PracticeCompletionFilter.INCOMPLETE -> "practice_filter_incomplete"
                        }

                        val activeColor = when (filterOption) {
                            PracticeCompletionFilter.COMPLETED -> CGAColors.SuccessGreen
                            PracticeCompletionFilter.INCOMPLETE -> CGAColors.GamificationOrange
                            PracticeCompletionFilter.ALL -> CGAColors.ActionBlue
                        }

                        val activeBg = when (filterOption) {
                            PracticeCompletionFilter.COMPLETED -> tokens.successContainer
                            PracticeCompletionFilter.INCOMPLETE -> tokens.warningContainer
                            PracticeCompletionFilter.ALL -> tokens.infoContainer
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isFilterSelected) activeBg else tokens.surface)
                                .border(
                                    width = if (isFilterSelected) 2.dp else 1.dp,
                                    color = if (isFilterSelected) activeColor else tokens.borderMain,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    practiceViewModel.setCompletionFilter(filterOption)
                                }
                                .padding(vertical = 9.dp, horizontal = 2.dp)
                                .testTag(testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Text(
                                    text = "${label.uppercase()} ($count)",
                                    color = if (isFilterSelected) activeColor else tokens.textHeading,
                                    fontWeight = if (isFilterSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontSize = if (label.length > 8 || count >= 100) 9.5.sp else 10.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeCategoryName Problem Set (${filteredProblems.size})",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$completedCount/$totalCount Solved",
                        color = tokens.textMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (filteredProblems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(tokens.surface)
                            .border(1.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                            .padding(28.dp)
                            .testTag("practice_empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.completionFilter == PracticeCompletionFilter.COMPLETED) tokens.infoContainer
                                        else tokens.successContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.completionFilter == PracticeCompletionFilter.COMPLETED) Icons.Default.FilterList
                                                  else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (uiState.completionFilter == PracticeCompletionFilter.COMPLETED) tokens.onInfoContainer
                                           else tokens.onSuccessContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = when (uiState.completionFilter) {
                                    PracticeCompletionFilter.COMPLETED -> "No completed problems yet."
                                    PracticeCompletionFilter.INCOMPLETE -> "No incomplete problems."
                                    else -> "No problems found."
                                },
                                color = tokens.textHeading,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = when (uiState.completionFilter) {
                                    PracticeCompletionFilter.COMPLETED -> "Solve problems in this category to track your completed practice here."
                                    PracticeCompletionFilter.INCOMPLETE -> "All problems in this category have been solved! Great job!"
                                    else -> "No problems available in this category."
                                },
                                color = tokens.textMuted,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )

                            if (uiState.completionFilter != PracticeCompletionFilter.ALL) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tokens.infoContainer)
                                        .clickable {
                                            practiceViewModel.setCompletionFilter(PracticeCompletionFilter.ALL)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("reset_filter_button")
                                ) {
                                    Text(
                                        text = "Show All ($totalCount)",
                                        color = tokens.onInfoContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(filteredProblems, key = { it.id }) { problem ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.surface)
                        .border(
                            2.dp,
                            if (problem.isSolved) CGAColors.Primary else tokens.borderMain,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { practiceViewModel.startProblem(problem.id) }
                        .padding(16.dp)
                        .testTag("practice_problem_${problem.id}")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (problem.isSolved) tokens.successContainer
                                            else tokens.infoContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (problem.isSolved) Icons.Default.Check else Icons.Default.Code,
                                        contentDescription = null,
                                        tint = if (problem.isSolved) tokens.onSuccessContainer else tokens.onInfoContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = problem.title,
                                        color = tokens.textHeading,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${problem.category} • ${problem.difficulty}",
                                        color = tokens.textMuted,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                if (problem.isSolved) {
                                    TagPill(
                                        text = "Complete",
                                        backgroundColor = tokens.successContainer,
                                        textColor = tokens.onSuccessContainer,
                                        modifier = Modifier.wrapContentWidth()
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                TagPill(
                                    text = "+${problem.xpReward} XP",
                                    backgroundColor = if (problem.isSolved) tokens.mutedSurface else tokens.warningContainer,
                                    textColor = if (problem.isSolved) tokens.textMuted else tokens.onWarningContainer,
                                    modifier = Modifier.wrapContentWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = problem.description,
                            color = tokens.textBody,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CgaPrimaryButton(
                            text = if (problem.isSolved) "COMPLETE" else "START PROBLEM",
                            leadingIcon = if (problem.isSolved) Icons.Default.Check else null,
                            onClick = { practiceViewModel.startProblem(problem.id) },
                            variant = if (problem.isSolved) CgaButtonVariant.Cyan else CgaButtonVariant.Primary,
                            testTag = "start_problem_btn_${problem.id}"
                        )

                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
