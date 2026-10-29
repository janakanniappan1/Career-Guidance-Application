package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.repository.MockAuthRepository
import com.example.domain.model.AssessmentAttempt
import com.example.domain.model.AssessmentOption
import com.example.domain.model.AssessmentQuestion
import com.example.domain.model.CareerRecommendation
import com.example.domain.model.OtherCareerMatch
import com.example.domain.model.StudentProfile
import com.example.domain.repository.AssessmentRepository
import com.example.domain.repository.CareerRepository
import com.example.ui.screens.AssessmentResultScreen
import com.example.ui.theme.CGATheme
import com.example.viewmodel.AssessmentResultUiState
import com.example.viewmodel.AssessmentViewModel
import com.example.viewmodel.CareerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class AssessmentResultsResolutionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: MockAuthRepository
    private lateinit var careerRepository: TestableCareerRepository
    private lateinit var assessmentRepository: TestableAssessmentRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = MockAuthRepository()
        careerRepository = TestableCareerRepository()
        assessmentRepository = TestableAssessmentRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test Case 1:
     * User completes assessment, navigates to Assessment Results screen,
     * and recommendation renders immediately instead of staying on infinite loading.
     */
    @Test
    fun testAssessmentCompletionRendersImmediatelyWithoutInfiniteSpinner() = runTest(testDispatcher) {
        val testUser = StudentProfile(
            id = "user_alpha",
            name = "Test Student",
            email = "student@test.com",
            department = "CSE",
            yearOfStudy = "3rd Year",
            careerInterest = "AI",
            isAssessmentCompleted = false,
            xp = 0
        )
        authRepository.updateProfile(testUser)
        advanceUntilIdle()

        val assessmentViewModel = AssessmentViewModel(
            assessmentRepository = assessmentRepository,
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        val careerViewModel = CareerViewModel(
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        advanceUntilIdle()

        // Submit assessment
        var completedRecommendation: CareerRecommendation? = null
        assessmentViewModel.selectOption("opt1")
        assessmentViewModel.submitAssessment { rec ->
            completedRecommendation = rec
            careerViewModel.setRecommendation(rec)
        }
        advanceUntilIdle()

        assertNotNull(completedRecommendation)

        // Render AssessmentResultScreen
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

        // Verify results screen is rendered and loading spinner is gone
        composeTestRule.onNodeWithTag("assessment_results_screen")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("assessment_xp_banner")
            .assertIsDisplayed()
            .assertTextContains("+100 XP", substring = true)
        composeTestRule.onNodeWithTag("primary_recommendation_card")
            .assertIsDisplayed()
    }

    /**
     * Test Case 2:
     * Reopen Assessment Results screen -> confirms previously saved recommendation loads correctly.
     */
    @Test
    fun testReopeningAssessmentResultsScreenLoadsSavedRecommendation() = runTest(testDispatcher) {
        val sampleRecommendation = CareerRecommendation(
            careerId = "aiml_engineer",
            title = "AI / ML Engineer",
            matchPercentage = 94,
            reasonForRecommendation = "Exceptional affinity for algorithms and neural networks",
            otherSuitableCareers = listOf(
                OtherCareerMatch("data_scientist", "Data Scientist", 86, "Strong statistics foundation")
            ),
            recommendedSkills = listOf("Python", "PyTorch", "TensorFlow")
        )
        careerRepository.saveRecommendation(sampleRecommendation)
        advanceUntilIdle()

        val careerViewModel = CareerViewModel(
            careerRepository = careerRepository,
            authRepository = authRepository
        )
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

        composeTestRule.onNodeWithTag("assessment_results_screen")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("primary_recommendation_card")
            .assertIsDisplayed()
    }

    /**
     * Test Case 3:
     * Force repository/query error -> confirms screen leaves Loading and displays error state with retry.
     */
    @Test
    fun testRepositoryErrorDisplaysErrorStateWithRetryAction() = runTest(testDispatcher) {
        val testUser = StudentProfile(
            id = "user_default",
            name = "Default Student",
            email = "default@test.com",
            department = "CSE",
            yearOfStudy = "3rd Year",
            careerInterest = "Tech",
            isAssessmentCompleted = true
        )
        authRepository.updateProfile(testUser)
        advanceUntilIdle()

        careerRepository.forceErrorOnLoad = true

        val careerViewModel = CareerViewModel(
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        careerViewModel.loadAssessmentResult("user_default")
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

        // Error container and retry button are displayed
        composeTestRule.onNodeWithTag("results_error_container")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("results_retry_button")
            .assertIsDisplayed()

        // Clear error and retry
        careerRepository.forceErrorOnLoad = false
        careerRepository.mockDatabaseRecommendations["user_default"] = CareerRecommendation(
            userId = "user_default",
            careerId = "software_dev",
            title = "Software Developer",
            matchPercentage = 90,
            reasonForRecommendation = "Solid programming and problem solving foundation",
            otherSuitableCareers = emptyList(),
            recommendedSkills = listOf("Kotlin", "Java")
        )

        composeTestRule.onNodeWithTag("results_retry_button").performClick()
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("assessment_results_screen")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("primary_recommendation_card")
            .assertIsDisplayed()
    }

    /**
     * Test Case 4:
     * Authenticated user sees only their own assessment result/recommendation.
     */
    @Test
    fun testAuthenticatedUserSeesOnlyTheirOwnRecommendation() = runTest(testDispatcher) {
        careerRepository.mockDatabaseRecommendations["user_A"] = CareerRecommendation(
            userId = "user_A",
            careerId = "cyber_analyst",
            title = "Cybersecurity Analyst",
            matchPercentage = 85,
            reasonForRecommendation = "Network security alignment",
            otherSuitableCareers = emptyList(),
            recommendedSkills = listOf("Network Security", "Linux")
        )
        careerRepository.mockDatabaseRecommendations["user_B"] = CareerRecommendation(
            userId = "user_B",
            careerId = "uiux_designer",
            title = "UI/UX Designer",
            matchPercentage = 92,
            reasonForRecommendation = "Design thinking alignment",
            otherSuitableCareers = emptyList(),
            recommendedSkills = listOf("Figma", "User Research")
        )

        val userA = StudentProfile(
            id = "user_A",
            name = "Student A",
            email = "userA@test.com",
            department = "CSE",
            yearOfStudy = "2nd Year",
            careerInterest = "Security",
            isAssessmentCompleted = true
        )
        authRepository.updateProfile(userA)
        advanceUntilIdle()

        val careerViewModel = CareerViewModel(
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        careerViewModel.loadAssessmentResult("user_A")
        advanceUntilIdle()

        val uiState = careerViewModel.assessmentResultUiState.value
        assertTrue(uiState is AssessmentResultUiState.Success)
        assertEquals("cyber_analyst", (uiState as AssessmentResultUiState.Success).recommendation.careerId)
    }

    /**
     * Test Case 5:
     * Navigate away and back to Assessment Results -> confirms state does not get permanently stuck.
     */
    @Test
    fun testNavigateAwayAndBackPreservesState() = runTest(testDispatcher) {
        val recommendation = CareerRecommendation(
            careerId = "cloud_engineer",
            title = "Cloud Engineer",
            matchPercentage = 88,
            reasonForRecommendation = "Scalable systems expertise",
            otherSuitableCareers = emptyList(),
            recommendedSkills = listOf("AWS", "Kubernetes")
        )
        val careerViewModel = CareerViewModel(
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        careerViewModel.setRecommendation(recommendation)
        advanceUntilIdle()

        // First render
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
        composeTestRule.onNodeWithTag("assessment_results_screen").assertIsDisplayed()

        // "Navigate away and back" (trigger reload)
        careerViewModel.loadAssessmentResult()
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("assessment_results_screen").assertIsDisplayed()
    }

    /**
     * Test Case 6:
     * Cold-start / relaunch the app -> saved recommendation is loaded from repository without getting stuck.
     */
    @Test
    fun testColdStartLoadsSavedRecommendationFromRepository() = runTest(testDispatcher) {
        val coldRecommendation = CareerRecommendation(
            userId = "user_cold",
            careerId = "data_analyst",
            title = "Data Analyst",
            matchPercentage = 87,
            reasonForRecommendation = "Statistical reasoning aptitude",
            otherSuitableCareers = emptyList(),
            recommendedSkills = listOf("SQL", "Python", "Tableau")
        )
        careerRepository.mockDatabaseRecommendations["user_cold"] = coldRecommendation

        val coldUser = StudentProfile(
            id = "user_cold",
            name = "Cold User",
            email = "cold@test.com",
            department = "Computer Science",
            yearOfStudy = "4th Year",
            careerInterest = "Data Science",
            isAssessmentCompleted = true
        )
        authRepository.updateProfile(coldUser)
        advanceUntilIdle()

        // Fresh ViewModel instance simulating cold start
        val freshCareerViewModel = CareerViewModel(
            careerRepository = careerRepository,
            authRepository = authRepository
        )
        freshCareerViewModel.loadAssessmentResult("user_cold")
        advanceUntilIdle()

        composeTestRule.setContent {
            CGATheme {
                AssessmentResultScreen(
                    careerViewModel = freshCareerViewModel,
                    onNavigateBack = {},
                    onNavigateToRoadmap = {},
                    onNavigateToCareerDetails = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("assessment_results_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("primary_recommendation_card").assertIsDisplayed()
    }

    private class TestableAssessmentRepository : AssessmentRepository {
        val answers = mutableMapOf<Int, String>()
        val savedAttempts = mutableListOf<AssessmentAttempt>()

        override fun getAssessmentQuestions(): List<AssessmentQuestion> {
            return listOf(
                AssessmentQuestion(
                    id = 1,
                    questionNumber = 1,
                    category = "AI & ML",
                    questionText = "Sample Question 1",
                    options = listOf(
                        AssessmentOption("opt1", "Option A", mapOf("aiml_engineer" to 5)),
                        AssessmentOption("opt2", "Option B", mapOf("software_dev" to 5))
                    )
                )
            )
        }
        override suspend fun saveAnswer(questionId: Int, optionId: String) {
            answers[questionId] = optionId
        }
        override suspend fun getUserAnswers(): Map<Int, String> = answers.toMap()
        override suspend fun clearAnswers() {
            answers.clear()
        }
        override suspend fun saveAssessmentAttempt(attempt: AssessmentAttempt): Result<Unit> {
            savedAttempts.add(attempt)
            return Result.success(Unit)
        }
    }

    private class TestableCareerRepository : CareerRepository {
        val flow = MutableStateFlow<CareerRecommendation?>(null)
        val mockDatabaseRecommendations = mutableMapOf<String, CareerRecommendation>()
        var forceErrorOnLoad = false

        override suspend fun calculateRecommendation(answers: Map<Int, String>): CareerRecommendation {
            val rec = CareerRecommendation(
                careerId = "aiml_engineer",
                title = "AI / ML Engineer",
                matchPercentage = 94,
                reasonForRecommendation = "Top affinity for AI/ML",
                otherSuitableCareers = emptyList(),
                recommendedSkills = listOf("Python", "Math")
            )
            flow.value = rec
            return rec
        }

        override fun getLatestRecommendation(): Flow<CareerRecommendation?> = flow.asStateFlow()

        override suspend fun getCareerDetails(careerId: String): CareerRecommendation? = flow.value

        override suspend fun saveRecommendation(recommendation: CareerRecommendation) {
            flow.value = recommendation
        }

        override fun getAllCareers(): List<CareerRecommendation> = emptyList()

        override suspend fun loadLatestRecommendation(userId: String?): Result<CareerRecommendation?> {
            if (forceErrorOnLoad) {
                return Result.failure(RuntimeException("Supabase connection timeout"))
            }
            val key = userId ?: "user_default"
            val rec = mockDatabaseRecommendations[key] ?: flow.value
            flow.value = rec
            return Result.success(rec)
        }
    }
}
