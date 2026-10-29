package com.example

import com.example.data.repository.MockAiRoadmapGuidanceRepository
import com.example.data.repository.MockRoadmapRepository
import com.example.domain.model.AiRoadmapGuidance
import com.example.domain.model.RoadmapGuidanceContext
import com.example.domain.repository.AiRoadmapGuidanceRepository
import com.example.viewmodel.AiRoadmapGuidanceUiState
import com.example.viewmodel.RoadmapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiRoadmapGuidanceTest {

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
    fun testRoadmapContextAndGuidanceSerialization() {
        val context = RoadmapGuidanceContext(
            career = "AI / Machine Learning Engineer",
            overallProgressPercentage = 42,
            completedTasks = listOf("Python Fundamentals", "NumPy and Pandas"),
            pendingTasks = listOf("Machine Learning Fundamentals", "Deep Learning", "Model Deployment"),
            currentStage = "2. Programming",
            nextTasks = listOf("Machine Learning Fundamentals"),
            skillCategories = listOf("Python", "Machine Learning")
        )

        val encoded = json.encodeToString(RoadmapGuidanceContext.serializer(), context)
        assertTrue(encoded.contains("AI / Machine Learning Engineer"))
        assertTrue(encoded.contains("Python Fundamentals"))
        assertTrue(encoded.contains("NumPy and Pandas"))
        assertTrue(encoded.contains("Machine Learning Fundamentals"))

        val decoded = json.decodeFromString(RoadmapGuidanceContext.serializer(), encoded)
        assertEquals(context.career, decoded.career)
        assertEquals(context.overallProgressPercentage, decoded.overallProgressPercentage)
        assertEquals(context.completedTasks, decoded.completedTasks)
        assertEquals(context.pendingTasks, decoded.pendingTasks)
        assertEquals(context.currentStage, decoded.currentStage)

        val guidance = AiRoadmapGuidance(guidance = "Focus on Machine Learning Fundamentals next.")
        val guidanceJson = json.encodeToString(AiRoadmapGuidance.serializer(), guidance)
        val decodedGuidance = json.decodeFromString(AiRoadmapGuidance.serializer(), guidanceJson)
        assertEquals(guidance.guidance, decodedGuidance.guidance)
    }

    @Test
    fun testMockAiRoadmapGuidanceRepositorySuccess() = runTest {
        val repo = MockAiRoadmapGuidanceRepository(shouldFail = false, delayMs = 0)
        val context = RoadmapGuidanceContext(
            career = "Software Developer",
            overallProgressPercentage = 30,
            completedTasks = listOf("Computer Systems", "Git Workflow"),
            pendingTasks = listOf("Kotlin Object-Oriented", "Data Structures"),
            currentStage = "2. Programming",
            nextTasks = listOf("Kotlin Object-Oriented"),
            skillCategories = listOf("Foundation", "Kotlin")
        )

        val result = repo.generateRoadmapGuidance(context)
        assertTrue(result.isSuccess)
        val guidance = result.getOrThrow()
        assertNotNull(guidance.guidance)
        assertTrue(guidance.guidance.contains("30%"))
        assertTrue(guidance.guidance.contains("Kotlin Object-Oriented"))
    }

    @Test
    fun testMockAiRoadmapGuidanceRepositoryFailure() = runTest {
        val repo = MockAiRoadmapGuidanceRepository(shouldFail = true, delayMs = 0)
        val context = RoadmapGuidanceContext(
            career = "Data Analyst",
            overallProgressPercentage = 20
        )

        val result = repo.generateRoadmapGuidance(context)
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun testRoadmapViewModelAiGuidanceLifecycle() = runTest {
        val roadmapRepo = MockRoadmapRepository()
        val aiRepo = MockAiRoadmapGuidanceRepository(shouldFail = false, delayMs = 0)

        val viewModel = RoadmapViewModel(
            roadmapRepository = roadmapRepo,
            aiRoadmapGuidanceRepository = aiRepo
        )

        assertEquals(AiRoadmapGuidanceUiState.Idle, viewModel.aiGuidanceState.value)

        viewModel.loadRoadmapForCareer("aiml_engineer")
        advanceUntilIdle()

        val state = viewModel.aiGuidanceState.value
        assertTrue(state is AiRoadmapGuidanceUiState.Success)
        val success = state as AiRoadmapGuidanceUiState.Success
        assertTrue(success.guidance.guidance.isNotBlank())
    }

    @Test
    fun testRoadmapViewModelAiGuidanceErrorAndRetry() = runTest {
        val roadmapRepo = MockRoadmapRepository()
        val aiRepo = MockAiRoadmapGuidanceRepository(shouldFail = true, delayMs = 0)

        val viewModel = RoadmapViewModel(
            roadmapRepository = roadmapRepo,
            aiRoadmapGuidanceRepository = aiRepo
        )

        viewModel.loadRoadmapForCareer("software_dev")
        advanceUntilIdle()

        // Should be in Error state
        assertTrue(viewModel.aiGuidanceState.value is AiRoadmapGuidanceUiState.Error)

        // Roadmap itself should still be completely loaded and functional
        assertNotNull(viewModel.uiState.value.roadmap)
        assertEquals("Software Developer", viewModel.uiState.value.roadmap?.careerTitle)

        // Now recover AI repo and retry
        aiRepo.shouldFail = false
        viewModel.retryAiGuidance()
        advanceUntilIdle()

        assertTrue(viewModel.aiGuidanceState.value is AiRoadmapGuidanceUiState.Success)
    }

    @Test
    fun testRoadmapViewModelDuplicateRequestPrevention() = runTest {
        val roadmapRepo = MockRoadmapRepository()
        var callCount = 0

        val countingRepo = object : AiRoadmapGuidanceRepository {
            override suspend fun generateRoadmapGuidance(context: RoadmapGuidanceContext): Result<AiRoadmapGuidance> {
                callCount++
                return Result.success(AiRoadmapGuidance("Call #$callCount for ${context.career}"))
            }
        }

        val viewModel = RoadmapViewModel(
            roadmapRepository = roadmapRepo,
            aiRoadmapGuidanceRepository = countingRepo
        )

        viewModel.loadRoadmapForCareer("aiml_engineer")
        advanceUntilIdle()
        assertEquals(1, callCount)

        // Subsequent call with identical roadmap state should be skipped by cache
        viewModel.loadAiGuidance(forceRefresh = false)
        advanceUntilIdle()
        assertEquals(1, callCount)

        // Explicit retry forces a fresh call
        viewModel.retryAiGuidance()
        advanceUntilIdle()
        assertEquals(2, callCount)
    }

    @Test
    fun testPersonalizationBetweenDifferentStudentsAndCareers() = runTest {
        val repo = MockAiRoadmapGuidanceRepository(shouldFail = false, delayMs = 0)

        // Student A: AI/ML Engineer
        val studentA = RoadmapGuidanceContext(
            career = "AI / Machine Learning Engineer",
            overallProgressPercentage = 40,
            completedTasks = listOf("Python Fundamentals", "NumPy and Pandas"),
            pendingTasks = listOf("Deep Learning", "Model Deployment"),
            currentStage = "3. Core Skills",
            nextTasks = listOf("Deep Learning"),
            skillCategories = listOf("Python", "Deep Learning")
        )

        // Student B: UI/UX Designer
        val studentB = RoadmapGuidanceContext(
            career = "UI/UX Designer",
            overallProgressPercentage = 55,
            completedTasks = listOf("Figma Fundamentals", "User Research"),
            pendingTasks = listOf("Interactive Prototyping", "Usability Testing"),
            currentStage = "3. Design Systems",
            nextTasks = listOf("Interactive Prototyping"),
            skillCategories = listOf("Design", "Research")
        )

        val resultA = repo.generateRoadmapGuidance(studentA).getOrThrow()
        val resultB = repo.generateRoadmapGuidance(studentB).getOrThrow()

        assertNotEquals(resultA.guidance, resultB.guidance)

        // Verify Student A guidance contains AI/ML specific tasks and progress
        assertTrue(resultA.guidance.contains("40%"))
        assertTrue(resultA.guidance.contains("Python Fundamentals"))
        assertTrue(resultA.guidance.contains("Deep Learning"))

        // Verify Student B guidance contains UI/UX specific tasks and progress
        assertTrue(resultB.guidance.contains("55%"))
        assertTrue(resultB.guidance.contains("Figma Fundamentals"))
        assertTrue(resultB.guidance.contains("Interactive Prototyping"))
    }

    @Test
    fun testSameCareerDifferentProgressStatesProducesDifferentGuidance() = runTest {
        val repo = MockAiRoadmapGuidanceRepository(shouldFail = false, delayMs = 0)

        val earlyStageState = RoadmapGuidanceContext(
            career = "AI / Machine Learning Engineer",
            overallProgressPercentage = 20,
            completedTasks = listOf("Linear Algebra: Vectors & Matrices"),
            pendingTasks = listOf("Python Fundamentals", "Machine Learning", "Deep Learning"),
            currentStage = "1. Foundation",
            nextTasks = listOf("Python Fundamentals"),
            skillCategories = listOf("Mathematics", "Python")
        )

        val advancedStageState = RoadmapGuidanceContext(
            career = "AI / Machine Learning Engineer",
            overallProgressPercentage = 70,
            completedTasks = listOf("Python Fundamentals", "Machine Learning", "Deep Learning Fundamentals"),
            pendingTasks = listOf("MLOps: Dockerizing Models", "Capstone Project"),
            currentStage = "4. Advanced Skills",
            nextTasks = listOf("MLOps: Dockerizing Models"),
            skillCategories = listOf("MLOps", "Projects")
        )

        val earlyGuidance = repo.generateRoadmapGuidance(earlyStageState).getOrThrow()
        val advancedGuidance = repo.generateRoadmapGuidance(advancedStageState).getOrThrow()

        assertNotEquals(earlyGuidance.guidance, advancedGuidance.guidance)

        assertTrue(earlyGuidance.guidance.contains("20%"))
        assertTrue(earlyGuidance.guidance.contains("Linear Algebra"))
        assertTrue(earlyGuidance.guidance.contains("Python Fundamentals"))

        assertTrue(advancedGuidance.guidance.contains("70%"))
        assertTrue(advancedGuidance.guidance.contains("MLOps: Dockerizing Models"))
    }

    @Test
    fun testProgressChangeTriggersGuidanceUpdateWhenTaskCompleted() = runTest {
        val roadmapRepo = MockRoadmapRepository()
        var callCount = 0
        var lastCompletedMention = ""

        val dynamicRepo = object : AiRoadmapGuidanceRepository {
            override suspend fun generateRoadmapGuidance(context: RoadmapGuidanceContext): Result<AiRoadmapGuidance> {
                callCount++
                lastCompletedMention = context.completedTasks.joinToString()
                return Result.success(AiRoadmapGuidance("Progress: ${context.overallProgressPercentage}%, Completed: $lastCompletedMention"))
            }
        }

        val viewModel = RoadmapViewModel(
            roadmapRepository = roadmapRepo,
            aiRoadmapGuidanceRepository = dynamicRepo
        )

        // State 1: Load roadmap initial state
        viewModel.loadRoadmapForCareer("aiml_engineer")
        advanceUntilIdle()
        assertEquals(1, callCount)
        val initialGuidance = (viewModel.aiGuidanceState.value as AiRoadmapGuidanceUiState.Success).guidance.guidance

        // State 2: Student completes a task
        val stageId = viewModel.uiState.value.roadmap?.stages?.firstOrNull()?.id ?: "stage_math"
        val taskId = viewModel.uiState.value.roadmap?.stages?.firstOrNull()?.tasks?.firstOrNull()?.id ?: "t1_1"
        viewModel.toggleTask(stageId, taskId)
        advanceUntilIdle()

        // Call count should now be 2 because progress changed
        assertEquals(2, callCount)
        val updatedGuidance = (viewModel.aiGuidanceState.value as AiRoadmapGuidanceUiState.Success).guidance.guidance
        assertNotEquals(initialGuidance, updatedGuidance)
    }
}
