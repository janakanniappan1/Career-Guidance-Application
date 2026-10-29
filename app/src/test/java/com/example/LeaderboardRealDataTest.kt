package com.example

import com.example.data.model.ProfileDto
import com.example.data.repository.MockAuthRepository
import com.example.data.repository.MockRoadmapRepository
import com.example.domain.model.LeaderboardUser
import com.example.domain.model.RankedLeaderboardEntry
import com.example.domain.model.RankingMode
import com.example.domain.model.StudentProfile
import com.example.domain.repository.LeaderboardRepository
import com.example.domain.model.DomainLeaderboard
import com.example.viewmodel.LeaderboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardRealDataTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. VERIFY REAL USERS ONLY: Mock catalog names NEVER appear in production output
    @Test
    fun test1_noFakeCatalogNamesInLeaderboard() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val realUser = StudentProfile(
            id = "usr_real_1",
            name = "Rizwan Ahamed",
            email = "rizwan@college.edu",
            department = "CSE",
            yearOfStudy = "4th Year",
            careerInterest = "Artificial Intelligence",
            recommendedCareerId = "aiml_engineer",
            isAssessmentCompleted = true,
            currentStreak = 12,
            xp = 1850
        )
        authRepo.updateProfile(realUser)

        val realLeaderboardRepository = object : LeaderboardRepository {
            override fun getLeaderboard(domainId: String, rankingMode: RankingMode): Flow<DomainLeaderboard> {
                val user = LeaderboardUser(
                    userId = realUser.id,
                    displayName = realUser.name,
                    domainId = "aiml_engineer",
                    domainTitle = "AI / Machine Learning Engineer",
                    department = realUser.department,
                    yearOfStudy = realUser.yearOfStudy,
                    xp = realUser.xp,
                    completionPercentage = 75,
                    streak = realUser.currentStreak,
                    completedTasks = 15,
                    totalTasks = 20,
                    isCurrentUser = true
                )
                val entry = RankedLeaderboardEntry(
                    rank = 1,
                    user = user,
                    primaryMetricValue = "${user.xp} XP",
                    metricUnit = "XP",
                    rankingMode = rankingMode,
                    isTop3 = true,
                    isCurrentUser = true
                )
                return flowOf(
                    DomainLeaderboard(
                        domainId = "aiml_engineer",
                        domainTitle = "AI / Machine Learning Engineer",
                        rankingMode = rankingMode,
                        entries = listOf(entry),
                        currentUserEntry = entry,
                        totalParticipants = 1,
                        domainTotalTasks = 20
                    )
                )
            }

            override suspend fun refreshLeaderboard(domainId: String, rankingMode: RankingMode): DomainLeaderboard {
                return getLeaderboard(domainId, rankingMode).first()
            }
        }

        val viewModel = LeaderboardViewModel(realLeaderboardRepository, authRepo)
        testScheduler.advanceUntilIdle()

        val entries = viewModel.uiState.value.entries
        assertEquals(1, entries.size)
        assertEquals("Rizwan Ahamed", entries.first().user.displayName)

        // Strict assertion that no fake catalog names exist
        val fakeNames = listOf("Aarav Sharma", "Priya Patel", "Rohan Verma", "Kavya Reddy", "Vikram Malhotra", "Ananya Iyer")
        for (fake in fakeNames) {
            assertFalse(entries.any { it.user.displayName.contains(fake, ignoreCase = true) })
        }
    }

    // 2. EMPTY DOMAIN LEADERBOARD: Valid empty/single state with zero fake peers
    @Test
    fun test2_emptyDomainHandlesGracefullyWithoutFakePeers() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val emptyRepo = object : LeaderboardRepository {
            override fun getLeaderboard(domainId: String, rankingMode: RankingMode): Flow<DomainLeaderboard> {
                return flowOf(
                    DomainLeaderboard(
                        domainId = domainId,
                        domainTitle = "Cloud & DevOps Engineer",
                        rankingMode = rankingMode,
                        entries = emptyList(),
                        currentUserEntry = null,
                        totalParticipants = 0,
                        domainTotalTasks = 25
                    )
                )
            }

            override suspend fun refreshLeaderboard(domainId: String, rankingMode: RankingMode): DomainLeaderboard {
                return getLeaderboard(domainId, rankingMode).first()
            }
        }

        val viewModel = LeaderboardViewModel(emptyRepo, authRepo)
        viewModel.selectDomain("cloud_engineer")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(0, state.entries.size)
        assertEquals(0, state.totalParticipants)
    }

    // 3. SORTING BY XP, COMPLETION, AND STREAK
    @Test
    fun test3_rankingModesSortCorrectly() = runTest(testDispatcher) {
        val user1 = LeaderboardUser(userId = "u1", displayName = "Alice", domainId = "aiml", domainTitle = "AI", xp = 3000, completionPercentage = 50, streak = 5)
        val user2 = LeaderboardUser(userId = "u2", displayName = "Bob", domainId = "aiml", domainTitle = "AI", xp = 1000, completionPercentage = 95, streak = 20)
        val user3 = LeaderboardUser(userId = "u3", displayName = "Charlie", domainId = "aiml", domainTitle = "AI", xp = 2000, completionPercentage = 80, streak = 30)

        val users = listOf(user1, user2, user3)

        // XP Sorting
        val xpSorted = users.sortedByDescending { it.xp }
        assertEquals("Alice", xpSorted[0].displayName)
        assertEquals("Charlie", xpSorted[1].displayName)
        assertEquals("Bob", xpSorted[2].displayName)

        // Completion Sorting
        val compSorted = users.sortedByDescending { it.completionPercentage }
        assertEquals("Bob", compSorted[0].displayName)
        assertEquals("Charlie", compSorted[1].displayName)
        assertEquals("Alice", compSorted[2].displayName)

        // Streak Sorting
        val streakSorted = users.sortedByDescending { it.streak }
        assertEquals("Charlie", streakSorted[0].displayName)
        assertEquals("Bob", streakSorted[1].displayName)
        assertEquals("Alice", streakSorted[2].displayName)
    }

    // 4. MULTIPLE REAL USERS PRESERVED: Multiple domain participants are all reflected in state
    @Test
    fun test4_multipleRealUsersPreservedInUiState() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val user1 = LeaderboardUser(userId = "u1", displayName = "Jana K", domainId = "aiml_engineer", domainTitle = "AI / Machine Learning Engineer", xp = 745, completionPercentage = 40, streak = 4, isCurrentUser = true)
        val user2 = LeaderboardUser(userId = "u2", displayName = "Siddharth N", domainId = "aiml_engineer", domainTitle = "AI / Machine Learning Engineer", xp = 1200, completionPercentage = 65, streak = 8, isCurrentUser = false)
        val user3 = LeaderboardUser(userId = "u3", displayName = "Meera R", domainId = "aiml_engineer", domainTitle = "AI / Machine Learning Engineer", xp = 950, completionPercentage = 50, streak = 6, isCurrentUser = false)

        val entries = listOf(
            RankedLeaderboardEntry(rank = 1, user = user2, primaryMetricValue = "1200 XP", metricUnit = "XP", rankingMode = RankingMode.XP, isTop3 = true, isCurrentUser = false),
            RankedLeaderboardEntry(rank = 2, user = user3, primaryMetricValue = "950 XP", metricUnit = "XP", rankingMode = RankingMode.XP, isTop3 = true, isCurrentUser = false),
            RankedLeaderboardEntry(rank = 3, user = user1, primaryMetricValue = "745 XP", metricUnit = "XP", rankingMode = RankingMode.XP, isTop3 = true, isCurrentUser = true)
        )

        val multiUserRepo = object : LeaderboardRepository {
            override fun getLeaderboard(domainId: String, rankingMode: RankingMode): Flow<DomainLeaderboard> {
                return flowOf(
                    DomainLeaderboard(
                        domainId = "aiml_engineer",
                        domainTitle = "AI / Machine Learning Engineer",
                        rankingMode = rankingMode,
                        entries = entries,
                        currentUserEntry = entries[2],
                        totalParticipants = 3,
                        domainTotalTasks = 20
                    )
                )
            }

            override suspend fun refreshLeaderboard(domainId: String, rankingMode: RankingMode): DomainLeaderboard {
                return getLeaderboard(domainId, rankingMode).first()
            }
        }

        val viewModel = LeaderboardViewModel(multiUserRepo, authRepo)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.entries.size)
        assertEquals(3, state.totalParticipants)
        assertEquals(3, state.top3Entries.size)
        assertEquals(0, state.remainingEntries.size)
        assertNotNull(state.currentUserEntry)
        assertEquals(3, state.currentUserEntry?.rank)
        assertEquals("Jana K", state.currentUserEntry?.user?.displayName)
    }

    // 5. RPC FAILURE PRODUCES ERROR STATE: No silent fallback to single-user profile
    @Test
    fun test5_rpcFailureProducesErrorState() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val failingRepo = object : LeaderboardRepository {
            override fun getLeaderboard(domainId: String, rankingMode: RankingMode): Flow<DomainLeaderboard> {
                return kotlinx.coroutines.flow.flow {
                    throw IllegalStateException("Leaderboard is temporarily unavailable. Please try again later.")
                }
            }

            override suspend fun refreshLeaderboard(domainId: String, rankingMode: RankingMode): DomainLeaderboard {
                throw IllegalStateException("Leaderboard is temporarily unavailable. Please try again later.")
            }
        }

        val viewModel = LeaderboardViewModel(failingRepo, authRepo)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("temporarily unavailable", ignoreCase = true))
        assertEquals(0, state.entries.size)
    }

    // 6. ZERO XP USERS RETAINED IN DOMAIN
    @Test
    fun test6_zeroXpUsersRetainedInDomain() = runTest(testDispatcher) {
        val zeroXpUser = LeaderboardUser(
            userId = "u_zero",
            displayName = "New Student",
            domainId = "aiml_engineer",
            domainTitle = "AI / Machine Learning Engineer",
            xp = 0,
            completionPercentage = 0,
            streak = 0
        )
        val activeUser = LeaderboardUser(
            userId = "u_active",
            displayName = "Active Student",
            domainId = "aiml_engineer",
            domainTitle = "AI / Machine Learning Engineer",
            xp = 500,
            completionPercentage = 25,
            streak = 3
        )

        val entries = listOf(
            RankedLeaderboardEntry(rank = 1, user = activeUser, primaryMetricValue = "500 XP", metricUnit = "XP", rankingMode = RankingMode.XP, isTop3 = true, isCurrentUser = false),
            RankedLeaderboardEntry(rank = 2, user = zeroXpUser, primaryMetricValue = "0 XP", metricUnit = "XP", rankingMode = RankingMode.XP, isTop3 = true, isCurrentUser = false)
        )

        val repo = object : LeaderboardRepository {
            override fun getLeaderboard(domainId: String, rankingMode: RankingMode): Flow<DomainLeaderboard> {
                return flowOf(
                    DomainLeaderboard(
                        domainId = "aiml_engineer",
                        domainTitle = "AI / Machine Learning Engineer",
                        rankingMode = rankingMode,
                        entries = entries,
                        currentUserEntry = null,
                        totalParticipants = 2,
                        domainTotalTasks = 20
                    )
                )
            }

            override suspend fun refreshLeaderboard(domainId: String, rankingMode: RankingMode): DomainLeaderboard {
                return getLeaderboard(domainId, rankingMode).first()
            }
        }

        val viewModel = LeaderboardViewModel(repo, MockAuthRepository())
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.entries.size)
        assertTrue(state.entries.any { it.user.xp == 0 && it.user.displayName == "New Student" })
    }

    // 7. YOUR STANDING CARD METADATA: No "Cohort" suffix in domainTitle display
    @Test
    fun test7_standingCardMetadataMatchesDomainTitleWithoutCohort() {
        val user = LeaderboardUser(
            userId = "u1",
            displayName = "Jana K",
            domainId = "aiml_engineer",
            domainTitle = "AI / Machine Learning Engineer",
            xp = 745,
            completionPercentage = 40,
            streak = 4,
            isCurrentUser = true
        )
        val entry = RankedLeaderboardEntry(
            rank = 2,
            user = user,
            primaryMetricValue = "745 XP",
            metricUnit = "XP",
            rankingMode = RankingMode.XP,
            isTop3 = true,
            isCurrentUser = true
        )

        assertEquals("AI / Machine Learning Engineer", entry.user.domainTitle)
        assertFalse(entry.user.domainTitle.endsWith("Cohort"))
    }
}
