package com.example

import com.example.data.model.AssessmentOptionDto
import com.example.data.model.AssessmentQuestionDto
import com.example.data.model.CareerDto
import com.example.data.model.CareerRecommendationDto
import com.example.data.model.CareerRecommendationMatchDto
import com.example.data.repository.MockAuthRepository
import com.example.domain.model.AssessmentAttempt
import com.example.domain.model.AssessmentOption
import com.example.domain.model.AssessmentQuestion
import com.example.domain.model.CareerRecommendation
import com.example.domain.model.OtherCareerMatch
import com.example.domain.repository.AssessmentRepository
import com.example.domain.repository.CareerRepository
import com.example.viewmodel.AssessmentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

/**
 * Unit & Integration tests for Phase 2: Assessment + Career Recommendation.
 * Verifies:
 * - Deterministic scoring algorithm precision: (65 + (ratio * 31)).toInt().coerceIn(65, 96)
 * - Ranking of primary and alternative careers
 * - DTO to Domain transformations
 * - AssessmentViewModel flow execution and state transitions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AssessmentCareerIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testAssessmentRepository: FakeAssessmentRepository
    private lateinit var testCareerRepository: FakeCareerRepository
    private lateinit var testAuthRepository: MockAuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testAssessmentRepository = FakeAssessmentRepository()
        testCareerRepository = FakeCareerRepository(testAssessmentRepository)
        testAuthRepository = MockAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDeterministicScoringAlgorithmProducesConsistentResults() = runTest(testDispatcher) {
        // Prepare answers targeted towards AI/ML Engineer
        val answers = mapOf(
            1 to "1a", // high AI/ML
            2 to "2a",
            3 to "3a"
        )

        val recommendation1 = testCareerRepository.calculateRecommendation(answers)
        val recommendation2 = testCareerRepository.calculateRecommendation(answers)

        // Algorithm MUST be 100% deterministic
        assertEquals(recommendation1.careerId, recommendation2.careerId)
        assertEquals(recommendation1.matchPercentage, recommendation2.matchPercentage)
        assertEquals(recommendation1.otherSuitableCareers.size, recommendation2.otherSuitableCareers.size)

        // Percentage must strictly obey the bounds [65, 96]
        assertTrue(recommendation1.matchPercentage in 65..96)
        recommendation1.otherSuitableCareers.forEach {
            assertTrue(it.matchPercentage in 65..96)
        }
    }

    @Test
    fun testAlternativeMatchesRankedDescending() = runTest(testDispatcher) {
        val answers = mapOf(1 to "1a", 2 to "2b", 3 to "3c")
        val recommendation = testCareerRepository.calculateRecommendation(answers)

        assertTrue(recommendation.otherSuitableCareers.isNotEmpty())
        var prevScore = recommendation.matchPercentage
        for (match in recommendation.otherSuitableCareers) {
            assertTrue("Matches must be <= top score", match.matchPercentage <= prevScore)
            prevScore = match.matchPercentage
        }
    }

    @Test
    fun testAssessmentViewModelSubmitsAndTriggersCallback() = runTest(testDispatcher) {
        val viewModel = AssessmentViewModel(
            assessmentRepository = testAssessmentRepository,
            careerRepository = testCareerRepository,
            authRepository = testAuthRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        testAssessmentRepository.saveAnswer(1, "1a")
        testAssessmentRepository.saveAnswer(2, "2a")

        var completedRecommendation: CareerRecommendation? = null
        viewModel.submitAssessment { rec ->
            completedRecommendation = rec
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(completedRecommendation)
        val user = testAuthRepository.getCurrentUser().first()
        assertEquals(true, user?.isAssessmentCompleted)
        assertEquals(completedRecommendation?.careerId, user?.recommendedCareerId)
    }

    @Test
    fun testSelectOptionUpdatesStateWithoutSubmittingOrNavigating() = runTest(testDispatcher) {
        val viewModel = AssessmentViewModel(
            assessmentRepository = testAssessmentRepository,
            careerRepository = testCareerRepository,
            authRepository = testAuthRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val initialQuestion = viewModel.uiState.value.currentQuestion
        assertNotNull(initialQuestion)
        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)
        assertFalse(viewModel.uiState.value.canGoNext)
        assertNull(viewModel.uiState.value.selectedOptionId)

        // Select an option
        viewModel.selectOption("1a")
        testDispatcher.scheduler.advanceUntilIdle()

        val stateAfterSelect = viewModel.uiState.value
        assertEquals("1a", stateAfterSelect.selectedOptionId)
        assertEquals("1a", stateAfterSelect.selectedAnswers[initialQuestion?.id])
        assertTrue(stateAfterSelect.canGoNext)
        assertEquals(0, stateAfterSelect.currentQuestionIndex) // Must NOT automatically navigate
        assertFalse(stateAfterSelect.isSubmitting) // Must NOT automatically submit
    }

    @Test
    fun testSelectOptionDoesNotCrashWhenRepositoryThrowsException() = runTest(testDispatcher) {
        val throwingRepo = object : AssessmentRepository {
            override fun getAssessmentQuestions() = testAssessmentRepository.getAssessmentQuestions()
            override suspend fun saveAnswer(questionId: Int, optionId: String) {
                throw IllegalStateException("User session is not authenticated.")
            }
            override suspend fun getUserAnswers() = emptyMap<Int, String>()
            override suspend fun saveAssessmentAttempt(attempt: AssessmentAttempt) = Result.success(Unit)
            override suspend fun clearAnswers() {}
        }

        val viewModel = AssessmentViewModel(
            assessmentRepository = throwingRepo,
            careerRepository = testCareerRepository,
            authRepository = testAuthRepository
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Selecting option MUST NOT crash when saveAnswer throws IllegalStateException
        viewModel.selectOption("1b")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("1b", state.selectedOptionId)
        assertEquals("1b", state.selectedAnswers[1])
        assertTrue(state.canGoNext)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun testCareerDtoAndAssessmentDtoSerialization() {
        val qDto = AssessmentQuestionDto(
            id = 10,
            questionNumber = 10,
            questionText = "Which field excites you most?",
            category = "Interests"
        )
        val optDto = AssessmentOptionDto(
            id = "10a",
            questionId = 10,
            text = "Neural networks and autonomous reasoning",
            scores = mapOf("aiml_engineer" to 10, "software_dev" to 3)
        )

        val domainQ = qDto.toDomain(listOf(optDto.toDomain()))
        assertEquals(10, domainQ.id)
        assertEquals("Which field excites you most?", domainQ.questionText)
        assertEquals(1, domainQ.options.size)
        assertEquals(10, domainQ.options[0].scores["aiml_engineer"])
        assertEquals(3, domainQ.options[0].scores["software_dev"])

        val cDto = CareerDto(
            id = "aiml_engineer",
            title = "AI / Machine Learning Engineer",
            roleOverview = "Builds deep learning models.",
            averageSalary = "$140,000 / yr",
            jobGrowth = "High (+24%)"
        )
        assertEquals("aiml_engineer", cDto.id)
        assertEquals("AI / Machine Learning Engineer", cDto.title)
    }

    // --- Fakes for isolated test harness ---

    class FakeAssessmentRepository : AssessmentRepository {
        private val answers = mutableMapOf<Int, String>()
        private val questions = listOf(
            AssessmentQuestion(
                id = 1,
                questionNumber = 1,
                category = "Foundations",
                questionText = "What kind of projects do you enjoy building most?",
                options = listOf(
                    AssessmentOption("1a", "Deep learning & AI models", mapOf("aiml_engineer" to 10, "data_scientist" to 8, "software_dev" to 3)),
                    AssessmentOption("1b", "Web services & backend platforms", mapOf("software_dev" to 10, "cloud_engineer" to 8, "aiml_engineer" to 2)),
                    AssessmentOption("1c", "Intuitive interfaces & user workflows", mapOf("uiux_designer" to 10, "software_dev" to 4))
                )
            ),
            AssessmentQuestion(
                id = 2,
                questionNumber = 2,
                category = "Problem Solving",
                questionText = "How do you prefer analyzing complex problems?",
                options = listOf(
                    AssessmentOption("2a", "Through statistical analysis and mathematical equations", mapOf("data_scientist" to 10, "aiml_engineer" to 9, "data_analyst" to 7)),
                    AssessmentOption("2b", "By designing scalable system architectures", mapOf("cloud_engineer" to 10, "software_dev" to 9))
                )
            ),
            AssessmentQuestion(
                id = 3,
                questionNumber = 3,
                category = "Focus Area",
                questionText = "Which work environment motivates you most?",
                options = listOf(
                    AssessmentOption("3a", "Cutting-edge AI research labs and robotics", mapOf("aiml_engineer" to 10, "data_scientist" to 7)),
                    AssessmentOption("3b", "Defending networks and critical digital assets", mapOf("cyber_analyst" to 10, "cloud_engineer" to 6)),
                    AssessmentOption("3c", "Unlocking business insights from market datasets", mapOf("data_analyst" to 10, "data_scientist" to 6))
                )
            )
        )

        override fun getAssessmentQuestions(): List<AssessmentQuestion> = questions

        override suspend fun saveAnswer(questionId: Int, optionId: String) {
            answers[questionId] = optionId
        }

        override suspend fun getUserAnswers(): Map<Int, String> = answers.toMap()

        override suspend fun saveAssessmentAttempt(attempt: AssessmentAttempt): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun clearAnswers() {
            answers.clear()
        }
    }

    class FakeCareerRepository(private val assessmentRepo: AssessmentRepository) : CareerRepository {
        private val latestFlow = MutableStateFlow<CareerRecommendation?>(null)

        override suspend fun calculateRecommendation(answers: Map<Int, String>): CareerRecommendation {
            val questions = assessmentRepo.getAssessmentQuestions()
            val allCareers = listOf("aiml_engineer", "software_dev", "data_analyst", "data_scientist", "uiux_designer", "cyber_analyst", "cloud_engineer")
            val scoreAccumulator = allCareers.associateWith { 0 }.toMutableMap()
            val maxPossibleScores = allCareers.associateWith { 0 }.toMutableMap()

            for (q in questions) {
                val careerMaxInQuestion = mutableMapOf<String, Int>()
                for (opt in q.options) {
                    opt.scores.forEach { (career, score) ->
                        careerMaxInQuestion[career] = maxOf(careerMaxInQuestion[career] ?: 0, score)
                    }
                }
                careerMaxInQuestion.forEach { (career, maxScore) ->
                    maxPossibleScores[career] = (maxPossibleScores[career] ?: 0) + maxScore
                }
            }

            for (q in questions) {
                val selectedOptionId = answers[q.id]
                val selectedOption = q.options.find { it.id == selectedOptionId }
                selectedOption?.scores?.forEach { (career, points) ->
                    scoreAccumulator[career] = (scoreAccumulator[career] ?: 0) + points
                }
            }

            val percentages = scoreAccumulator.mapValues { (career, rawScore) ->
                val maxPoints = maxPossibleScores[career] ?: 1
                if (maxPoints == 0 || rawScore == 0) {
                    75
                } else {
                    val ratio = rawScore.toFloat() / maxPoints.toFloat()
                    (65 + (ratio * 31)).toInt().coerceIn(65, 96)
                }
            }

            val sorted = percentages.entries.sortedByDescending { it.value }
            val top = sorted.first()
            val others = sorted.drop(1).take(3).map {
                OtherCareerMatch(it.key, it.key, it.value)
            }

            val rec = CareerRecommendation(
                careerId = top.key,
                title = top.key.replace("_", " ").uppercase(),
                matchPercentage = top.value,
                reasonForRecommendation = "Deterministic score alignment",
                otherSuitableCareers = others,
                recommendedSkills = listOf("Technical Aptitude", "Problem Solving")
            )
            latestFlow.value = rec
            return rec
        }

        override fun getLatestRecommendation(): Flow<CareerRecommendation?> = latestFlow

        override suspend fun getCareerDetails(careerId: String): CareerRecommendation? = null

        override suspend fun saveRecommendation(recommendation: CareerRecommendation) {
            latestFlow.value = recommendation
        }

        override fun getAllCareers(): List<CareerRecommendation> = emptyList()
    }
}
