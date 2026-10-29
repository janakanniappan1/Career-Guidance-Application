package com.example

import com.example.data.repository.MockAiGuidanceRepository
import com.example.data.repository.MockAssessmentRepository
import com.example.data.repository.MockCareerRepository
import com.example.domain.model.AiCareerGuidance
import com.example.domain.model.CareerGuidanceContext
import com.example.domain.model.CareerRecommendation
import com.example.viewmodel.AiGuidanceUiState
import com.example.viewmodel.CareerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.data.repository.SupabaseAiGuidanceRepository
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiGuidanceTest {

    private val testDispatcher = StandardTestDispatcher()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testContextAndGuidanceSerialization() {
        val context = CareerGuidanceContext(
            department = "AI & Data Science",
            year = "3rd Year",
            careerInterest = "Machine Learning",
            career = "AI / Machine Learning Engineer",
            matchPercentage = 92,
            careerSkills = listOf("Python", "Deep Learning", "TensorFlow")
        )

        val jsonStr = json.encodeToString(CareerGuidanceContext.serializer(), context)
        assertTrue(jsonStr.contains("AI & Data Science"))
        assertTrue(jsonStr.contains("Machine Learning"))
        assertTrue(jsonStr.contains("AI / Machine Learning Engineer"))

        val decoded = json.decodeFromString(CareerGuidanceContext.serializer(), jsonStr)
        assertEquals(context.career, decoded.career)
        assertEquals(context.careerInterest, decoded.careerInterest)
        assertEquals(context.matchPercentage, decoded.matchPercentage)
        assertEquals(context.careerSkills, decoded.careerSkills)

        val guidance = AiCareerGuidance(guidance = "AI/ML is a strong match for your analytical problem solving.")
        val guidanceJson = json.encodeToString(AiCareerGuidance.serializer(), guidance)
        val decodedGuidance = json.decodeFromString(AiCareerGuidance.serializer(), guidanceJson)
        assertEquals("AI/ML is a strong match for your analytical problem solving.", decodedGuidance.displayGuidance)
    }

    @Test
    fun testAiCareerGuidanceBackwardCompatibilityAndDisplayProperty() {
        val withExplanationOnly = AiCareerGuidance(explanation = "Legacy explanation")
        assertEquals("Legacy explanation", withExplanationOnly.displayGuidance)

        val withGuidanceOnly = AiCareerGuidance(guidance = "Modern guidance paragraph")
        assertEquals("Modern guidance paragraph", withGuidanceOnly.displayGuidance)

        val jsonWithGuidance = """{"guidance":"Only guidance field present."}"""
        val decodedFromGuidance = json.decodeFromString(AiCareerGuidance.serializer(), jsonWithGuidance)
        assertEquals("Only guidance field present.", decodedFromGuidance.displayGuidance)
    }

    @Test
    fun testMockAiGuidanceRepositorySuccess() = runTest {
        val repo = MockAiGuidanceRepository(shouldFail = false, delayMs = 0)
        val context = CareerGuidanceContext(
            department = "Computer Science",
            year = "2nd Year",
            career = "Software Developer",
            matchPercentage = 88,
            careerSkills = listOf("Kotlin", "Data Structures")
        )

        val result = repo.generateCareerGuidance(context)
        assertTrue(result.isSuccess)
        val guidance = result.getOrThrow()
        assertNotNull(guidance.explanation)
        assertTrue(guidance.explanation.contains("Software Developer"))
        assertTrue(guidance.explanation.contains("Kotlin"))
    }

    @Test
    fun testMockAiGuidanceRepositoryFailure() = runTest {
        val repo = MockAiGuidanceRepository(shouldFail = true, delayMs = 0)
        val context = CareerGuidanceContext(
            career = "Cloud Engineer",
            matchPercentage = 80
        )

        val result = repo.generateCareerGuidance(context)
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun testCareerViewModelAiGuidanceSuccessLifecycle() = runTest {
        val assessmentRepo = MockAssessmentRepository()
        val careerRepo = MockCareerRepository(assessmentRepo)
        val aiRepo = MockAiGuidanceRepository(shouldFail = false, delayMs = 0)

        val viewModel = CareerViewModel(
            careerRepository = careerRepo,
            aiGuidanceRepository = aiRepo
        )

        val recommendation = CareerRecommendation(
            careerId = "aiml_engineer",
            title = "AI / Machine Learning Engineer",
            matchPercentage = 92,
            reasonForRecommendation = "Strong logic."
        )

        // Initial state should be Idle
        assertEquals(AiGuidanceUiState.Idle, viewModel.aiGuidanceState.value)

        // Request guidance
        viewModel.loadAiGuidance(recommendation)
        advanceUntilIdle()

        // Should transition to Success
        val state = viewModel.aiGuidanceState.value
        assertTrue(state is AiGuidanceUiState.Success)
        val success = state as AiGuidanceUiState.Success
        assertTrue(success.guidance.explanation.contains("AI / Machine Learning Engineer"))
    }

    @Test
    fun testCareerViewModelAiGuidanceErrorAndRetry() = runTest {
        val assessmentRepo = MockAssessmentRepository()
        val careerRepo = MockCareerRepository(assessmentRepo)
        val aiRepo = MockAiGuidanceRepository(shouldFail = true, delayMs = 0)

        val viewModel = CareerViewModel(
            careerRepository = careerRepo,
            aiGuidanceRepository = aiRepo
        )

        val recommendation = CareerRecommendation(
            careerId = "data_scientist",
            title = "Data Scientist",
            matchPercentage = 89,
            reasonForRecommendation = "Strong math."
        )

        // Request guidance with failing repo
        viewModel.loadAiGuidance(recommendation)
        advanceUntilIdle()

        // Should be Error
        assertTrue(viewModel.aiGuidanceState.value is AiGuidanceUiState.Error)

        // Now fix repo and retry
        aiRepo.shouldFail = false
        viewModel.retryAiGuidance(recommendation)
        advanceUntilIdle()

        // Should now be Success
        assertTrue(viewModel.aiGuidanceState.value is AiGuidanceUiState.Success)
    }

    @Test
    fun testCareerViewModelCachingPreventsRedundantCalls() = runTest {
        val assessmentRepo = MockAssessmentRepository()
        val careerRepo = MockCareerRepository(assessmentRepo)
        var callCount = 0

        val countingRepo = object : com.example.domain.repository.AiGuidanceRepository {
            override suspend fun generateCareerGuidance(context: CareerGuidanceContext): Result<AiCareerGuidance> {
                callCount++
                return Result.success(AiCareerGuidance("Guidance #$callCount for ${context.career}"))
            }
        }

        val viewModel = CareerViewModel(
            careerRepository = careerRepo,
            aiGuidanceRepository = countingRepo
        )

        val recommendation = CareerRecommendation(
            careerId = "cyber_analyst",
            title = "Cybersecurity Analyst",
            matchPercentage = 85,
            reasonForRecommendation = "Strong security interest."
        )

        // First call
        viewModel.loadAiGuidance(recommendation)
        advanceUntilIdle()
        assertEquals(1, callCount)

        // Subsequent call for the same recommendation without forceRefresh should be ignored
        viewModel.loadAiGuidance(recommendation)
        advanceUntilIdle()
        assertEquals(1, callCount)

        // Explicit retry should trigger a new call
        viewModel.retryAiGuidance(recommendation)
        advanceUntilIdle()
        assertEquals(2, callCount)
    }

    @Test
    fun testPersonalizationDifferentiationBetweenTwoStudents() = runTest {
        val repo = MockAiGuidanceRepository(shouldFail = false, delayMs = 0)

        val studentA = CareerGuidanceContext(
            department = "Computer Science & Engineering",
            year = "3rd Year",
            career = "AI / Machine Learning Engineer",
            matchPercentage = 85,
            careerSkills = listOf("Python", "Machine Learning")
        )

        val studentB = CareerGuidanceContext(
            department = "Information Technology",
            year = "2nd Year",
            career = "UI/UX Designer",
            matchPercentage = 81,
            careerSkills = listOf("Figma", "User Research")
        )

        val resultA = repo.generateCareerGuidance(studentA).getOrThrow()
        val resultB = repo.generateCareerGuidance(studentB).getOrThrow()

        // Verify distinct content
        org.junit.Assert.assertNotEquals(resultA.explanation, resultB.explanation)

        // Verify Student A specific context
        assertTrue(resultA.explanation.contains("AI / Machine Learning Engineer"))
        assertTrue(resultA.explanation.contains("Computer Science & Engineering"))
        assertTrue(resultA.explanation.contains("Python"))
        assertTrue(resultA.explanation.contains("85%"))

        // Verify Student B specific context
        assertTrue(resultB.explanation.contains("UI/UX Designer"))
        assertTrue(resultB.explanation.contains("Information Technology"))
        assertTrue(resultB.explanation.contains("Figma"))
        assertTrue(resultB.explanation.contains("81%"))
    }

    @Test
    fun testSupabaseAiGuidanceRepositorySanitizationRejectsInstructionLeakageCaseF() {
        val repo = SupabaseAiGuidanceRepository()
        val sanitizeMethod = SupabaseAiGuidanceRepository::class.java.getDeclaredMethod(
            "sanitizeCareerGuidance",
            String::class.java,
            CareerGuidanceContext::class.java
        ).apply { isAccessible = true }

        val context = CareerGuidanceContext(
            department = "AI & ML",
            year = "2",
            career = "Data Scientist",
            matchPercentage = 90,
            careerSkills = listOf("Python", "SQL")
        )

        val malformedAiOutputs = listOf(
            "Sentence 1 (Encouragement & connection to Career): Your 90% match is great! Sentence 2: Keep going.",
            "Sentence 1: You have great skills. Sentence 2: Keep learning.",
            "Encouragement: You are well suited for this job. Connection: Your skills match.",
            "Next step: Take online classes and complete projects.",
            "Instructions: Generate career guidance for student.",
            "Here is your personalized guidance: You are a strong candidate.",
            "### AI Career Analysis\n- Bullet 1\n- Bullet 2"
        )

        for (malformed in malformedAiOutputs) {
            val result = sanitizeMethod.invoke(repo, malformed, context) as String
            assertNotNull(result)
            // Prove that forbidden prompt instruction leakage is NEVER displayed to the user
            assertFalse(result.contains("Sentence 1", ignoreCase = true))
            assertFalse(result.contains("Sentence 2", ignoreCase = true))
            assertFalse(result.contains("Encouragement", ignoreCase = true))
            assertFalse(result.contains("Next step:", ignoreCase = true))
            assertFalse(result.contains("Instructions:", ignoreCase = true))
            assertFalse(result.contains("Here is", ignoreCase = true))
            assertFalse(result.startsWith("#"))
            // And prove it falls back to a clean personalized sentence
            assertTrue(result.contains("Data Scientist"))
            assertTrue(result.contains("90%"))
        }
    }

    @Test
    fun testCasesAThroughEContextHandlingInDeterministicFallback() {
        val repo = SupabaseAiGuidanceRepository()
        val fallbackMethod = SupabaseAiGuidanceRepository::class.java.getDeclaredMethod(
            "generateDeterministicCareerGuidance",
            CareerGuidanceContext::class.java
        ).apply { isAccessible = true }

        // CASE A
        val caseA = CareerGuidanceContext(
            department = "AI & ML",
            year = "2",
            career = "Data Scientist",
            matchPercentage = 90,
            careerSkills = listOf("Python", "SQL")
        )
        val resultA = fallbackMethod.invoke(repo, caseA) as String
        assertTrue(resultA.contains("Data Scientist"))
        assertTrue(resultA.contains("90%"))
        assertTrue(resultA.contains("Python and SQL"))
        assertTrue(resultA.contains("AI & ML"))

        // CASE B
        val caseB = CareerGuidanceContext(
            department = "AI & ML",
            year = "2",
            career = "Software Developer",
            matchPercentage = 75,
            careerSkills = listOf("Python", "Java")
        )
        val resultB = fallbackMethod.invoke(repo, caseB) as String
        assertTrue(resultB.contains("Software Developer"))
        assertTrue(resultB.contains("75%"))
        assertTrue(resultB.contains("Python and Java"))

        // CASE C
        val caseC = CareerGuidanceContext(
            career = "UI/UX Designer",
            matchPercentage = 82,
            careerSkills = listOf("Figma", "Design")
        )
        val resultC = fallbackMethod.invoke(repo, caseC) as String
        assertTrue(resultC.contains("UI/UX Designer"))
        assertTrue(resultC.contains("82%"))
        assertTrue(resultC.contains("Figma and Design"))

        // CASE D: Missing skills
        val caseD = CareerGuidanceContext(
            department = "Computer Science",
            year = "3rd Year",
            career = "Cloud Engineer",
            matchPercentage = 80,
            careerSkills = emptyList()
        )
        val resultD = fallbackMethod.invoke(repo, caseD) as String
        assertTrue(resultD.contains("Cloud Engineer"))
        assertTrue(resultD.contains("80%"))
        assertTrue(resultD.contains("Computer Science"))
        // Does not invent skills
        assertFalse(resultD.contains("Python"))

        // CASE E: Missing match percentage
        val caseE = CareerGuidanceContext(
            department = "Information Technology",
            year = "1st Year",
            career = "Cybersecurity Analyst",
            matchPercentage = 0,
            careerSkills = listOf("Networking")
        )
        val resultE = fallbackMethod.invoke(repo, caseE) as String
        assertTrue(resultE.contains("Cybersecurity Analyst"))
        assertTrue(resultE.contains("Networking"))
        assertFalse(resultE.contains("0%"))
    }
}
