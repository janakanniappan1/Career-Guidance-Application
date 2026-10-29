package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.data.repository.MockAuthRepository
import com.example.data.repository.MockPracticeRepository
import com.example.domain.model.PracticeCategories
import com.example.domain.model.PracticeProblem
import com.example.domain.model.StudentProfile
import com.example.domain.repository.PracticeRepository
import com.example.ui.screens.PracticeScreen
import com.example.ui.theme.CGATheme
import com.example.viewmodel.PracticeCompletionFilter
import com.example.viewmodel.PracticeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PracticeCategoryFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: MockAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = MockAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createRepresentativeProblemSet(): List<PracticeProblem> {
        return listOf(
            PracticeProblem(id = "T1", title = "Technical Question 1", category = "Technical", difficulty = "Medium", description = "Desc T1", isSolved = false),
            PracticeProblem(id = "T2", title = "Technical Question 2", category = "Technical", difficulty = "Hard", description = "Desc T2", isSolved = false),
            PracticeProblem(id = "I1", title = "Interview Question 1", category = "Interview", difficulty = "Easy", description = "Desc I1", isSolved = true),
            PracticeProblem(id = "I2", title = "Interview Question 2", category = "Interview", difficulty = "Medium", description = "Desc I2", isSolved = true),
            PracticeProblem(id = "S1", title = "Scenario Question 1", category = "Scenario", difficulty = "Medium", description = "Desc S1", isSolved = true),
            PracticeProblem(id = "B1", title = "Behavioral Question 1", category = "Behavioral", difficulty = "Easy", description = "Desc B1", isSolved = true),
            PracticeProblem(id = "H1", title = "HR Question 1", category = "HR", difficulty = "Easy", description = "Desc H1", isSolved = true),
            PracticeProblem(id = "C1", title = "Coding Problem 1", category = "Coding", difficulty = "Easy", description = "Desc C1", isSolved = true),
            PracticeProblem(id = "A1", title = "Aptitude Question 1", category = "Aptitude", difficulty = "Easy", description = "Desc A1", isSolved = true)
        )
    }

    /**
     * STEP 4 — PRIMARY BUG VERIFICATION:
     * No Technical questions solved + solved questions exist in other categories (Interview, Scenario, Behavioral, HR, Coding, Aptitude)
     * -> Technical Completed count MUST strictly be 0 (NOT 2).
     */
    @Test
    fun testOriginalBugTechnicalCompletedCountRemainsZeroWhenNonTechnicalSolved() = runTest(testDispatcher) {
        val problems = createRepresentativeProblemSet()
        val customRepo = InMemoryCategoryTestRepository(problems)
        val viewModel = PracticeViewModel(practiceRepository = customRepo, authRepository = authRepository)
        advanceUntilIdle()

        // 1. Coding tab (Index 0)
        val codingProblems = viewModel.getFilteredProblems(categoryIndex = 0, filter = PracticeCompletionFilter.ALL)
        val codingCompleted = viewModel.getFilteredProblems(categoryIndex = 0, filter = PracticeCompletionFilter.COMPLETED)
        assertEquals("Coding tab must only contain Coding problems", 1, codingProblems.size)
        assertEquals("Coding tab problem must be C1", "C1", codingProblems.first().id)
        assertEquals("Coding tab completed count must be 1", 1, codingCompleted.size)

        // 2. Aptitude tab (Index 1)
        val aptitudeProblems = viewModel.getFilteredProblems(categoryIndex = 1, filter = PracticeCompletionFilter.ALL)
        val aptitudeCompleted = viewModel.getFilteredProblems(categoryIndex = 1, filter = PracticeCompletionFilter.COMPLETED)
        assertEquals("Aptitude tab must only contain Aptitude problems", 1, aptitudeProblems.size)
        assertEquals("Aptitude tab problem must be A1", "A1", aptitudeProblems.first().id)
        assertEquals("Aptitude tab completed count must be 1", 1, aptitudeCompleted.size)

        // 3. Technical tab (Index 2)
        val technicalProblems = viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.ALL)
        val technicalCompleted = viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED)
        val technicalIncomplete = viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.INCOMPLETE)

        assertEquals("Technical tab must only contain T1 and T2", 2, technicalProblems.size)
        assertTrue("Technical tab must contain T1", technicalProblems.any { it.id == "T1" })
        assertTrue("Technical tab must contain T2", technicalProblems.any { it.id == "T2" })
        assertFalse("Technical tab must NOT contain Interview questions", technicalProblems.any { it.category.equals("Interview", ignoreCase = true) })
        assertFalse("Technical tab must NOT contain Scenario questions", technicalProblems.any { it.category.equals("Scenario", ignoreCase = true) })
        assertFalse("Technical tab must NOT contain Behavioral questions", technicalProblems.any { it.category.equals("Behavioral", ignoreCase = true) })
        assertFalse("Technical tab must NOT contain HR questions", technicalProblems.any { it.category.equals("HR", ignoreCase = true) })

        // CRITICAL ASSERTION: Technical completed count MUST be 0
        assertEquals("CRITICAL: Technical completed count MUST be 0 when no Technical question is solved", 0, technicalCompleted.size)
        assertEquals("Technical incomplete count must be 2", 2, technicalIncomplete.size)
    }

    /**
     * STEP 4 UI VERIFICATION:
     * Compose test verifying the filter count pill on PracticeScreen renders "0" for Technical Completed.
     */
    @Test
    fun testPracticeScreenUiDisplaysZeroCompletedForTechnicalTab() = runTest(testDispatcher) {
        val problems = createRepresentativeProblemSet()
        val customRepo = InMemoryCategoryTestRepository(problems)
        val viewModel = PracticeViewModel(practiceRepository = customRepo, authRepository = authRepository)
        advanceUntilIdle()

        // Switch to Technical tab (tab 2)
        viewModel.setActiveTab(2)
        advanceUntilIdle()

        composeTestRule.setContent {
            CGATheme {
                PracticeScreen(
                    practiceViewModel = viewModel,
                    onNavigateBack = {},
                    onNavigateToHome = {},
                    onNavigateToRoadmap = {},
                    onNavigateToExplore = {},
                    onNavigateToProfile = {},
                    onNavigateToQuiz = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify completed filter pill displays count 0
        composeTestRule.onNodeWithTag("practice_filter_completed")
            .assertIsDisplayed()
            .assertTextContains("0", substring = true)

        // Verify all filter pill displays count 2 (only T1 and T2)
        composeTestRule.onNodeWithTag("practice_filter_all")
            .assertIsDisplayed()
            .assertTextContains("2", substring = true)
    }

    /**
     * STEP 5 REGRESSION 1 & 2:
     * Solving one Technical question -> count becomes 1.
     * Solving another Technical question -> count becomes 2.
     */
    @Test
    fun testSolvingTechnicalQuestionsIncrementsTechnicalCountSequentially() = runTest(testDispatcher) {
        val problems = createRepresentativeProblemSet()
        val customRepo = InMemoryCategoryTestRepository(problems)
        val viewModel = PracticeViewModel(practiceRepository = customRepo, authRepository = authRepository)
        advanceUntilIdle()

        assertEquals(0, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)

        // Solve T1
        customRepo.markProblemSolved("T1")
        advanceUntilIdle()
        assertEquals("Solving one Technical question must make Technical completed count 1", 1, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)

        // Solve T2
        customRepo.markProblemSolved("T2")
        advanceUntilIdle()
        assertEquals("Solving second Technical question must make Technical completed count 2", 2, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)
    }

    /**
     * STEP 5 REGRESSION 3–8:
     * Solving Interview, Scenario, Behavioral, HR, Coding, or Aptitude questions
     * does NOT change the Technical completed count.
     */
    @Test
    fun testSolvingOtherCategoriesDoesNotAffectTechnicalCount() = runTest(testDispatcher) {
        val problems = listOf(
            PracticeProblem(id = "T1", title = "Tech 1", category = "Technical", difficulty = "Easy", description = "T1", isSolved = false),
            PracticeProblem(id = "I1", title = "Interview 1", category = "Interview", difficulty = "Easy", description = "I1", isSolved = false),
            PracticeProblem(id = "S1", title = "Scenario 1", category = "Scenario", difficulty = "Easy", description = "S1", isSolved = false),
            PracticeProblem(id = "B1", title = "Behavioral 1", category = "Behavioral", difficulty = "Easy", description = "B1", isSolved = false),
            PracticeProblem(id = "H1", title = "HR 1", category = "HR", difficulty = "Easy", description = "H1", isSolved = false),
            PracticeProblem(id = "C1", title = "Coding 1", category = "Coding", difficulty = "Easy", description = "C1", isSolved = false),
            PracticeProblem(id = "A1", title = "Aptitude 1", category = "Aptitude", difficulty = "Easy", description = "A1", isSolved = false)
        )
        val customRepo = InMemoryCategoryTestRepository(problems)
        val viewModel = PracticeViewModel(practiceRepository = customRepo, authRepository = authRepository)
        advanceUntilIdle()

        assertEquals(0, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)

        // 3. Solve Interview question
        customRepo.markProblemSolved("I1")
        advanceUntilIdle()
        assertEquals("Solving Interview question must not change Technical count", 0, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)

        // 4. Solve Scenario question
        customRepo.markProblemSolved("S1")
        advanceUntilIdle()
        assertEquals("Solving Scenario question must not change Technical count", 0, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)

        // 5. Solve Behavioral question
        customRepo.markProblemSolved("B1")
        advanceUntilIdle()
        assertEquals("Solving Behavioral question must not change Technical count", 0, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)

        // 6. Solve HR question
        customRepo.markProblemSolved("H1")
        advanceUntilIdle()
        assertEquals("Solving HR question must not change Technical count", 0, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)

        // 7. Solve Coding question
        customRepo.markProblemSolved("C1")
        advanceUntilIdle()
        assertEquals("Solving Coding question must not change Technical count", 0, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)

        // 8. Solve Aptitude question
        customRepo.markProblemSolved("A1")
        advanceUntilIdle()
        assertEquals("Solving Aptitude question must not change Technical count", 0, viewModel.getFilteredProblems(categoryIndex = 2, filter = PracticeCompletionFilter.COMPLETED).size)
    }

    /**
     * STEP 5 REGRESSION 9:
     * Switching between tabs maintains each tab's own correct category and count.
     */
    @Test
    fun testSwitchingBetweenTabsMaintainsCorrectCounts() = runTest(testDispatcher) {
        val problems = createRepresentativeProblemSet()
        val customRepo = InMemoryCategoryTestRepository(problems)
        val viewModel = PracticeViewModel(practiceRepository = customRepo, authRepository = authRepository)
        advanceUntilIdle()

        // Tab 0: Coding
        viewModel.setActiveTab(0)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.activeTab)
        assertEquals(1, viewModel.getFilteredProblems(0, PracticeCompletionFilter.COMPLETED).size)

        // Tab 1: Aptitude
        viewModel.setActiveTab(1)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.activeTab)
        assertEquals(1, viewModel.getFilteredProblems(1, PracticeCompletionFilter.COMPLETED).size)

        // Tab 2: Technical
        viewModel.setActiveTab(2)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.activeTab)
        assertEquals(0, viewModel.getFilteredProblems(2, PracticeCompletionFilter.COMPLETED).size)
    }

    /**
     * STEP 5 REGRESSION 10:
     * Reopening Practice maintains counts based on persisted user progress.
     */
    @Test
    fun testReopeningPracticePreservesPersistedUserProgressCounts() = runTest(testDispatcher) {
        val user = StudentProfile(id = "usr_reopen_test", name = "Test User", email = "test@reopen.com")
        authRepository.updateProfile(user)
        advanceUntilIdle()

        val problems = listOf(
            PracticeProblem(id = "t_alpha", title = "Alpha Tech", category = "Technical", difficulty = "Easy", description = "Alpha", isSolved = true),
            PracticeProblem(id = "t_beta", title = "Beta Tech", category = "Technical", difficulty = "Easy", description = "Beta", isSolved = false),
            PracticeProblem(id = "iv_gamma", title = "Gamma Interview", category = "Interview", difficulty = "Easy", description = "Gamma", isSolved = true)
        )
        val customRepo = InMemoryCategoryTestRepository(problems)

        // First session
        val vm1 = PracticeViewModel(practiceRepository = customRepo, authRepository = authRepository)
        advanceUntilIdle()
        assertEquals(1, vm1.getFilteredProblems(2, PracticeCompletionFilter.COMPLETED).size)

        // Fresh session simulating reopening Practice
        val vm2 = PracticeViewModel(practiceRepository = customRepo, authRepository = authRepository)
        advanceUntilIdle()
        assertEquals("Reopened session must reflect persisted 1 Technical completed", 1, vm2.getFilteredProblems(2, PracticeCompletionFilter.COMPLETED).size)
        assertEquals("Reopened session must have 1 Incomplete Technical problem", 1, vm2.getFilteredProblems(2, PracticeCompletionFilter.INCOMPLETE).size)
    }

    /**
     * STEP 5 REGRESSION 11:
     * User progress isolation: User A's solved questions must never affect User B's counts.
     */
    @Test
    fun testDifferentUsersProgressIsolation() = runTest(testDispatcher) {
        val userA = StudentProfile(id = "usr_user_A", name = "User A", email = "a@cga.edu")
        val userB = StudentProfile(id = "usr_user_B", name = "User B", email = "b@cga.edu")

        val userSolvedMap = mutableMapOf(
            "usr_user_A" to setOf("T1"),
            "usr_user_B" to emptySet<String>()
        )

        val isolatedRepo = object : PracticeRepository by MockPracticeRepository(authRepository = authRepository) {
            override fun getProblems(careerId: String?): Flow<List<PracticeProblem>> {
                val base = listOf(
                    PracticeProblem(id = "T1", title = "Tech 1", category = "Technical", difficulty = "Easy", description = "T1"),
                    PracticeProblem(id = "T2", title = "Tech 2", category = "Technical", difficulty = "Easy", description = "T2")
                )
                return authRepository.getCurrentUser().map { u ->
                    val solved = userSolvedMap[u?.id] ?: emptySet()
                    base.map { p -> p.copy(isSolved = solved.contains(p.id)) }
                }
            }
        }

        // Active user: User A
        authRepository.updateProfile(userA)
        advanceUntilIdle()
        val vmA = PracticeViewModel(practiceRepository = isolatedRepo, authRepository = authRepository)
        advanceUntilIdle()
        assertEquals("User A solved T1, so completed count must be 1", 1, vmA.getFilteredProblems(2, PracticeCompletionFilter.COMPLETED).size)

        // Active user: User B
        authRepository.updateProfile(userB)
        advanceUntilIdle()
        val vmB = PracticeViewModel(practiceRepository = isolatedRepo, authRepository = authRepository)
        advanceUntilIdle()
        assertEquals("User B has solved 0 problems, so completed count must be 0", 0, vmB.getFilteredProblems(2, PracticeCompletionFilter.COMPLETED).size)
    }

    /**
     * Helper in-memory repository for precise category filter testing.
     */
    private class InMemoryCategoryTestRepository(
        initialProblems: List<PracticeProblem>
    ) : PracticeRepository by MockPracticeRepository() {
        private val problemsFlow = MutableStateFlow(initialProblems)

        fun markProblemSolved(id: String) {
            val updated = problemsFlow.value.map {
                if (it.id == id) it.copy(isSolved = true) else it
            }
            problemsFlow.value = updated
        }

        override fun getProblems(careerId: String?): Flow<List<PracticeProblem>> = problemsFlow.asStateFlow()

        override suspend fun solveProblem(id: String, selectedAnswerIndex: Int?): com.example.domain.model.PracticeProblemSolveResult {
            markProblemSolved(id)
            return com.example.domain.model.PracticeProblemSolveResult(
                isCorrect = true,
                xpAwarded = 20,
                isFirstSolve = true,
                isSolved = true,
                totalXp = 100,
                message = "Solved"
            )
        }

        override suspend fun completeProblem(id: String, selectedAnswerIndex: Int?): Boolean {
            markProblemSolved(id)
            return true
        }

        override suspend fun toggleProblemSolved(id: String): Boolean {
            markProblemSolved(id)
            return true
        }
    }
}
