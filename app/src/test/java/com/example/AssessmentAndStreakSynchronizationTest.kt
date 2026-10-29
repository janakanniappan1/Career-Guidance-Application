package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.data.repository.MockAuthRepository
import com.example.data.repository.MockPracticeRepository
import com.example.data.repository.MockRoadmapRepository
import com.example.domain.model.AssessmentAttempt
import com.example.domain.model.AssessmentOption
import com.example.domain.model.AssessmentQuestion
import com.example.domain.model.CareerRecommendation
import com.example.domain.model.PracticeConstants
import com.example.domain.model.QuizAttempt
import com.example.domain.model.QuizMode
import com.example.domain.model.StudentProfile
import com.example.domain.repository.AssessmentRepository
import com.example.domain.repository.CareerRepository
import com.example.domain.repository.PracticeRepository
import com.example.ui.screens.AssessmentResultScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.CGATheme
import com.example.viewmodel.AssessmentViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CareerViewModel
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.PracticeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Dedicated test suite verifying:
 * Part 1: Career Assessment +100 XP award, persistence, immediate cache update, idempotency, failure handling,
 *         and UI display on AssessmentResultScreen and ProfileScreen.
 * Part 2: Daily Speed Test streak synchronization, immediate cache and UI update, no optimistic update,
 *         failure preservation of previous streak, and same-day idempotency.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class AssessmentAndStreakSynchronizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: MockAuthRepository
    private lateinit var assessmentRepository: TestAssessmentRepository
    private lateinit var careerRepository: TestCareerRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = MockAuthRepository()
        assessmentRepository = TestAssessmentRepository()
        careerRepository = TestCareerRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // PART 1 — ASSESSMENT +100 XP TESTS
    // =========================================================================

    /**
     * Test 1: Fresh user completing career assessment gets +100 XP.
     */
    @Test
    fun testFreshUserAssessmentAwards100Xp() = runTest(testDispatcher) {
        val freshProfile = StudentProfile(
            id = "usr_fresh_1",
            name = "Fresh Student",
            email = "fresh@example.com",
            department = "Computer Science",
            yearOfStudy = "1st Year",
            careerInterest = "",
            currentStreak = 1,
            isAssessmentCompleted = false,
            recommendedCareerId = null,
            dailyMotivationalMessage = "Welcome!",
            xp = 0
        )
        authRepository.updateProfile(freshProfile)
        advanceUntilIdle()

        val viewModel = AssessmentViewModel(
            assessmentRepository = assessmentRepository,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        var completedRecommendation: CareerRecommendation? = null
        viewModel.selectOption("opt_1a")
        viewModel.submitAssessment { rec ->
            completedRecommendation = rec
        }
        advanceUntilIdle()

        assertNotNull(completedRecommendation)
        val updatedUser = authRepository.getCurrentUser().first()
        assertNotNull(updatedUser)
        assertEquals("Fresh user must be awarded 100 XP upon assessment completion", 100, updatedUser?.xp)
        assertTrue("User must be marked as assessment completed", updatedUser?.isAssessmentCompleted == true)
    }

    /**
     * Test 2: Assessment XP is authoritatively persisted to the user profile.
     */
    @Test
    fun testAssessmentXpPersistedToProfile() = runTest(testDispatcher) {
        val initialProfile = StudentProfile(
            id = "usr_persist_2",
            name = "Persist Test User",
            email = "persist@example.com",
            department = "Information Technology",
            yearOfStudy = "2nd Year",
            careerInterest = "",
            currentStreak = 2,
            isAssessmentCompleted = false,
            recommendedCareerId = null,
            dailyMotivationalMessage = "Keep going!",
            xp = 0
        )
        authRepository.updateProfile(initialProfile)
        advanceUntilIdle()

        val viewModel = AssessmentViewModel(
            assessmentRepository = assessmentRepository,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        viewModel.selectOption("opt_1a")
        viewModel.submitAssessment {}
        advanceUntilIdle()

        val persisted = authRepository.getCurrentUser().first()
        assertEquals(100, persisted?.xp)
        assertEquals("cloud_engineer", persisted?.recommendedCareerId)
        assertTrue(persisted?.isAssessmentCompleted == true)
    }

    /**
     * Test 3: Assessment XP updates the in-memory cached profile immediately.
     */
    @Test
    fun testAssessmentXpUpdatesCachedProfileImmediately() = runTest(testDispatcher) {
        val user = StudentProfile(
            id = "usr_cache_3",
            name = "Cache User",
            email = "cache@example.com",
            department = "ECE",
            yearOfStudy = "3rd Year",
            careerInterest = "",
            currentStreak = 1,
            isAssessmentCompleted = false,
            recommendedCareerId = null,
            dailyMotivationalMessage = "Focus on growth",
            xp = 0
        )
        authRepository.updateProfile(user)
        advanceUntilIdle()

        val viewModel = AssessmentViewModel(
            assessmentRepository = assessmentRepository,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        viewModel.selectOption("opt_1a")
        viewModel.submitAssessment {}
        advanceUntilIdle()

        // Cache must reflect 100 XP immediately without needing app restart or extra query
        val cached = authRepository.getCurrentUser().first()
        assertEquals(100, cached?.xp)
    }

    /**
     * Test 4: Re-taking the assessment does NOT award duplicate XP (Idempotency).
     */
    @Test
    fun testRetryingAssessmentDoesNotAwardDuplicateXp() = runTest(testDispatcher) {
        val completedProfile = StudentProfile(
            id = "usr_retry_4",
            name = "Retry User",
            email = "retry@example.com",
            department = "CSE",
            yearOfStudy = "4th Year",
            careerInterest = "",
            currentStreak = 3,
            isAssessmentCompleted = true,
            recommendedCareerId = "cloud_engineer",
            dailyMotivationalMessage = "Consistency is key",
            xp = 100
        )
        authRepository.updateProfile(completedProfile)
        advanceUntilIdle()

        val viewModel = AssessmentViewModel(
            assessmentRepository = assessmentRepository,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        viewModel.selectOption("opt_1a")
        viewModel.submitAssessment {}
        advanceUntilIdle()

        val userAfterRetry = authRepository.getCurrentUser().first()
        assertEquals("Retrying assessment must NOT award duplicate XP", 100, userAfterRetry?.xp)
    }

    /**
     * Test 5: AssessmentResultScreen shows "+100 XP" banner for fresh user.
     */
    @Test
    fun testResultsScreenShows100XpForFreshUser() = runTest(testDispatcher) {
        val recommendation = CareerRecommendation(
            careerId = "cloud_engineer",
            title = "Cloud Engineer",
            matchPercentage = 88,
            reasonForRecommendation = "Strong systems alignment",
            otherSuitableCareers = emptyList(),
            recommendedSkills = listOf("AWS", "Kubernetes")
        )
        careerRepository.setLatestRecommendation(recommendation)
        advanceUntilIdle()

        val careerViewModel = CareerViewModel(careerRepository = careerRepository)
        advanceUntilIdle()

        composeTestRule.setContent {
            CGATheme {
                AssessmentResultScreen(
                    careerViewModel = careerViewModel,
                    onNavigateBack = {},
                    onNavigateToRoadmap = {},
                    onNavigateToCareerDetails = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("assessment_xp_banner")
            .assertIsDisplayed()
            .assertTextContains("+100 XP", substring = true)
    }

    /**
     * Test 6: ProfileScreen shows 100 XP after assessment completion.
     */
    @Test
    fun testProfileScreenShows100XpAfterAssessment() = runTest(testDispatcher) {
        val profileWithXp = StudentProfile(
            id = "usr_profile_6",
            name = "Profile User",
            email = "profile@example.com",
            department = "Computer Science",
            yearOfStudy = "2nd Year",
            careerInterest = "Cloud Computing",
            currentStreak = 2,
            isAssessmentCompleted = true,
            recommendedCareerId = "cloud_engineer",
            dailyMotivationalMessage = "Never give up!",
            xp = 100
        )
        authRepository.updateProfile(profileWithXp)
        advanceUntilIdle()

        val roadmapRepository = MockRoadmapRepository()
        val dashboardViewModel = DashboardViewModel(
            authRepository = authRepository,
            careerRepository = careerRepository,
            roadmapRepository = roadmapRepository
        )
        val authViewModel = AuthViewModel(authRepository = authRepository)
        advanceUntilIdle()

        composeTestRule.setContent {
            CGATheme {
                ProfileScreen(
                    dashboardViewModel = dashboardViewModel,
                    authViewModel = authViewModel,
                    onNavigateBack = {},
                    onNavigateToHome = {},
                    onNavigateToRoadmap = {},
                    onNavigateToPractice = {},
                    onNavigateToExplore = {},
                    onNavigateToResume = {},
                    onNavigateToPortfolio = {},
                    onNavigateToAdmin = {},
                    onLogout = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_total_xp")
            .assertIsDisplayed()
            .assertTextContains("100 XP", substring = true)
    }

    /**
     * Test 7: Assessment failure (database/attempt save failure) does NOT award XP.
     */
    @Test
    fun testAssessmentFailureDoesNotAwardXp() = runTest(testDispatcher) {
        val failingRepo = object : AssessmentRepository {
            override fun getAssessmentQuestions() = assessmentRepository.getAssessmentQuestions()
            override suspend fun saveAnswer(questionId: Int, optionId: String) {}
            override suspend fun getUserAnswers() = mapOf(1 to "opt_1a")
            override suspend fun saveAssessmentAttempt(attempt: AssessmentAttempt): Result<Unit> {
                return Result.failure(IllegalStateException("Database network timeout"))
            }
            override suspend fun clearAnswers() {}
        }

        val initialProfile = StudentProfile(
            id = "usr_fail_7",
            name = "Fail Test User",
            email = "fail@example.com",
            department = "Data Science",
            yearOfStudy = "1st Year",
            careerInterest = "",
            currentStreak = 1,
            isAssessmentCompleted = false,
            recommendedCareerId = null,
            dailyMotivationalMessage = "Keep trying!",
            xp = 0
        )
        authRepository.updateProfile(initialProfile)
        advanceUntilIdle()

        val viewModel = AssessmentViewModel(
            assessmentRepository = failingRepo,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        viewModel.selectOption("opt_1a")
        var completedCalled = false
        viewModel.submitAssessment { completedCalled = true }
        advanceUntilIdle()

        assertFalse("onComplete callback must not be invoked on failure", completedCalled)
        assertEquals("Database network timeout", viewModel.uiState.value.error)
        val userAfterFailure = authRepository.getCurrentUser().first()
        assertEquals("XP must remain 0 when assessment attempt save fails", 0, userAfterFailure?.xp)
        assertFalse("isAssessmentCompleted must remain false on failure", userAfterFailure?.isAssessmentCompleted == true)
    }

    // =========================================================================
    // PART 2 — DAILY SPEED TEST STREAK TESTS
    // =========================================================================

    /**
     * Test 8: Daily Speed Test updates streak immediately in AuthRepository cache.
     */
    @Test
    fun testDailySpeedTestUpdatesStreakImmediately() = runTest(testDispatcher) {
        val user = StudentProfile(
            id = "usr_streak_8",
            name = "Streak User",
            email = "streak@example.com",
            department = "IT",
            yearOfStudy = "3rd Year",
            careerInterest = "",
            currentStreak = 1,
            isAssessmentCompleted = true,
            recommendedCareerId = "cloud_engineer",
            dailyMotivationalMessage = "Daily streak test",
            xp = 50
        )
        authRepository.updateProfile(user)
        advanceUntilIdle()

        val practiceRepository = MockPracticeRepository(authRepository = authRepository)
        val attempt = QuizAttempt(
            id = "attempt_daily_1",
            userId = user.id,
            careerId = "cloud_engineer",
            topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
            score = 5,
            totalQuestions = 5,
            xpEarned = 50,
            completedAt = System.currentTimeMillis()
        )

        val saveResult = practiceRepository.saveQuizAttempt(attempt)
        assertTrue(saveResult.isSuccess)
        advanceUntilIdle()

        val updated = authRepository.getCurrentUser().first()
        assertEquals("Authoritative streak must immediately update to 2", 2, updated?.currentStreak)
    }

    /**
     * Test 9: Daily Speed Test streak is immediately reflected in ViewModel UI state.
     */
    @Test
    fun testDailySpeedTestStreakReflectedInUiState() = runTest(testDispatcher) {
        val user = StudentProfile(
            id = "usr_vm_streak_9",
            name = "VM Streak User",
            email = "vmstreak@example.com",
            department = "CSE",
            yearOfStudy = "2nd Year",
            careerInterest = "",
            currentStreak = 1,
            isAssessmentCompleted = true,
            recommendedCareerId = "cloud_engineer",
            dailyMotivationalMessage = "Daily focus",
            xp = 100
        )
        authRepository.updateProfile(user)
        advanceUntilIdle()

        val practiceRepository = MockPracticeRepository(authRepository = authRepository)
        val practiceViewModel = PracticeViewModel(
            practiceRepository = practiceRepository,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        assertEquals(1, practiceViewModel.uiState.value.currentStreak)

        practiceViewModel.startDailySpeedTest()
        advanceUntilIdle()

        // Answer all questions
        val totalQuestions = practiceViewModel.quizState.value.questions.size
        for (i in 0 until totalQuestions) {
            val q = practiceViewModel.quizState.value.questions[practiceViewModel.quizState.value.currentIndex]
            practiceViewModel.selectQuizOption(q.correctIndex)
            practiceViewModel.checkAnswer()
            practiceViewModel.nextQuizQuestion()
            advanceUntilIdle()
        }

        // Streak must immediately reflect 2 in both uiState and quizState
        assertEquals(2, practiceViewModel.uiState.value.currentStreak)
        assertEquals(2, practiceViewModel.quizState.value.currentStreak)
    }

    /**
     * Test 10: Daily Speed Test does NOT optimistically increment streak before save succeeds.
     */
    @Test
    fun testDailySpeedTestDoesNotOptimisticallyIncrement() = runTest(testDispatcher) {
        val user = StudentProfile(
            id = "usr_no_optimistic_10",
            name = "No Optimistic",
            email = "nooptimistic@example.com",
            department = "IT",
            yearOfStudy = "1st Year",
            careerInterest = "",
            currentStreak = 3,
            isAssessmentCompleted = true,
            recommendedCareerId = "cloud_engineer",
            dailyMotivationalMessage = "Steady progress",
            xp = 200
        )
        authRepository.updateProfile(user)
        advanceUntilIdle()

        val practiceRepository = MockPracticeRepository(authRepository = authRepository)
        val practiceViewModel = PracticeViewModel(
            practiceRepository = practiceRepository,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        practiceViewModel.startDailySpeedTest()
        advanceUntilIdle()

        // While taking quiz, streak MUST strictly remain 3
        practiceViewModel.selectQuizOption(0)
        assertEquals("Streak must not optimistically change during quiz progression", 3, practiceViewModel.quizState.value.currentStreak)
        assertEquals("UiState streak must not change during quiz progression", 3, practiceViewModel.uiState.value.currentStreak)
    }

    /**
     * Test 11: Streak RPC or save failure preserves previous valid streak.
     */
    @Test
    fun testDailySpeedTestStreakFailurePreservesPreviousStreak() = runTest(testDispatcher) {
        val user = StudentProfile(
            id = "usr_fail_streak_11",
            name = "Preserve Streak User",
            email = "failstreak@example.com",
            department = "ECE",
            yearOfStudy = "3rd Year",
            careerInterest = "",
            currentStreak = 5,
            isAssessmentCompleted = true,
            recommendedCareerId = "cloud_engineer",
            dailyMotivationalMessage = "Resilience",
            xp = 350
        )
        authRepository.updateProfile(user)
        advanceUntilIdle()

        val failingPracticeRepo = object : PracticeRepository by MockPracticeRepository(authRepository = authRepository) {
            override suspend fun saveQuizAttempt(attempt: QuizAttempt): Result<Unit> {
                // Simulate RPC failure
                return Result.failure(RuntimeException("record_user_activity RPC timeout"))
            }
        }

        val practiceViewModel = PracticeViewModel(
            practiceRepository = failingPracticeRepo,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        practiceViewModel.startDailySpeedTest()
        advanceUntilIdle()

        val totalQuestions = practiceViewModel.quizState.value.questions.size
        for (i in 0 until totalQuestions) {
            val q = practiceViewModel.quizState.value.questions[practiceViewModel.quizState.value.currentIndex]
            practiceViewModel.selectQuizOption(q.correctIndex)
            practiceViewModel.checkAnswer()
            practiceViewModel.nextQuizQuestion()
            advanceUntilIdle()
        }

        // On failure, streak must be preserved at previous valid streak (5)
        assertEquals("Previous valid streak must be strictly preserved on failure", 5, authRepository.getCurrentUser().first()?.currentStreak)
        assertEquals("UiState streak must preserve previous streak on failure", 5, practiceViewModel.uiState.value.currentStreak)
    }

    /**
     * Test 12: Subsequent Speed Test on the same day does NOT duplicate streak.
     */
    @Test
    fun testSubsequentSpeedTestSameDayDoesNotDuplicateStreak() = runTest(testDispatcher) {
        val user = StudentProfile(
            id = "usr_sameday_12",
            name = "Same Day User",
            email = "sameday@example.com",
            department = "MECH",
            yearOfStudy = "2nd Year",
            careerInterest = "",
            currentStreak = 1,
            isAssessmentCompleted = true,
            recommendedCareerId = "cloud_engineer",
            dailyMotivationalMessage = "One day at a time",
            xp = 100
        )
        authRepository.updateProfile(user)
        advanceUntilIdle()

        val practiceRepository = MockPracticeRepository(authRepository = authRepository)

        // Attempt 1: First Daily Speed Test of the day
        val attempt1 = QuizAttempt(
            id = "attempt_1",
            userId = user.id,
            careerId = "cloud_engineer",
            topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
            score = 5,
            totalQuestions = 5,
            xpEarned = 50,
            completedAt = System.currentTimeMillis()
        )
        practiceRepository.saveQuizAttempt(attempt1)
        advanceUntilIdle()

        // Streak increments to 2, XP increments to 150
        val afterFirstAttempt = authRepository.getCurrentUser().first()
        assertEquals(2, afterFirstAttempt?.currentStreak)
        assertEquals(150, afterFirstAttempt?.xp)

        // Attempt 2: Second Daily Speed Test on the SAME day
        val attempt2 = QuizAttempt(
            id = "attempt_2",
            userId = user.id,
            careerId = "cloud_engineer",
            topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
            score = 5,
            totalQuestions = 5,
            xpEarned = 50,
            completedAt = System.currentTimeMillis()
        )
        practiceRepository.saveQuizAttempt(attempt2)
        advanceUntilIdle()

        // Must strictly preserve streak at 2, and XP at 150 (idempotent, no duplicate streak)
        val afterSecondAttempt = authRepository.getCurrentUser().first()
        assertEquals("Subsequent test on same day must not increment streak", 2, afterSecondAttempt?.currentStreak)
        assertEquals("Subsequent test on same day must not duplicate XP", 150, afterSecondAttempt?.xp)
    }

    // =========================================================================
    // TEST HELPER IMPLEMENTATIONS
    // =========================================================================

    class TestAssessmentRepository : AssessmentRepository {
        private val answers = mutableMapOf<Int, String>()
        private val questions = listOf(
            AssessmentQuestion(
                id = 1,
                questionNumber = 1,
                category = "Cloud & Infrastructure",
                questionText = "Which area of engineering interests you?",
                options = listOf(
                    AssessmentOption("opt_1a", "Scalable cloud architectures", mapOf("cloud_engineer" to 10))
                )
            )
        )

        override fun getAssessmentQuestions(): List<AssessmentQuestion> = questions
        override suspend fun saveAnswer(questionId: Int, optionId: String) {
            answers[questionId] = optionId
        }
        override suspend fun getUserAnswers(): Map<Int, String> = answers.toMap()
        override suspend fun saveAssessmentAttempt(attempt: AssessmentAttempt): Result<Unit> = Result.success(Unit)
        override suspend fun clearAnswers() {
            answers.clear()
        }
    }

    class TestCareerRepository : CareerRepository {
        private val recommendationFlow = MutableStateFlow<CareerRecommendation?>(null)

        fun setLatestRecommendation(rec: CareerRecommendation) {
            recommendationFlow.value = rec
        }

        override suspend fun calculateRecommendation(answers: Map<Int, String>): CareerRecommendation {
            val rec = CareerRecommendation(
                careerId = "cloud_engineer",
                title = "Cloud Engineer",
                matchPercentage = 88,
                reasonForRecommendation = "Strong interest in scalable infrastructure",
                otherSuitableCareers = emptyList(),
                recommendedSkills = listOf("AWS", "Docker", "Kubernetes")
            )
            recommendationFlow.value = rec
            return rec
        }

        override fun getLatestRecommendation(): Flow<CareerRecommendation?> = recommendationFlow
        override suspend fun getCareerDetails(careerId: String): CareerRecommendation? = recommendationFlow.value
        override suspend fun saveRecommendation(recommendation: CareerRecommendation) {
            recommendationFlow.value = recommendation
        }
        override fun getAllCareers(): List<CareerRecommendation> = emptyList()
    }
}
