package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RankedLeaderboardEntry
import com.example.domain.model.RankingMode
import com.example.ui.components.CgaBottomNavigation
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.LocalCGATokens
import com.example.util.CareerCanonicalUtil
import com.example.viewmodel.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    leaderboardViewModel: LeaderboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToRoadmap: (String) -> Unit,
    onNavigateToPractice: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by leaderboardViewModel.uiState.collectAsState()
    val tokens = LocalCGATokens.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        leaderboardViewModel.refresh()
    }

    LaunchedEffect(state.refreshError) {
        state.refreshError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            leaderboardViewModel.clearRefreshError()
        }
    }

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "Domain Leaderboard",
                onBackClick = onNavigateBack,
                testTag = "leaderboard_top_bar"
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CgaBottomNavigation(
                currentRoute = "leaderboard",
                onNavigate = { route ->
                    when (route) {
                        "dashboard" -> onNavigateToHome()
                        "explore" -> onNavigateToExplore()
                        "roadmap" -> onNavigateToRoadmap(state.selectedDomainId)
                        "practice" -> onNavigateToPractice()
                        "profile" -> onNavigateToProfile()
                    }
                }
            )
        },
        containerColor = tokens.contentBackground,
        modifier = modifier.testTag("leaderboard_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header: Compact title and subtitle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🏆", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Leaderboard",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Compete, improve, and track your progress",
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 1. DOMAIN SELECTOR CHIPS ROW (Horizontal scrollable)
            DomainSelectorBar(
                selectedDomainId = state.selectedDomainId,
                availableDomains = state.availableDomains,
                onDomainSelected = { leaderboardViewModel.selectDomain(it) }
            )

            // 2. RANKING MODE TABS (XP, Course Completion %, Streak)
            RankingModeTabBar(
                activeMode = state.activeRankingMode,
                onModeSelected = { leaderboardViewModel.selectRankingMode(it) }
            )

            // 3. MAIN CONTENT: Loading, Error, or Leaderboard Entries
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { leaderboardViewModel.refresh() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("leaderboard_pull_to_refresh")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            state.isLoading && !state.isRefreshing && state.leaderboard == null -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = CGAColors.Primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Loading ${state.selectedDomainTitle} rankings...",
                                            color = tokens.textMuted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            state.error != null && state.leaderboard == null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "Unable to load leaderboard",
                                            color = tokens.textHeading,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = state.error ?: "Please check your network and try again.",
                                            color = tokens.textMuted,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        CgaPrimaryButton(
                                            text = "RETRY",
                                            onClick = { leaderboardViewModel.refresh() },
                                            testTag = "leaderboard_retry_button"
                                        )
                                    }
                                }
                            }

                            state.entries.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = tokens.textMuted,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            text = "No rankings yet for ${state.selectedDomainTitle}",
                                            color = tokens.textHeading,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Be the first to complete roadmap milestones and practice problems in this domain!",
                                            color = tokens.textMuted,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            else -> {
                                val listEntries = if (state.top3Entries.size >= 3) {
                                    state.entries.filter { it.rank > 3 }
                                } else {
                                    state.entries
                                }

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Section: Top 3 Podium
                                    if (state.top3Entries.isNotEmpty()) {
                                        item {
                                            Top3Podium(
                                                topEntries = state.top3Entries,
                                                activeMode = state.activeRankingMode,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 12.dp)
                                            )
                                        }
                                    }

                                    // Header for remaining rankings
                                    if (listEntries.isNotEmpty()) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (state.top3Entries.size >= 3) {
                                                        "Rankings (#4 - #${state.totalParticipants})"
                                                    } else {
                                                        "All Domain Participants (${state.totalParticipants})"
                                                    },
                                                    color = tokens.textMuted,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Text(
                                                    text = when (state.activeRankingMode) {
                                                        RankingMode.XP -> "TOTAL XP"
                                                        RankingMode.COMPLETION -> "ROADMAP PROGRESS"
                                                        RankingMode.STREAK -> "CURRENT STREAK"
                                                    },
                                                    color = tokens.textMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        // Remaining Participants List
                                        items(
                                            items = listEntries,
                                            key = { it.user.userId }
                                        ) { entry ->
                                            LeaderboardRow(
                                                entry = entry,
                                                activeMode = state.activeRankingMode
                                            )
                                        }
                                    } else if (state.top3Entries.isNotEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "All domain participants are featured on the podium above",
                                                    color = tokens.textMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section: Current User Sticky Bottom Banner (highlighting current user's standing on ALL domain tabs!)
                    val currentUserEntry = state.currentUserEntry
                    if (currentUserEntry != null) {
                        CurrentUserStandingBar(
                            entry = currentUserEntry,
                            activeMode = state.activeRankingMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1. Horizontal Domain Selector Chip Row
 */
@Composable
private fun DomainSelectorBar(
    selectedDomainId: String,
    availableDomains: List<Pair<String, String>>,
    onDomainSelected: (String) -> Unit
) {
    val tokens = LocalCGATokens.current
    val lazyListState = rememberLazyListState()

    LaunchedEffect(selectedDomainId) {
        val index = availableDomains.indexOfFirst { it.first == selectedDomainId }
        if (index >= 0) {
            lazyListState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = lazyListState,
        modifier = Modifier
            .fillMaxWidth()
            .background(tokens.surface)
            .testTag("domain_selector_bar"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(availableDomains, key = { it.first }) { (domainId, title) ->
            val isSelected = domainId == selectedDomainId
            val chipLabel = when (domainId) {
                CareerCanonicalUtil.CAREER_AIML -> "AI / ML"
                CareerCanonicalUtil.CAREER_SOFTWARE_DEV -> "Software Dev"
                CareerCanonicalUtil.CAREER_DATA_ANALYST -> "Data Analyst"
                CareerCanonicalUtil.CAREER_DATA_SCIENTIST -> "Data Science"
                CareerCanonicalUtil.CAREER_UIUX -> "UI / UX"
                CareerCanonicalUtil.CAREER_CYBER_SECURITY -> "Cybersecurity"
                CareerCanonicalUtil.CAREER_CLOUD_ENGINEER -> "Cloud & DevOps"
                else -> title
            }

            val chipBg by animateColorAsState(
                targetValue = if (isSelected) CGAColors.Primary else tokens.contentBackground,
                label = "domain_chip_bg"
            )
            val chipBorder by animateColorAsState(
                targetValue = if (isSelected) CGAColors.Primary else tokens.borderMain,
                label = "domain_chip_border"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = chipBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onDomainSelected(domainId) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("domain_chip_$domainId")
            ) {
                Text(
                    text = chipLabel,
                    color = if (isSelected) Color.White else tokens.textHeading,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 2. Three-mode Segmented Ranking Tabs (⚡ XP, 📚 Completion, 🔥 Streak)
 */
@Composable
private fun RankingModeTabBar(
    activeMode: RankingMode,
    onModeSelected: (RankingMode) -> Unit
) {
    val tokens = LocalCGATokens.current

    Surface(
        color = tokens.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, tokens.borderMain, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankingTabItem(
                prefixEmoji = "⚡",
                label = "XP",
                isSelected = activeMode == RankingMode.XP,
                onClick = { onModeSelected(RankingMode.XP) },
                tag = "tab_xp",
                modifier = Modifier.weight(1f)
            )

            RankingTabItem(
                prefixEmoji = "📚",
                label = "Completion",
                isSelected = activeMode == RankingMode.COMPLETION,
                onClick = { onModeSelected(RankingMode.COMPLETION) },
                tag = "tab_completion",
                modifier = Modifier.weight(1f)
            )

            RankingTabItem(
                prefixEmoji = "🔥",
                label = "Streak",
                isSelected = activeMode == RankingMode.STREAK,
                onClick = { onModeSelected(RankingMode.STREAK) },
                tag = "tab_streak",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RankingTabItem(
    prefixEmoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) CGAColors.Primary else Color.Transparent,
        label = "tab_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else tokens.textMuted,
        label = "tab_text"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = prefixEmoji,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * 3. Top 3 Podium (Redesigned with Gold #D4A72C, Silver #8A9298, Bronze #A66A3F)
 */
@Composable
private fun Top3Podium(
    topEntries: List<RankedLeaderboardEntry>,
    activeMode: RankingMode,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    val first = topEntries.firstOrNull()
    val second = topEntries.getOrNull(1)
    val third = topEntries.getOrNull(2)

    Surface(
        color = tokens.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, tokens.borderMain, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFD4A72C),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TOP PERFORMERS",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Proper Podium Arrangement: #2 Silver (Left) | #1 Gold (Center, Tallest) | #3 Bronze (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // #2 Place (Silver #8A9298) - Left, Medium Height
                if (second != null) {
                    PodiumColumn(
                        entry = second,
                        rank = 2,
                        medalEmoji = "🥈",
                        accentColor = Color(0xFF8A9298),
                        stepGradient = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
                        stepBorder = Color(0xFF8A9298),
                        stepTextColor = Color(0xFF2C3238),
                        stepHeight = 76.dp,
                        avatarBg = Color(0xFFF1F3F5),
                        scoreColor = Color(0xFF475569),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // #1 Place (Gold #D4A72C) - Center, Tallest & Most Prominent
                if (first != null) {
                    PodiumColumn(
                        entry = first,
                        rank = 1,
                        medalEmoji = "🥇",
                        accentColor = Color(0xFFD4A72C),
                        stepGradient = listOf(Color(0xFFFFF7DF), Color(0xFFF9E498)),
                        stepBorder = Color(0xFFD4A72C),
                        stepTextColor = Color(0xFF4A3400),
                        stepHeight = 104.dp,
                        avatarBg = Color(0xFFFFF7DF),
                        scoreColor = Color(0xFF785600),
                        modifier = Modifier.weight(1.08f)
                    )
                }

                // #3 Place (Bronze #A66A3F) - Right, Slightly Shorter
                if (third != null) {
                    PodiumColumn(
                        entry = third,
                        rank = 3,
                        medalEmoji = "🥉",
                        accentColor = Color(0xFFA66A3F),
                        stepGradient = listOf(Color(0xFFFAF0EA), Color(0xFFE8D3C5)),
                        stepBorder = Color(0xFFA66A3F),
                        stepTextColor = Color(0xFF4A260E),
                        stepHeight = 54.dp,
                        avatarBg = Color(0xFFFAF0EA),
                        scoreColor = Color(0xFF7C3E14),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    entry: RankedLeaderboardEntry,
    rank: Int,
    medalEmoji: String,
    accentColor: Color,
    stepGradient: List<Color>,
    stepBorder: Color,
    stepTextColor: Color,
    stepHeight: androidx.compose.ui.unit.Dp,
    avatarBg: Color,
    scoreColor: Color,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current
    val isYou = entry.isCurrentUser

    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Crown / Medal Badge Indicator
        Text(
            text = medalEmoji,
            fontSize = if (rank == 1) 22.sp else 18.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. User Avatar Circle with metallic border (and CGA Primary ring if current user)
        Box(
            modifier = Modifier.size(if (rank == 1) 50.dp else 44.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(avatarBg)
                    .border(
                        width = if (isYou) 2.5.dp else 2.dp,
                        color = if (isYou) CGAColors.Primary else accentColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.user.displayName.firstOrNull()?.uppercase() ?: "S",
                    color = if (isYou) CGAColors.Primary else accentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (rank == 1) 18.sp else 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. User Name & "YOU" Indicator
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isYou) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CGAColors.Primary)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "YOU",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = entry.user.displayName,
                color = tokens.textHeading,
                fontWeight = if (rank == 1) FontWeight.ExtraBold else FontWeight.Bold,
                fontSize = if (rank == 1) 12.sp else 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 4. Score with correct dynamic unit (XP, %, or days)
        Text(
            text = entry.primaryMetricValue,
            color = scoreColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (rank == 1) 12.sp else 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Tactile Podium Step Box (Custom Height, Gradient, Border, Accessible Dark Text)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stepHeight)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(stepGradient)
                )
                .border(
                    width = if (isYou) 2.dp else 1.5.dp,
                    color = if (isYou) CGAColors.Primary else stepBorder,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = when (rank) {
                        1 -> "1st"
                        2 -> "2nd"
                        3 -> "3rd"
                        else -> "#$rank"
                    },
                    color = stepTextColor,
                    fontWeight = FontWeight.Black,
                    fontSize = if (rank == 1) 18.sp else 14.sp,
                    letterSpacing = 0.5.sp
                )

                Icon(
                    imageVector = if (rank == 1) Icons.Default.EmojiEvents else Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = stepTextColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(if (rank == 1) 20.dp else 16.dp)
                )
            }
        }
    }
}

/**
 * 4. Individual Leaderboard Row Item
 */
@Composable
private fun LeaderboardRow(
    entry: RankedLeaderboardEntry,
    activeMode: RankingMode
) {
    val tokens = LocalCGATokens.current
    val isYou = entry.isCurrentUser

    val rowBg = if (isYou) {
        tokens.primary.copy(alpha = 0.08f)
    } else {
        tokens.surface
    }

    val rowBorder = if (isYou) {
        CGAColors.Primary
    } else {
        tokens.borderMain
    }

    Surface(
        color = rowBg,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = if (isYou) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isYou) 1.5.dp else 1.dp,
                color = rowBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("leaderboard_row_${entry.rank}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        when (entry.rank) {
                            1 -> Color(0xFFFEF3C7)
                            2 -> Color(0xFFF1F5F9)
                            3 -> Color(0xFFFDF2E9)
                            else -> if (isYou) CGAColors.Primary.copy(alpha = 0.15f) else tokens.contentBackground
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    color = when (entry.rank) {
                        1 -> Color(0xFFB45309)
                        2 -> Color(0xFF475569)
                        3 -> Color(0xFF9A3412)
                        else -> if (isYou) CGAColors.Primary else tokens.textHeading
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Avatar initial circle
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isYou) CGAColors.LightGreen else tokens.contentBackground)
                    .border(1.dp, if (isYou) CGAColors.Primary else tokens.borderMain, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.user.displayName.firstOrNull()?.uppercase() ?: "S",
                    color = if (isYou) CGAColors.Primary else tokens.textHeading,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // User Info Column (Weight 1f for responsive wrapping)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.user.displayName,
                        color = tokens.textHeading,
                        fontWeight = if (isYou) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isYou) {
                        TagPill(
                            text = "YOU",
                            backgroundColor = CGAColors.Primary,
                            textColor = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val subtext = buildString {
                    if (entry.user.department.isNotBlank()) append(entry.user.department)
                    if (entry.user.yearOfStudy.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(entry.user.yearOfStudy)
                    }
                    if (activeMode != RankingMode.COMPLETION) {
                        if (isNotEmpty()) append(" • ")
                        append("${entry.user.completionPercentage}% Done")
                    } else {
                        if (isNotEmpty()) append(" • ")
                        append("${entry.user.xp} XP")
                    }
                }

                Text(
                    text = subtext.ifBlank { "Department Student" },
                    color = tokens.textMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Score Metric Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (activeMode) {
                            RankingMode.XP -> Color(0xFFFFFBEB)
                            RankingMode.COMPLETION -> tokens.successContainer
                            RankingMode.STREAK -> tokens.streakBg
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when (activeMode) {
                            RankingMode.XP -> Color(0xFFFDE68A)
                            RankingMode.COMPLETION -> tokens.onSuccessContainer.copy(alpha = 0.35f)
                            RankingMode.STREAK -> tokens.streakBorder
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = entry.primaryMetricValue,
                    color = when (activeMode) {
                        RankingMode.XP -> Color(0xFFB45309)
                        RankingMode.COMPLETION -> tokens.onSuccessContainer
                        RankingMode.STREAK -> tokens.streakText
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 5. Pinned Standing Card for Logged-In User
 */
@Composable
private fun CurrentUserStandingBar(
    entry: RankedLeaderboardEntry,
    activeMode: RankingMode,
    modifier: Modifier = Modifier
) {
    val tokens = LocalCGATokens.current

    Surface(
        color = tokens.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        modifier = modifier
            .border(1.5.dp, CGAColors.Primary, RoundedCornerShape(16.dp))
            .testTag("current_user_standing_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CGAColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${entry.rank}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Your Standing",
                            color = tokens.textHeading,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TagPill(
                            text = "YOU",
                            backgroundColor = CGAColors.Primary,
                            textColor = Color.White
                        )
                    }
                    Text(
                        text = "${entry.user.domainTitle} • ${entry.user.completionPercentage}% Completed",
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.primaryMetricValue,
                    color = CGAColors.Primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    text = when (activeMode) {
                        RankingMode.XP -> "Total XP"
                        RankingMode.COMPLETION -> "Completed"
                        RankingMode.STREAK -> "Day Streak"
                    },
                    color = tokens.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
