package com.example

import com.example.data.repository.DefaultPracticeCatalog
import com.example.data.repository.MockAssessmentRepository
import com.example.data.repository.MockAuthRepository
import com.example.data.repository.MockCareerRepository
import com.example.data.repository.MockPracticeRepository
import com.example.data.repository.MockRoadmapRepository
import com.example.domain.model.CareerRecommendation
import com.example.domain.model.PracticeProblem
import com.example.domain.model.SkillProgressItem
import com.example.domain.model.StudentProfile
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CareerRepository
import com.example.domain.repository.PracticeRepository
import com.example.domain.repository.RoadmapRepository
import com.example.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardProgressMetricsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. Assessment Incomplete: readiness uses assessmentScore = 0
    @Test
    fun testPlacementReadiness_assessmentIncomplete() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val user = StudentProfile(
            id = "user_1",
            name = "Test Student",
            email = "student@college.edu",
            isAssessmentCompleted = false,
            currentStreak = 0,
            xp = 0
        )
        authRepo.updateProfile(user)
        val careerRepo = MockCareerRepository(MockAssessmentRepository())
        val customRoadmapRepo = object : RoadmapRepository by MockRoadmapRepository(authRepo) {
            override fun getOverallProgress(careerId: String): Flow<Int> = flowOf(0)
            override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> = flowOf(emptyList())
        }
        val practiceRepo = MockPracticeRepository(authRepo, initialSolvedProblemIds = emptySet())

        val viewModel = DashboardViewModel(authRepo, careerRepo, customRoadmapRepo, practiceRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.placementReadinessPercentage)
    }

    // 2. Assessment Complete: assessmentScore = 100 (25% contribution = 25)
    @Test
    fun testPlacementReadiness_assessmentCompleteOnly() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val user = StudentProfile(
            id = "user_1",
            name = "Test Student",
            email = "student@college.edu",
            isAssessmentCompleted = true,
            currentStreak = 0,
            xp = 100,
            recommendedCareerId = "aiml_engineer"
        )
        authRepo.updateProfile(user)
        val careerRepo = MockCareerRepository(MockAssessmentRepository())
        val customRoadmapRepo = object : RoadmapRepository by MockRoadmapRepository(authRepo) {
            override fun getOverallProgress(careerId: String): Flow<Int> = flowOf(0)
            override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> = flowOf(emptyList())
        }
        val practiceRepo = MockPracticeRepository(authRepo, initialSolvedProblemIds = emptySet())

        val viewModel = DashboardViewModel(authRepo, careerRepo, customRoadmapRepo, practiceRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 100 * 0.25 + 0 * 0.35 + 0 * 0.30 + 0 * 0.10 = 25
        assertEquals(25, state.placementReadinessPercentage)
    }

    // 3. Roadmap progress changes: 35% weight
    @Test
    fun testPlacementReadiness_roadmapProgressContributesCorrectly() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val user = StudentProfile(
            id = "user_1",
            name = "Test Student",
            email = "student@college.edu",
            isAssessmentCompleted = true, // 25
            currentStreak = 0,
            xp = 100,
            recommendedCareerId = "aiml_engineer"
        )
        authRepo.updateProfile(user)
        val careerRepo = MockCareerRepository(MockAssessmentRepository())
        // Roadmap at 40% (40 * 0.35 = 14)
        val customRoadmapRepo = object : RoadmapRepository by MockRoadmapRepository(authRepo) {
            override fun getOverallProgress(careerId: String): Flow<Int> = flowOf(40)
            override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> = flowOf(emptyList())
        }
        val practiceRepo = MockPracticeRepository(authRepo, initialSolvedProblemIds = emptySet())

        val viewModel = DashboardViewModel(authRepo, careerRepo, customRoadmapRepo, practiceRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 100 * 0.25 + 40 * 0.35 + 0 * 0.30 + 0 * 0.10 = 25 + 14 = 39
        assertEquals(39, state.placementReadinessPercentage)
    }

    // 4. Practice progress: 30% weight using actual 84 problem catalog
    @Test
    fun testPlacementReadiness_practiceProgressUses84Catalog() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val user = StudentProfile(
            id = "user_1",
            name = "Test Student",
            email = "student@college.edu",
            isAssessmentCompleted = true, // 25
            currentStreak = 0,
            xp = 100,
            recommendedCareerId = "aiml_engineer"
        )
        authRepo.updateProfile(user)
        val careerRepo = MockCareerRepository(MockAssessmentRepository())
        val customRoadmapRepo = object : RoadmapRepository by MockRoadmapRepository(authRepo) {
            override fun getOverallProgress(careerId: String): Flow<Int> = flowOf(40) // 14
            override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> = flowOf(emptyList())
        }

        // 42 out of 84 solved = 50% practice score (50 * 0.30 = 15)
        val solvedIds = DefaultPracticeCatalog.GLOBAL_PRACTICE_PROBLEMS.take(42).map { it.id }.toSet()
        val practiceRepo = MockPracticeRepository(authRepo, initialSolvedProblemIds = solvedIds)

        val viewModel = DashboardViewModel(authRepo, careerRepo, customRoadmapRepo, practiceRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 100 * 0.25 + 40 * 0.35 + 50 * 0.30 + 0 * 0.10 = 25 + 14 + 15 = 54
        assertEquals(54, state.placementReadinessPercentage)
        assertEquals(42, state.solvedPracticeProblemsCount)
        assertEquals(84, state.totalPracticeProblemsCount)
    }

    // 5. Streak / Activity score: min(currentStreak * 10, 100) * 0.10
    @Test
    fun testPlacementReadiness_streakComponent() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val user = StudentProfile(
            id = "user_1",
            name = "Test Student",
            email = "student@college.edu",
            isAssessmentCompleted = true, // 25
            currentStreak = 5, // 50 * 0.10 = 5
            xp = 200,
            recommendedCareerId = "aiml_engineer"
        )
        authRepo.updateProfile(user)
        val careerRepo = MockCareerRepository(MockAssessmentRepository())
        val customRoadmapRepo = object : RoadmapRepository by MockRoadmapRepository(authRepo) {
            override fun getOverallProgress(careerId: String): Flow<Int> = flowOf(40) // 14
            override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> = flowOf(emptyList())
        }

        // 50 out of 84 solved ~ 59.5% -> 59% (59 * 0.30 = 17.7 -> 25 + 14 + 17.7 + 5 = 61.7 -> 62)
        val solvedIds = DefaultPracticeCatalog.GLOBAL_PRACTICE_PROBLEMS.take(50).map { it.id }.toSet()
        val practiceRepo = MockPracticeRepository(authRepo, initialSolvedProblemIds = solvedIds)

        val viewModel = DashboardViewModel(authRepo, careerRepo, customRoadmapRepo, practiceRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 100 * 0.25 + 40 * 0.35 + 59 * 0.30 + 50 * 0.10 = 25 + 14 + 17.7 + 5 = 61.7 -> round to 62
        assertEquals(62, state.placementReadinessPercentage)
    }

    // 6. Streak capping at 100 for 10+ days and 100% readiness
    @Test
    fun testPlacementReadiness_streakCappedAt100() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val user = StudentProfile(
            id = "user_1",
            name = "Test Student",
            email = "student@college.edu",
            isAssessmentCompleted = true, // 25
            currentStreak = 15, // capped at 100 -> 100 * 0.10 = 10
            xp = 1000,
            recommendedCareerId = "aiml_engineer"
        )
        authRepo.updateProfile(user)
        val careerRepo = MockCareerRepository(MockAssessmentRepository())
        val customRoadmapRepo = object : RoadmapRepository by MockRoadmapRepository(authRepo) {
            override fun getOverallProgress(careerId: String): Flow<Int> = flowOf(100) // 35
            override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> = flowOf(emptyList())
        }
        val solvedIds = DefaultPracticeCatalog.GLOBAL_PRACTICE_PROBLEMS.map { it.id }.toSet()
        val practiceRepo = MockPracticeRepository(authRepo, initialSolvedProblemIds = solvedIds) // 30

        val viewModel = DashboardViewModel(authRepo, careerRepo, customRoadmapRepo, practiceRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 100 * 0.25 + 100 * 0.35 + 100 * 0.30 + 100 * 0.10 = 100
        assertEquals(100, state.placementReadinessPercentage)
    }

    // 7. Dynamic skill breakdown emission
    @Test
    fun testSkillBreakdown_emittedFromRoadmapRepo() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val user = StudentProfile(
            id = "user_1",
            name = "Test Student",
            email = "student@college.edu",
            isAssessmentCompleted = true,
            currentStreak = 3,
            xp = 150,
            recommendedCareerId = "aiml_engineer"
        )
        authRepo.updateProfile(user)
        val careerRepo = MockCareerRepository(MockAssessmentRepository())
        val dynamicSkills = listOf(
            SkillProgressItem("Python & Core Tools", 75, 3, 4),
            SkillProgressItem("Machine Learning Foundations", 50, 2, 4),
            SkillProgressItem("Deep Learning & LLMs", 25, 1, 4)
        )
        val customRoadmapRepo = object : RoadmapRepository by MockRoadmapRepository(authRepo) {
            override fun getOverallProgress(careerId: String): Flow<Int> = flowOf(50)
            override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> = flowOf(dynamicSkills)
        }
        val practiceRepo = MockPracticeRepository(authRepo, initialSolvedProblemIds = emptySet())

        val viewModel = DashboardViewModel(authRepo, careerRepo, customRoadmapRepo, practiceRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3, state.skillBreakdown.size)
        assertEquals("Python & Core Tools", state.skillBreakdown[0].skillName)
        assertEquals(75, state.skillBreakdown[0].percentage)
        assertEquals("Machine Learning Foundations", state.skillBreakdown[1].skillName)
        assertEquals(50, state.skillBreakdown[1].percentage)
    }

    // 8. User isolation test: user A data is distinct from user B
    @Test
    fun testUserIsolation_dashboardUpdatesPerUser() = runTest(testDispatcher) {
        val authRepo = MockAuthRepository()
        val userA = StudentProfile(
            id = "user_A",
            name = "Alice",
            email = "alice@college.edu",
            isAssessmentCompleted = false,
            currentStreak = 1,
            xp = 50
        )
        authRepo.updateProfile(userA)
        val careerRepo = MockCareerRepository(MockAssessmentRepository())
        val customRoadmapRepo = object : RoadmapRepository by MockRoadmapRepository(authRepo) {
            override fun getOverallProgress(careerId: String): Flow<Int> = flowOf(0)
            override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> = flowOf(emptyList())
        }
        val practiceRepo = MockPracticeRepository(authRepo, initialSolvedProblemIds = emptySet())

        val viewModel = DashboardViewModel(authRepo, careerRepo, customRoadmapRepo, practiceRepo)
        advanceUntilIdle()

        assertEquals("Alice", viewModel.uiState.value.studentName)
        assertEquals(1, viewModel.uiState.value.placementReadinessPercentage) // 0 + 0 + 0 + 10*0.10 = 1

        // Switch to User B
        val userB = StudentProfile(
            id = "user_B",
            name = "Bob",
            email = "bob@college.edu",
            isAssessmentCompleted = true,
            currentStreak = 10,
            xp = 500,
            recommendedCareerId = "software_dev"
        )
        authRepo.updateProfile(userB)
        advanceUntilIdle()

        assertEquals("Bob", viewModel.uiState.value.studentName)
        assertEquals(35, viewModel.uiState.value.placementReadinessPercentage) // 25 + 0 + 0 + 10 = 35
    }
}
