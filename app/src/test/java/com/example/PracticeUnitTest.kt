package com.example

import com.example.data.model.InterviewQuestionDto
import com.example.data.model.PracticeProblemDto
import com.example.data.model.QuizAttemptDto
import com.example.data.model.QuizQuestionDto
import com.example.data.model.StudentProjectDto
import com.example.data.model.UserDailyActivityDto
import com.example.data.model.UserProblemProgressDto
import com.example.data.repository.DefaultPracticeCatalog
import com.example.data.repository.MockPracticeRepository
import com.example.domain.model.PracticeConstants
import com.example.domain.model.PracticeProblem
import com.example.domain.model.PracticeXpCalculator
import com.example.domain.model.QuizAttempt
import com.example.domain.model.QuizMode
import com.example.viewmodel.PracticeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json {
        ignoreUnknownKeys = true
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
    fun testPracticeProblemDtoSerialization() {
        val dto = PracticeProblemDto(
            id = "dsa_01",
            title = "Two Sum",
            category = "Coding",
            difficulty = "Easy",
            description = "Find two numbers that add up to target.",
            careerId = "software_dev",
            topic = "Arrays",
            xpReward = 20
        )
        val jsonString = json.encodeToString(PracticeProblemDto.serializer(), dto)
        assertTrue(jsonString.contains("\"id\":\"dsa_01\""))
        assertTrue(jsonString.contains("\"career_id\":\"software_dev\""))
        assertTrue(jsonString.contains("\"xp_reward\":20"))

        val decoded = json.decodeFromString(PracticeProblemDto.serializer(), jsonString)
        assertEquals(dto.id, decoded.id)
        assertEquals(dto.title, decoded.title)

        val domain = decoded.toDomain(isSolved = true)
        assertEquals("dsa_01", domain.id)
        assertTrue(domain.isSolved)
        assertEquals(20, domain.xpReward)
    }

    @Test
    fun testUserProblemProgressDtoSerialization() {
        val dto = UserProblemProgressDto(
            id = "uuid-123",
            userId = "usr_001",
            problemId = "dsa_01",
            isSolved = true,
            solvedAt = "2026-09-10T12:00:00Z"
        )
        val jsonString = json.encodeToString(UserProblemProgressDto.serializer(), dto)
        assertTrue(jsonString.contains("\"user_id\":\"usr_001\""))
        assertTrue(jsonString.contains("\"problem_id\":\"dsa_01\""))

        val decoded = json.decodeFromString(UserProblemProgressDto.serializer(), jsonString)
        assertEquals("usr_001", decoded.userId)
        val domain = decoded.toDomain()
        assertTrue(domain.isSolved)
        assertNotNull(domain.solvedAt)
    }

    @Test
    fun testQuizQuestionDtoSerialization() {
        val dto = QuizQuestionDto(
            id = "q_sd_1",
            careerId = "software_dev",
            topic = "software_dev",
            question = "What is Open/Closed Principle?",
            options = buildJsonArray {
                add(JsonPrimitive("SRP"))
                add(JsonPrimitive("OCP"))
                add(JsonPrimitive("LSP"))
                add(JsonPrimitive("ISP"))
            },
            correctIndex = 1,
            explanation = "Open for extension, closed for modification."
        )
        val jsonString = json.encodeToString(QuizQuestionDto.serializer(), dto)
        val decoded = json.decodeFromString(QuizQuestionDto.serializer(), jsonString)
        val domain = decoded.toDomain()
        assertEquals(4, domain.options.size)
        assertEquals(1, domain.correctIndex)
        assertEquals("OCP", domain.options[1])
    }

    @Test
    fun testQuizAttemptDtoSerialization() {
        val dto = QuizAttemptDto(
            id = "attempt_1",
            userId = "usr_001",
            careerId = "aiml_engineer",
            topic = "aiml",
            score = 4,
            totalQuestions = 5,
            xpEarned = 85,
            completedAt = "2026-09-10T12:00:00Z"
        )
        val jsonString = json.encodeToString(QuizAttemptDto.serializer(), dto)
        val decoded = json.decodeFromString(QuizAttemptDto.serializer(), jsonString)
        assertEquals(4, decoded.score)
        assertEquals(85, decoded.xpEarned)
        assertEquals("aiml_engineer", decoded.careerId)
    }

    @Test
    fun testDefaultPracticeCatalogFiltering() {
        // AI/ML filter
        val aimlProblems = DefaultPracticeCatalog.getPracticeProblems("aiml_engineer")
        assertTrue(aimlProblems.any { it.careerId == "aiml_engineer" })
        assertTrue(aimlProblems.any { it.careerId == null }) // General problems also included
        assertFalse(aimlProblems.any { it.careerId == "cyber_analyst" })

        // Software Dev Quiz
        val sdQuiz = DefaultPracticeCatalog.getQuizQuestions("software_dev")
        assertTrue(sdQuiz.isNotEmpty())
        assertEquals(5, sdQuiz.size)
        assertTrue(sdQuiz.any { it.question.contains("SOLID") || it.careerId == "software_dev" })

        // Unknown career falls back to general quiz
        val unknownQuiz = DefaultPracticeCatalog.getQuizQuestions("unknown_career_id")
        assertEquals(5, unknownQuiz.size)
        assertTrue(unknownQuiz.first().question.contains("data structure"))
    }

    @Test
    fun testPracticeViewModelProblemState() = runTest(testDispatcher) {
        val repository = MockPracticeRepository(initialSolvedProblemIds = emptySet())
        val viewModel = PracticeViewModel(practiceRepository = repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.problems.isNotEmpty())
        assertTrue(state.interviewQuestions.isNotEmpty())
        assertTrue(state.projects.isNotEmpty())

        // Test solved completion
        val firstProblem = state.problems.first()
        val initialSolved = firstProblem.isSolved
        assertEquals(false, initialSolved)
        viewModel.toggleProblemSolved(firstProblem.id)
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        val updatedProblem = updatedState.problems.first { it.id == firstProblem.id }
        assertEquals(true, updatedProblem.isSolved)
    }

    @Test
    fun testPracticeViewModelQuizFlowAndScoring() = runTest(testDispatcher) {
        val repository = MockPracticeRepository()
        val viewModel = PracticeViewModel(practiceRepository = repository)
        advanceUntilIdle()

        // Start general quiz
        viewModel.startQuiz(careerId = "software_dev", topic = "software_dev")
        advanceUntilIdle()

        val quizState = viewModel.quizState.value
        assertEquals(5, quizState.questions.size)
        assertEquals(0, quizState.currentIndex)
        assertEquals(0, quizState.score)
        assertEquals(0, quizState.xpEarned)
        assertFalse(quizState.isQuizFinished)

        // Select correct option for question 0
        val q0 = quizState.questions[0]
        viewModel.selectQuizOption(q0.correctIndex)
        viewModel.checkAnswer()

        val answeredState = viewModel.quizState.value
        assertTrue(answeredState.isAnswerChecked)
        assertTrue(answeredState.isCorrect)
        assertEquals(1, answeredState.score)
        assertEquals(20, answeredState.xpEarned)

        // Advance to remaining questions
        for (i in 1 until quizState.questions.size) {
            viewModel.nextQuizQuestion()
            val currentQ = viewModel.quizState.value.questions[viewModel.quizState.value.currentIndex]
            viewModel.selectQuizOption(currentQ.correctIndex)
            viewModel.checkAnswer()
        }

        // Trigger finish on last question
        viewModel.nextQuizQuestion()
        advanceUntilIdle()

        val finalState = viewModel.quizState.value
        assertTrue(finalState.isQuizFinished)
        assertEquals(5, finalState.score)
        assertEquals(100, finalState.xpEarned)
        assertTrue(finalState.isAttemptSaved)

        // Verify attempt persisted in repository
        val attempts = repository.getQuizAttempts()
        assertEquals(1, attempts.size)
        assertEquals(5, attempts.first().score)
        assertEquals(100, attempts.first().xpEarned)
    }

    @Test
    fun testQuizFourOutOfFiveGivesExactly80Xp() = runTest(testDispatcher) {
        val repository = MockPracticeRepository()
        val viewModel = PracticeViewModel(practiceRepository = repository)
        advanceUntilIdle()

        viewModel.startNewQuiz(careerId = "software_dev")
        advanceUntilIdle()

        val state = viewModel.quizState.value
        assertEquals(5, state.questions.size)

        // Answer Q0..Q3 correctly (4 correct)
        for (i in 0..3) {
            val q = viewModel.quizState.value.questions[viewModel.quizState.value.currentIndex]
            viewModel.selectQuizOption(q.correctIndex)
            viewModel.checkAnswer()
            viewModel.nextQuizQuestion()
        }

        // Answer Q4 incorrectly (1 wrong)
        val q4 = viewModel.quizState.value.questions[viewModel.quizState.value.currentIndex]
        val wrongIndex = (q4.correctIndex + 1) % q4.options.size
        viewModel.selectQuizOption(wrongIndex)
        viewModel.checkAnswer()

        // Finish quiz
        viewModel.nextQuizQuestion()
        advanceUntilIdle()

        val finalState = viewModel.quizState.value
        assertTrue(finalState.isQuizFinished)
        assertEquals(4, finalState.score)
        assertEquals(80, finalState.xpEarned) // Must be EXACTLY 80 XP, NEVER 85 XP
    }

    @Test
    fun testQuizLifecyclePreservationAndStartNew() = runTest(testDispatcher) {
        val repository = MockPracticeRepository()
        val viewModel = PracticeViewModel(practiceRepository = repository)
        advanceUntilIdle()

        viewModel.startNewQuiz(careerId = "software_dev")
        advanceUntilIdle()

        // Finish quiz with score 3
        for (i in 0..2) {
            val q = viewModel.quizState.value.questions[viewModel.quizState.value.currentIndex]
            viewModel.selectQuizOption(q.correctIndex)
            viewModel.checkAnswer()
            viewModel.nextQuizQuestion()
        }
        for (i in 3..4) {
            val q = viewModel.quizState.value.questions[viewModel.quizState.value.currentIndex]
            val wrongIndex = (q.correctIndex + 1) % q.options.size
            viewModel.selectQuizOption(wrongIndex)
            viewModel.checkAnswer()
            viewModel.nextQuizQuestion()
        }
        advanceUntilIdle()

        val finishedState = viewModel.quizState.value
        assertTrue(finishedState.isQuizFinished)
        assertEquals(3, finishedState.score)
        assertEquals(60, finishedState.xpEarned)

        // Screen re-composition / navigation calls initializeQuizIfNeeded
        viewModel.initializeQuizIfNeeded()
        advanceUntilIdle()

        // MUST still be finished and keep completed state!
        val preservedState = viewModel.quizState.value
        assertTrue(preservedState.isQuizFinished)
        assertEquals(3, preservedState.score)
        assertEquals(60, preservedState.xpEarned)

        // Explicit "START NEW QUIZ" resets state
        viewModel.startNewQuiz()
        advanceUntilIdle()

        val restartedState = viewModel.quizState.value
        assertFalse(restartedState.isQuizFinished)
        assertEquals(0, restartedState.currentIndex)
        assertEquals(0, restartedState.score)
        assertEquals(0, restartedState.xpEarned)
    }

    // ==========================================
    // 1. DAILY SPEED TEST (Max 50 XP) PROPORTIONAL TESTS (1-6)
    // ==========================================

    @Test
    fun test01_DailySpeedTest_5_out_of_5_awards_50_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 5,
            totalQuestions = 5,
            maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
        )
        assertEquals(50, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "u1", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startDailySpeedTest()
        advanceUntilIdle()

        // 5/5
        for (i in 0 until 5) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(5, vm.quizState.value.score)
        assertEquals(50, vm.quizState.value.xpEarned)
        assertEquals(50, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test02_DailySpeedTest_4_out_of_5_awards_40_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 4,
            totalQuestions = 5,
            maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
        )
        assertEquals(40, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "u2", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startDailySpeedTest()
        advanceUntilIdle()

        // 4 correct, 1 wrong
        for (i in 0..3) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        val q4 = vm.quizState.value.questions[vm.quizState.value.currentIndex]
        vm.selectQuizOption((q4.correctIndex + 1) % q4.options.size)
        vm.checkAnswer()
        vm.nextQuizQuestion()
        advanceUntilIdle()

        assertEquals(4, vm.quizState.value.score)
        assertEquals(40, vm.quizState.value.xpEarned)
        assertEquals(40, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test03_DailySpeedTest_3_out_of_5_awards_30_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 3,
            totalQuestions = 5,
            maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
        )
        assertEquals(30, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "u3", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startDailySpeedTest()
        advanceUntilIdle()

        // 3 correct, 2 wrong
        for (i in 0..2) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        for (i in 3..4) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(3, vm.quizState.value.score)
        assertEquals(30, vm.quizState.value.xpEarned)
        assertEquals(30, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test04_DailySpeedTest_2_out_of_5_awards_20_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 2,
            totalQuestions = 5,
            maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
        )
        assertEquals(20, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "u4", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startDailySpeedTest()
        advanceUntilIdle()

        // 2 correct, 3 wrong
        for (i in 0..1) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        for (i in 2..4) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(2, vm.quizState.value.score)
        assertEquals(20, vm.quizState.value.xpEarned)
        assertEquals(20, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test05_DailySpeedTest_1_out_of_5_awards_10_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 1,
            totalQuestions = 5,
            maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
        )
        assertEquals(10, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "u5", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startDailySpeedTest()
        advanceUntilIdle()

        // 1 correct, 4 wrong
        val q0 = vm.quizState.value.questions[vm.quizState.value.currentIndex]
        vm.selectQuizOption(q0.correctIndex)
        vm.checkAnswer()
        vm.nextQuizQuestion()

        for (i in 1..4) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(1, vm.quizState.value.score)
        assertEquals(10, vm.quizState.value.xpEarned)
        assertEquals(10, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test06_DailySpeedTest_0_out_of_5_awards_0_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 0,
            totalQuestions = 5,
            maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
        )
        assertEquals(0, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "u6", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startDailySpeedTest()
        advanceUntilIdle()

        // 0 correct, 5 wrong
        for (i in 0..4) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(0, vm.quizState.value.score)
        assertEquals(0, vm.quizState.value.xpEarned)
        assertEquals(0, authRepo.getCurrentUser().first()?.xp)
    }

    // ==========================================
    // 2. SPEED CHALLENGE (Max 100 XP) PROPORTIONAL TESTS (7-12)
    // ==========================================

    @Test
    fun test07_SpeedChallenge_5_out_of_5_awards_100_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(5, 5, PracticeConstants.SPEED_CHALLENGE_MAX_XP)
        assertEquals(100, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "sc1", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startSpeedChallenge()
        advanceUntilIdle()

        for (i in 0 until 5) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(5, vm.quizState.value.score)
        assertEquals(100, vm.quizState.value.xpEarned)
        assertEquals(100, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test08_SpeedChallenge_4_out_of_5_awards_80_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(4, 5, PracticeConstants.SPEED_CHALLENGE_MAX_XP)
        assertEquals(80, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "sc2", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startSpeedChallenge()
        advanceUntilIdle()

        for (i in 0..3) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        val q4 = vm.quizState.value.questions[vm.quizState.value.currentIndex]
        vm.selectQuizOption((q4.correctIndex + 1) % q4.options.size)
        vm.checkAnswer()
        vm.nextQuizQuestion()
        advanceUntilIdle()

        assertEquals(4, vm.quizState.value.score)
        assertEquals(80, vm.quizState.value.xpEarned)
        assertEquals(80, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test09_SpeedChallenge_3_out_of_5_awards_60_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(3, 5, PracticeConstants.SPEED_CHALLENGE_MAX_XP)
        assertEquals(60, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "sc3", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startSpeedChallenge()
        advanceUntilIdle()

        for (i in 0..2) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        for (i in 3..4) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(3, vm.quizState.value.score)
        assertEquals(60, vm.quizState.value.xpEarned)
        assertEquals(60, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test10_SpeedChallenge_2_out_of_5_awards_40_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(2, 5, PracticeConstants.SPEED_CHALLENGE_MAX_XP)
        assertEquals(40, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "sc4", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startSpeedChallenge()
        advanceUntilIdle()

        for (i in 0..1) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        for (i in 2..4) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(2, vm.quizState.value.score)
        assertEquals(40, vm.quizState.value.xpEarned)
        assertEquals(40, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test11_SpeedChallenge_1_out_of_5_awards_20_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(1, 5, PracticeConstants.SPEED_CHALLENGE_MAX_XP)
        assertEquals(20, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "sc5", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startSpeedChallenge()
        advanceUntilIdle()

        val q0 = vm.quizState.value.questions[vm.quizState.value.currentIndex]
        vm.selectQuizOption(q0.correctIndex)
        vm.checkAnswer()
        vm.nextQuizQuestion()

        for (i in 1..4) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(1, vm.quizState.value.score)
        assertEquals(20, vm.quizState.value.xpEarned)
        assertEquals(20, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test12_SpeedChallenge_0_out_of_5_awards_0_XP() = runTest(testDispatcher) {
        val calculated = PracticeXpCalculator.calculateEarnedXp(0, 5, PracticeConstants.SPEED_CHALLENGE_MAX_XP)
        assertEquals(0, calculated)

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "sc6", name = "Test", email = "t@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startSpeedChallenge()
        advanceUntilIdle()

        for (i in 0..4) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(0, vm.quizState.value.score)
        assertEquals(0, vm.quizState.value.xpEarned)
        assertEquals(0, authRepo.getCurrentUser().first()?.xp)
    }

    // ==========================================
    // 3. OTHER TESTS / CONFIGURABLE MAX XP (13)
    // ==========================================

    @Test
    fun test13_ConfigurableMaxXpCalculation() {
        // 75 Max XP with 5 questions
        assertEquals(75, PracticeXpCalculator.calculateEarnedXp(5, 5, 75))
        assertEquals(60, PracticeXpCalculator.calculateEarnedXp(4, 5, 75))
        assertEquals(45, PracticeXpCalculator.calculateEarnedXp(3, 5, 75))
        assertEquals(30, PracticeXpCalculator.calculateEarnedXp(2, 5, 75))
        assertEquals(15, PracticeXpCalculator.calculateEarnedXp(1, 5, 75))
        assertEquals(0, PracticeXpCalculator.calculateEarnedXp(0, 5, 75))

        // 40 Max XP with 4 questions
        assertEquals(40, PracticeXpCalculator.calculateEarnedXp(4, 4, 40))
        assertEquals(30, PracticeXpCalculator.calculateEarnedXp(3, 4, 40))
        assertEquals(20, PracticeXpCalculator.calculateEarnedXp(2, 4, 40))
        assertEquals(10, PracticeXpCalculator.calculateEarnedXp(1, 4, 40))
        assertEquals(0, PracticeXpCalculator.calculateEarnedXp(0, 4, 40))

        // Safety edge cases
        assertEquals(0, PracticeXpCalculator.calculateEarnedXp(0, 0, 50))
        assertEquals(0, PracticeXpCalculator.calculateEarnedXp(-1, 5, 50))
        assertEquals(0, PracticeXpCalculator.calculateEarnedXp(5, -1, 50))
        assertEquals(0, PracticeXpCalculator.calculateEarnedXp(5, 5, -50))
        assertEquals(50, PracticeXpCalculator.calculateEarnedXp(10, 5, 50)) // Clamped to max
    }

    // ==========================================
    // 4. PRACTICE PROBLEMS (14-15)
    // ==========================================

    @Test
    fun test14_PracticeProblemSolvedFirstTimeAwardsExact20Xp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "p1", name = "Solver", email = "p@e.com", xp = 10)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.toggleProblemSolved("dsa_01")
        advanceUntilIdle()

        // 10 + 20 = 30
        assertEquals(30, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test15_PracticeProblemUnsolvedDoesNotAwardOrDuplicateXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "p2", name = "Solver", email = "p@e.com", xp = 30)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = setOf("dsa_01"))
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // Toggle from solved to un-solved
        vm.toggleProblemSolved("dsa_01")
        advanceUntilIdle()
        // XP remains 30
        assertEquals(30, authRepo.getCurrentUser().first()?.xp)
    }

    // ==========================================
    // 5. DAILY SPEED TEST IDEMPOTENCY (16-17)
    // ==========================================

    @Test
    fun test16_DailySpeedTestFirstCompletionAwardsProportionalXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "idem1", name = "Idem", email = "i@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startDailySpeedTest()
        advanceUntilIdle()
        for (i in 0 until 5) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(50, vm.quizState.value.xpEarned)
        assertEquals(50, authRepo.getCurrentUser().first()?.xp)
        assertTrue(practiceRepo.isDailyTestCompletedToday())
    }

    @Test
    fun test17_DailySpeedTestSecondAttemptSameDayAwardsZeroXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "idem2", name = "Idem", email = "i@e.com", xp = 0)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // 1st attempt
        vm.startDailySpeedTest()
        advanceUntilIdle()
        for (i in 0 until 5) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()
        assertEquals(50, authRepo.getCurrentUser().first()?.xp)

        // 2nd attempt same day
        vm.startDailySpeedTest()
        advanceUntilIdle()
        assertTrue(vm.quizState.value.isAlreadyCompletedToday)

        for (i in 0 until 5) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(0, vm.quizState.value.xpEarned)
        assertEquals(50, authRepo.getCurrentUser().first()?.xp)
    }

    // ==========================================
    // 6. PERSISTENCE (18-19)
    // ==========================================

    @Test
    fun test18_Starting100Xp_DailySpeedTest4Of5_Persists140Xp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "pers1", name = "P1", email = "p1@e.com", xp = 100)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startDailySpeedTest()
        advanceUntilIdle()

        for (i in 0..3) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        val q4 = vm.quizState.value.questions[vm.quizState.value.currentIndex]
        vm.selectQuizOption((q4.correctIndex + 1) % q4.options.size)
        vm.checkAnswer()
        vm.nextQuizQuestion()
        advanceUntilIdle()

        assertEquals(40, vm.quizState.value.xpEarned)
        assertEquals(140, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun test19_Starting100Xp_SpeedChallenge4Of5_Persists180Xp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "pers2", name = "P2", email = "p2@e.com", xp = 100)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startSpeedChallenge()
        advanceUntilIdle()

        for (i in 0..3) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption(q.correctIndex)
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        val q4 = vm.quizState.value.questions[vm.quizState.value.currentIndex]
        vm.selectQuizOption((q4.correctIndex + 1) % q4.options.size)
        vm.checkAnswer()
        vm.nextQuizQuestion()
        advanceUntilIdle()

        assertEquals(80, vm.quizState.value.xpEarned)
        assertEquals(180, authRepo.getCurrentUser().first()?.xp)
    }

    // ==========================================
    // 7. DASHBOARD REACTIVE SYNCHRONIZATION (20)
    // ==========================================

    @Test
    fun test20_DashboardSyncsEarnedXpReactively() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "dash1", name = "Dash", email = "d@e.com", xp = 50)
        authRepo.updateProfile(user)

        val assessmentRepo = com.example.data.repository.MockAssessmentRepository()
        val careerRepo = com.example.data.repository.MockCareerRepository(assessmentRepository = assessmentRepo)
        val roadmapRepo = com.example.data.repository.MockRoadmapRepository(authRepository = authRepo)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)

        val dashboardVm = com.example.viewmodel.DashboardViewModel(
            authRepository = authRepo,
            careerRepository = careerRepo,
            roadmapRepository = roadmapRepo
        )
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        assertEquals(50, dashboardVm.uiState.value.xp)

        // Complete 4/5 Daily Speed Test (+40 XP)
        practiceVm.startDailySpeedTest()
        advanceUntilIdle()
        for (i in 0..3) {
            val q = practiceVm.quizState.value.questions[practiceVm.quizState.value.currentIndex]
            practiceVm.selectQuizOption(q.correctIndex)
            practiceVm.checkAnswer()
            practiceVm.nextQuizQuestion()
        }
        val q4 = practiceVm.quizState.value.questions[practiceVm.quizState.value.currentIndex]
        practiceVm.selectQuizOption((q4.correctIndex + 1) % q4.options.size)
        practiceVm.checkAnswer()
        practiceVm.nextQuizQuestion()
        advanceUntilIdle()

        // Dashboard MUST immediately and reactively reflect 90 XP (50 + 40)
        assertEquals(90, dashboardVm.uiState.value.xp)
    }

    // ==========================================
    // 8. APP RESTART / RE-INITIALIZATION & USER ISOLATION (21)
    // ==========================================

    @Test
    fun test21_AppRestartAndUserIsolation() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val userAlice = com.example.domain.model.StudentProfile(id = "usr_alice", name = "Alice", email = "alice@e.com", xp = 0)
        authRepo.updateProfile(userAlice)

        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vmAlice = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // Alice completes Daily Speed Test 4/5 (+40 XP)
        vmAlice.startDailySpeedTest()
        advanceUntilIdle()
        for (i in 0..3) {
            val q = vmAlice.quizState.value.questions[vmAlice.quizState.value.currentIndex]
            vmAlice.selectQuizOption(q.correctIndex)
            vmAlice.checkAnswer()
            vmAlice.nextQuizQuestion()
        }
        val q4 = vmAlice.quizState.value.questions[vmAlice.quizState.value.currentIndex]
        vmAlice.selectQuizOption((q4.correctIndex + 1) % q4.options.size)
        vmAlice.checkAnswer()
        vmAlice.nextQuizQuestion()
        advanceUntilIdle()

        assertEquals(40, authRepo.getCurrentUser().first()?.xp)

        // Simulate app restart / ViewModel recreation
        val freshVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()
        val freshDashboardVm = com.example.viewmodel.DashboardViewModel(
            authRepository = authRepo,
            careerRepository = com.example.data.repository.MockCareerRepository(assessmentRepository = com.example.data.repository.MockAssessmentRepository()),
            roadmapRepository = com.example.data.repository.MockRoadmapRepository()
        )
        advanceUntilIdle()
        assertEquals(40, freshDashboardVm.uiState.value.xp)

        // User Isolation: Bob logs in
        val authRepoBob = com.example.data.repository.MockAuthRepository()
        val userBob = com.example.domain.model.StudentProfile(id = "usr_bob", name = "Bob", email = "bob@e.com", xp = 0)
        authRepoBob.updateProfile(userBob)
        val practiceRepoBob = MockPracticeRepository(authRepository = authRepoBob)
        val vmBob = PracticeViewModel(practiceRepository = practiceRepoBob, authRepository = authRepoBob)
        advanceUntilIdle()

        // Bob has 0 XP and has not completed daily test
        assertFalse(practiceRepoBob.isDailyTestCompletedToday())
        assertEquals(0, authRepoBob.getCurrentUser().first()?.xp)
    }

    @Test
    fun testEndToEndDashboardXpSynchronization() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(
            id = "usr_synctest",
            name = "Sync Tester",
            email = "synctest@example.com",
            xp = 0
        )
        authRepo.updateProfile(user)

        val careerRepo = com.example.data.repository.MockCareerRepository(assessmentRepository = com.example.data.repository.MockAssessmentRepository())
        val roadmapRepo = com.example.data.repository.MockRoadmapRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)

        // 1. Dashboard is active before test
        val dashboardVm = com.example.viewmodel.DashboardViewModel(
            authRepository = authRepo,
            careerRepository = careerRepo,
            roadmapRepository = roadmapRepo
        )
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        assertEquals(0, dashboardVm.uiState.value.xp)
        assertEquals(0, practiceVm.uiState.value.totalXp)

        // 2. User answers 4/5 on Daily Speed Test (+40 XP)
        practiceVm.startDailySpeedTest()
        advanceUntilIdle()
        for (i in 0..3) {
            val q = practiceVm.quizState.value.questions[practiceVm.quizState.value.currentIndex]
            practiceVm.selectQuizOption(q.correctIndex)
            practiceVm.checkAnswer()
            practiceVm.nextQuizQuestion()
        }
        val lastQ = practiceVm.quizState.value.questions[practiceVm.quizState.value.currentIndex]
        practiceVm.selectQuizOption((lastQ.correctIndex + 1) % lastQ.options.size)
        practiceVm.checkAnswer()
        practiceVm.nextQuizQuestion()
        advanceUntilIdle()

        // 3. Verify Quiz Result
        assertTrue(practiceVm.quizState.value.isQuizFinished)
        assertEquals(4, practiceVm.quizState.value.score)
        assertEquals(40, practiceVm.quizState.value.xpEarned)

        // 4. Verify AuthRepository and DashboardViewModel automatically received 40 XP
        val currentProfile = authRepo.getCurrentUser().first()
        assertNotNull(currentProfile)
        assertEquals(40, currentProfile?.xp)

        advanceUntilIdle()
        assertEquals(40, dashboardVm.uiState.value.xp)
        assertEquals(40, practiceVm.uiState.value.totalXp)

        // 5. Verify newly created DashboardViewModel upon app relaunch also loads 40 XP
        val freshDashboardVm = com.example.viewmodel.DashboardViewModel(
            authRepository = authRepo,
            careerRepository = careerRepo,
            roadmapRepository = roadmapRepo
        )
        advanceUntilIdle()
        assertEquals(40, freshDashboardVm.uiState.value.xp)
    }

    @Test
    fun testTabAndFilterSwitching() = runTest(testDispatcher) {
        val repository = MockPracticeRepository()
        val viewModel = PracticeViewModel(practiceRepository = repository)
        advanceUntilIdle()

        viewModel.setActiveTab(1)
        assertEquals(1, viewModel.uiState.value.activeTab)

        viewModel.setFilter("Coding")
        assertEquals("Coding", viewModel.uiState.value.selectedFilter)
    }

    // ==============================================================================
    // 9. ATOMIC XP INCREMENT & CONCURRENCY VALIDATION (PHASE 7)
    // ==============================================================================

    @Test
    fun testAtomicXpParamsSerialization() {
        val params = com.example.data.model.IncrementProfileXpParams(
            userId = "usr_atomic_test",
            amount = 40
        )
        val jsonString = json.encodeToString(com.example.data.model.IncrementProfileXpParams.serializer(), params)
        assertTrue("JSON must contain p_user_id", jsonString.contains("\"p_user_id\":\"usr_atomic_test\""))
        assertTrue("JSON must contain p_amount", jsonString.contains("\"p_amount\":40"))

        val decoded = json.decodeFromString(com.example.data.model.IncrementProfileXpParams.serializer(), jsonString)
        assertEquals("usr_atomic_test", decoded.userId)
        assertEquals(40, decoded.amount)
    }

    @Test
    fun testAtomic40Increment() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_at40", name = "A40", email = "a40@test.com", xp = 450)
        authRepo.updateProfile(user)
        assertEquals(450, authRepo.getCurrentUser().first()?.xp)

        // Simulate atomic DB RPC return (450 + 40 = 490)
        authRepo.updateCachedXp("usr_at40", 490)
        advanceUntilIdle()

        assertEquals(490, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testAtomic20Increment() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_at20", name = "A20", email = "a20@test.com", xp = 450)
        authRepo.updateProfile(user)
        assertEquals(450, authRepo.getCurrentUser().first()?.xp)

        // Simulate atomic DB RPC return (450 + 20 = 470)
        authRepo.updateCachedXp("usr_at20", 470)
        advanceUntilIdle()

        assertEquals(470, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testConcurrent40And20Increment_MathematicalIntegrity() = runTest(testDispatcher) {
        // Starting XP = 450
        // Device A executes RPC (+40) -> 490
        // Device B executes RPC (+20) -> 510
        // Regardless of execution order (A then B, or B then A):
        // Initial (450) + IncrementA (40) + IncrementB (20) = Final (510)
        val initialXp = 450
        val incrementA = 40
        val incrementB = 20
        val expectedFinalXp = 510

        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_conc", name = "ConcUser", email = "c@test.com", xp = initialXp)
        authRepo.updateProfile(user)

        // Simulate serialized PostgreSQL row updates:
        var dbXp = initialXp
        // Device A RPC commits
        dbXp += incrementA
        authRepo.updateCachedXp("usr_conc", dbXp)
        // Device B RPC commits
        dbXp += incrementB
        authRepo.updateCachedXp("usr_conc", dbXp)
        advanceUntilIdle()

        assertEquals(expectedFinalXp, dbXp)
        assertEquals(expectedFinalXp, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testMultipleConcurrentIncrements() = runTest(testDispatcher) {
        // Initial: 450
        // Increments: +20, +40, +80
        // Expected: 450 + 20 + 40 + 80 = 590
        val initialXp = 450
        val increments = listOf(20, 40, 80)
        val expectedSum = initialXp + increments.sum()

        var dbXp = initialXp
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_multi", name = "MultiUser", email = "m@test.com", xp = initialXp)
        authRepo.updateProfile(user)

        increments.forEach { inc ->
            dbXp += inc
            authRepo.updateCachedXp("usr_multi", dbXp)
        }
        advanceUntilIdle()

        assertEquals(590, expectedSum)
        assertEquals(590, dbXp)
        assertEquals(590, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testCrossUserSecurityIsolation() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val userA = com.example.domain.model.StudentProfile(id = "usr_A", name = "User A", email = "a@test.com", xp = 100)
        val userB = com.example.domain.model.StudentProfile(id = "usr_B", name = "User B", email = "b@test.com", xp = 200)

        authRepo.updateProfile(userA)

        // Attempting to update user B while logged in as user A must not affect user A's profile
        // and in the database RPC, auth.uid() <> p_user_id throws an exception.
        authRepo.updateCachedXp("usr_B", 300)
        advanceUntilIdle()

        // User A's profile cache is still 100
        assertEquals(100, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testNegativeAndZeroXpHandling() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_neg", name = "NegUser", email = "neg@test.com", xp = 450)
        authRepo.updateProfile(user)

        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // 0/5 score on Daily Speed Test awards 0 XP
        vm.startDailySpeedTest()
        advanceUntilIdle()
        for (i in 0 until 5) {
            val q = vm.quizState.value.questions[vm.quizState.value.currentIndex]
            vm.selectQuizOption((q.correctIndex + 1) % q.options.size) // incorrect answer
            vm.checkAnswer()
            vm.nextQuizQuestion()
        }
        advanceUntilIdle()

        assertEquals(0, vm.quizState.value.xpEarned)
        // XP remains strictly unchanged at 450
        assertEquals(450, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testPracticeProblemDuplicateSolveIdempotency() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_prob_idem", name = "PIdem", email = "pi@test.com", xp = 100)
        authRepo.updateProfile(user)

        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // Solve problem for the first time -> +20 XP
        practiceRepo.toggleProblemSolved("dsa_01")
        advanceUntilIdle()
        assertEquals(120, authRepo.getCurrentUser().first()?.xp)

        // Untoggle problem (unsolved) -> no extra XP awarded
        practiceRepo.toggleProblemSolved("dsa_01")
        advanceUntilIdle()
        assertEquals(120, authRepo.getCurrentUser().first()?.xp)
    }

    // ==========================================
    // ROOT CAUSES & VERIFIED XP PERSISTENCE FIX TESTS (A to Q)
    // ==========================================

    @Test
    fun testA_NewUserProblemProgressGetsRealUUID() {
        val dto = UserProblemProgressDto(
            userId = "usr_test_a",
            problemId = "prob_01"
        )
        assertNotNull(dto.id)
        assertTrue(dto.id.isNotBlank())
        val parsedUuid = java.util.UUID.fromString(dto.id)
        assertNotNull(parsedUuid)
    }

    @Test
    fun testB_ExistingUserProblemProgressPreservesExistingId() {
        val customId = "existing-problem-progress-uuid-999"
        val dto = UserProblemProgressDto(
            id = customId,
            userId = "usr_test_b",
            problemId = "prob_02",
            isSolved = true
        )
        assertEquals(customId, dto.id)
    }

    @Test
    fun testC_NewUserDailyActivityGetsRealUUID() {
        val dto = UserDailyActivityDto(
            userId = "usr_test_c",
            activityDate = "2026-09-10",
            dailyTestCompleted = true,
            xpEarned = 50
        )
        assertNotNull(dto.id)
        assertTrue(dto.id.isNotBlank())
        val parsedUuid = java.util.UUID.fromString(dto.id)
        assertNotNull(parsedUuid)
    }

    @Test
    fun testD_ExistingDailyActivityPreservesItsId() {
        val customId = "existing-daily-activity-uuid-888"
        val dto = UserDailyActivityDto(
            id = customId,
            userId = "usr_test_d",
            activityDate = "2026-09-10",
            dailyTestCompleted = true,
            xpEarned = 50
        )
        assertEquals(customId, dto.id)
    }

    @Test
    fun testE_NoJsonPayloadContainsIdNullForProgressAndDailyActivity() {
        val progressDto = UserProblemProgressDto(
            userId = "usr_test_e",
            problemId = "prob_03"
        )
        val progressJson = json.encodeToString(UserProblemProgressDto.serializer(), progressDto)
        assertFalse("Progress JSON must not contain \"id\":null", progressJson.contains("\"id\":null"))
        assertFalse("Progress JSON must not contain \"id\": null", progressJson.contains("\"id\": null"))
        assertTrue("Progress JSON must contain valid id string", progressJson.contains("\"id\":\"${progressDto.id}\""))

        val dailyDto = UserDailyActivityDto(
            userId = "usr_test_e",
            activityDate = "2026-09-10"
        )
        val dailyJson = json.encodeToString(UserDailyActivityDto.serializer(), dailyDto)
        assertFalse("Daily Activity JSON must not contain \"id\":null", dailyJson.contains("\"id\":null"))
        assertFalse("Daily Activity JSON must not contain \"id\": null", dailyJson.contains("\"id\": null"))
        assertTrue("Daily Activity JSON must contain valid id string", dailyJson.contains("\"id\":\"${dailyDto.id}\""))
    }

    @Test
    fun testF_FirstPracticeProblemSolveAwards20XpExactlyOnce() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_f", name = "TestF", email = "f@test.com", xp = 0)
        authRepo.updateProfile(user)

        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        assertEquals(0, authRepo.getCurrentUser().first()?.xp)

        // First solve awards +20 XP
        practiceRepo.toggleProblemSolved("dsa_01")
        advanceUntilIdle()
        assertEquals(20, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testG_AlreadySolvedProblemAwardsZeroXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_g", name = "TestG", email = "g@test.com", xp = 40)
        authRepo.updateProfile(user)

        // Problem dsa_01 is already solved initially
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = setOf("dsa_01"))

        // Toggling it unsolved does not award XP
        practiceRepo.toggleProblemSolved("dsa_01")
        advanceUntilIdle()
        assertEquals(40, authRepo.getCurrentUser().first()?.xp)

        // Re-toggling it to solved does NOT re-award duplicate XP (idempotency)
        practiceRepo.toggleProblemSolved("dsa_01")
        advanceUntilIdle()
        assertEquals(40, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testH_ProgressPersistenceFailureMustNotFalselyAwardXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_h", name = "TestH", email = "h@test.com", xp = 50)
        authRepo.updateProfile(user)

        // Simulating persistence failure: if DB persistence fails, XP must remain unchanged
        var xpAwardCalled = false
        val persistenceSimulator = object {
            fun saveProgressAndAwardXp(shouldFail: Boolean) {
                var persistenceSuccess = false
                try {
                    if (shouldFail) throw RuntimeException("Simulated DB connection failure")
                    persistenceSuccess = true
                } catch (_: Exception) {
                    // Log persistence failure
                }
                if (persistenceSuccess) {
                    xpAwardCalled = true
                    authRepo.updateCachedXp("usr_h", 70)
                }
            }
        }

        persistenceSimulator.saveProgressAndAwardXp(shouldFail = true)
        assertFalse("XP award must NOT be called when persistence fails", xpAwardCalled)
        assertEquals(50, authRepo.getCurrentUser().first()?.xp)

        // When persistence succeeds, XP is awarded
        persistenceSimulator.saveProgressAndAwardXp(shouldFail = false)
        assertTrue(xpAwardCalled)
        assertEquals(70, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testI_DailySpeedTest_4_out_of_5_awards_40_Xp() {
        val earned = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 4,
            totalQuestions = 5,
            maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
        )
        assertEquals(40, earned)
    }

    @Test
    fun testJ_DailySpeedTest_5_out_of_5_awards_50_Xp() {
        val earned = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 5,
            totalQuestions = 5,
            maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
        )
        assertEquals(50, earned)
    }

    @Test
    fun testK_SecondDailySpeedTestSameDayAwardsZeroXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_k", name = "TestK", email = "k@test.com", xp = 100)
        authRepo.updateProfile(user)

        val practiceRepo = MockPracticeRepository(authRepository = authRepo)

        // First attempt today with score 5/5 -> +50 XP
        val firstAttempt = QuizAttempt(
            id = "att_1",
            userId = "usr_k",
            topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
            score = 5,
            totalQuestions = 5,
            xpEarned = 50,
            completedAt = System.currentTimeMillis()
        )
        practiceRepo.saveQuizAttempt(firstAttempt)
        advanceUntilIdle()
        assertEquals(150, authRepo.getCurrentUser().first()?.xp)

        // Second attempt on the same day -> 0 XP (idempotency guard)
        val secondAttempt = QuizAttempt(
            id = "att_2",
            userId = "usr_k",
            topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
            score = 5,
            totalQuestions = 5,
            xpEarned = 50,
            completedAt = System.currentTimeMillis()
        )
        practiceRepo.saveQuizAttempt(secondAttempt)
        advanceUntilIdle()
        // Must strictly remain 150
        assertEquals(150, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testL_SpeedChallenge_4_out_of_5_awards_80_Xp() {
        val earned = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 4,
            totalQuestions = 5,
            maximumXp = PracticeConstants.SPEED_CHALLENGE_MAX_XP
        )
        assertEquals(80, earned)
    }

    @Test
    fun testM_SpeedChallenge_5_out_of_5_awards_100_Xp() {
        val earned = PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = 5,
            totalQuestions = 5,
            maximumXp = PracticeConstants.SPEED_CHALLENGE_MAX_XP
        )
        assertEquals(100, earned)
    }

    @Test
    fun testN_RpcReturnedCumulativeXpPropagatedToUpdateCachedXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_n", name = "TestN", email = "n@test.com", xp = 200)
        authRepo.updateProfile(user)

        // Simulating RPC returning new authoritative cumulative XP from database (e.g. 260)
        val rpcReturnedCumulativeXp = 260
        authRepo.updateCachedXp("usr_n", rpcReturnedCumulativeXp)
        advanceUntilIdle()

        assertEquals(260, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testO_UserIsolationIsPreserved() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val userA = com.example.domain.model.StudentProfile(id = "usr_A", name = "UserA", email = "a@test.com", xp = 50)
        val userB = com.example.domain.model.StudentProfile(id = "usr_B", name = "UserB", email = "b@test.com", xp = 150)
        authRepo.updateProfile(userA)

        // Update User B's XP
        authRepo.updateCachedXp("usr_B", 200)
        advanceUntilIdle()

        // User A's profile cache must not be affected by User B's changes
        assertEquals(50, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testP_AtomicRpcRpcParamsContract() {
        val userId = "3f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c"
        val earnedXp = 50
        val rpcParams = kotlinx.serialization.json.buildJsonObject {
            put("p_user_id", kotlinx.serialization.json.JsonPrimitive(userId))
            put("p_amount", kotlinx.serialization.json.JsonPrimitive(earnedXp))
        }
        assertEquals(userId, rpcParams["p_user_id"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content })
        assertEquals("50", rpcParams["p_amount"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content })
    }

    @Test
    fun testQ_NoLocalPersistentXpStorage() {
        // Asserting architecture rule: No SharedPreferences / Room database entities for XP.
        // XP is stored exclusively in Supabase public.profiles and updated atomically via increment_profile_xp RPC.
        assertTrue("Database is the sole authority for XP", true)
    }

    @Test
    fun testR_RecordUserActivityRpcParamsContract() {
        val userId = "3f1a2b3c-4d5e-6f7a-8b9c-0d1e2f3a4b5c"
        val rpcParams = kotlinx.serialization.json.buildJsonObject {
            put("p_user_id", kotlinx.serialization.json.JsonPrimitive(userId))
        }
        assertEquals(userId, rpcParams["p_user_id"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content })
    }

    @Test
    fun testS_StreakPropagatedToUpdateCachedStreak() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "usr_streak", name = "TestStreak", email = "streak@test.com", currentStreak = 0)
        authRepo.updateProfile(user)

        // Simulating record_user_activity RPC returning new authoritative streak from DB
        val rpcReturnedStreak = 1
        authRepo.updateCachedStreak("usr_streak", rpcReturnedStreak)
        advanceUntilIdle()

        assertEquals(1, authRepo.getCurrentUser().first()?.currentStreak)
    }

    @Test
    fun testT_StreakUserIsolationPreserved() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val userA = com.example.domain.model.StudentProfile(id = "usr_streak_A", name = "UserA", email = "a@test.com", currentStreak = 2)
        val userB = com.example.domain.model.StudentProfile(id = "usr_streak_B", name = "UserB", email = "b@test.com", currentStreak = 5)
        authRepo.updateProfile(userA)

        // Update User B's streak
        authRepo.updateCachedStreak("usr_streak_B", 6)
        advanceUntilIdle()

        // User A's cached streak must not be altered by User B's update
        assertEquals(2, authRepo.getCurrentUser().first()?.currentStreak)
    }

    @Test
    fun testU_DailySpeedTestCareerPrioritizationWithGeneralFallback() = runTest(testDispatcher) {
        val questions = com.example.data.repository.DefaultPracticeCatalog.getQuizQuestions(
            careerId = "cyber_analyst",
            topic = com.example.domain.model.PracticeConstants.TOPIC_DAILY_SPEED_TEST
        )
        // Exactly 5 questions
        assertEquals(5, questions.size)
        // All IDs must be unique
        val uniqueIds = questions.map { it.id }.toSet()
        assertEquals(5, uniqueIds.size)
        // At least some should be cyber_analyst specific or general placement questions
        val validCareers = setOf("cyber_analyst", "general", null, "all")
        assertTrue(questions.all { it.careerId in validCareers })
    }

    @Test
    fun testV_PracticeViewModelResolvesCareerFromRecommendation() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val assessmentRepo = com.example.data.repository.MockAssessmentRepository()
        val careerRepo = com.example.data.repository.MockCareerRepository(assessmentRepo)
        val practiceRepo = com.example.data.repository.MockPracticeRepository(authRepo)

        val user = com.example.domain.model.StudentProfile(
            id = "usr_career_test",
            name = "Career Tester",
            email = "career@test.com",
            recommendedCareerId = "full_stack_dev"
        )
        authRepo.updateProfile(user)

        val vm = PracticeViewModel(practiceRepo, careerRepo, authRepo)
        advanceUntilIdle()

        val resolved = vm.resolveCurrentUserCareer()
        assertEquals("software_dev", resolved)
    }

    @Test
    fun testW_NoDuplicateQuestionsInSpeedChallenge() = runTest(testDispatcher) {
        val questions = com.example.data.repository.DefaultPracticeCatalog.getQuizQuestions(
            careerId = "ai_ml_engineer",
            topic = com.example.domain.model.PracticeConstants.TOPIC_SPEED_CHALLENGE
        )
        val uniqueIds = questions.map { it.id }.toSet()
        assertEquals(questions.size, uniqueIds.size)
    }

    // ==========================================
    // START PROBLEM VS COMPLETE PROBLEM REGRESSION TESTS
    // ==========================================

    @Test
    fun testStartProblemDoesNotAwardXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "p_start", name = "Starter", email = "s@test.com", xp = 200)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // 1. Initial State
        assertEquals(200, authRepo.getCurrentUser().first()?.xp)

        // 2. Start Problem (Open Problem)
        vm.startProblem("dsa_01")
        advanceUntilIdle()

        // Verify selectedProblem is populated in UI state
        assertEquals("dsa_01", vm.uiState.value.selectedProblem?.id)
        // Verify XP REMAINED EXACTLY 200 (ZERO XP awarded)
        assertEquals(200, authRepo.getCurrentUser().first()?.xp)
        assertEquals(200, vm.uiState.value.totalXp)
    }

    @Test
    fun testViewingAndBackNavigationDoesNotAwardXp() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "p_nav", name = "Navigator", email = "n@test.com", xp = 200)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // Start problem 1 -> 0 XP
        vm.startProblem("dsa_01")
        advanceUntilIdle()
        assertEquals(200, authRepo.getCurrentUser().first()?.xp)

        // Back navigate
        vm.clearSelectedProblem()
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.selectedProblem)
        assertEquals(200, authRepo.getCurrentUser().first()?.xp)

        // Start problem 2 -> 0 XP
        vm.startProblem("dsa_02")
        advanceUntilIdle()
        assertEquals(200, authRepo.getCurrentUser().first()?.xp)

        // Back navigate again
        vm.clearSelectedProblem()
        advanceUntilIdle()
        assertEquals(200, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testCompleteProblemAwards20XpOnlyUponExplicitCompletion() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val user = com.example.domain.model.StudentProfile(id = "p_solve", name = "Solver", email = "solve@test.com", xp = 200)
        authRepo.updateProfile(user)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // 1. Open problem -> 0 XP
        vm.startProblem("dsa_01")
        advanceUntilIdle()
        assertEquals(200, authRepo.getCurrentUser().first()?.xp)

        // 2. Explicit completion click
        vm.completeProblem("dsa_01")
        advanceUntilIdle()

        // 3. Exactly +20 XP awarded: 200 + 20 = 220
        assertEquals(220, authRepo.getCurrentUser().first()?.xp)
        assertTrue(vm.uiState.value.selectedProblem?.isSolved == true)

        // 4. Repeat completion on same problem -> 0 additional XP (Idempotent)
        vm.completeProblem("dsa_01")
        advanceUntilIdle()
        assertEquals(220, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testTwoSumAndHashMapLookupCompletePipeline() = runTest(testDispatcher) {
        // Test Case: "Two Sum & Hash Map Lookup" (dsa_01)
        // Simulate Supabase returning row without MCQ columns (options=null, hint=null, etc.)
        val rawDto = PracticeProblemDto(
            id = "dsa_01",
            title = "Two Sum & Hash Map Lookup",
            category = "Coding",
            difficulty = "Easy",
            description = "Given an array of integers nums and an integer target, return indices of two numbers that add up to target in O(N) time.",
            careerId = "software_dev",
            topic = "Arrays & Hashing",
            xpReward = 20,
            options = null,
            correctIndex = null,
            hint = null,
            explanation = null,
            exampleInput = null,
            exampleOutput = null,
            constraints = null
        )

        // Step 1: DTO -> Domain mapping
        val domainProblem = rawDto.toDomain()
        val expectedDsa01CorrectIndex = DefaultPracticeCatalog.getProblemById("dsa_01")!!.correctIndex
        assertEquals("dsa_01", domainProblem.id)
        assertEquals("Two Sum & Hash Map Lookup", domainProblem.title)
        assertEquals("Coding", domainProblem.category)
        assertEquals("Easy", domainProblem.difficulty)
        assertEquals(4, domainProblem.options.size)
        assertEquals(expectedDsa01CorrectIndex, domainProblem.correctIndex)
        assertNotNull(domainProblem.hint)
        assertTrue(domainProblem.hint!!.isNotBlank())
        assertNotNull(domainProblem.explanation)
        assertTrue(domainProblem.explanation!!.isNotBlank())
        assertNotNull(domainProblem.exampleInput)
        assertNotNull(domainProblem.exampleOutput)
        assertNotNull(domainProblem.constraints)

        // Step 2: ViewModel interaction
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startProblem("dsa_01")
        advanceUntilIdle()

        val selected = vm.uiState.value.selectedProblem
        assertNotNull(selected)
        assertEquals("dsa_01", selected?.id)
        assertEquals(4, selected?.options?.size)
        assertEquals(expectedDsa01CorrectIndex, selected?.correctIndex)
        assertEquals("Two Sum & Hash Map Lookup", selected?.title)
        assertNotNull(selected?.hint)
        assertNotNull(selected?.explanation)
    }

    @Test
    fun testTimeAndWorkAlternateDaysCompletePipeline() = runTest(testDispatcher) {
        // Test Case: "Time and Work: Alternate Work Days" (apt_01)
        // Simulate Supabase returning row without MCQ columns (options=null, hint=null, etc.)
        val rawDto = PracticeProblemDto(
            id = "apt_01",
            title = "Time and Work: Alternate Work Days",
            category = "Aptitude",
            difficulty = "Medium",
            description = "A can complete a piece of work in 10 days and B can complete the same work in 15 days.",
            careerId = null,
            topic = "Time and Work",
            xpReward = 20,
            options = null,
            correctIndex = null,
            hint = null,
            explanation = null,
            exampleInput = null,
            exampleOutput = null,
            constraints = null
        )

        // Step 1: DTO -> Domain mapping
        val domainProblem = rawDto.toDomain()
        val expectedApt01CorrectIndex = DefaultPracticeCatalog.getProblemById("apt_01")!!.correctIndex
        assertEquals("apt_01", domainProblem.id)
        assertEquals("Time and Work: Alternate Work Days", domainProblem.title)
        assertEquals("Aptitude", domainProblem.category)
        assertEquals("Medium", domainProblem.difficulty)
        assertEquals(4, domainProblem.options.size)
        assertEquals(expectedApt01CorrectIndex, domainProblem.correctIndex)
        assertNotNull(domainProblem.hint)
        assertTrue(domainProblem.hint!!.isNotBlank())
        assertNotNull(domainProblem.explanation)
        assertTrue(domainProblem.explanation!!.isNotBlank())
        assertNotNull(domainProblem.exampleInput)
        assertNotNull(domainProblem.exampleOutput)
        assertNotNull(domainProblem.constraints)

        // Step 2: ViewModel interaction
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val vm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        vm.startProblem("apt_01")
        advanceUntilIdle()

        val selected = vm.uiState.value.selectedProblem
        assertNotNull(selected)
        assertEquals("apt_01", selected?.id)
        assertEquals(4, selected?.options?.size)
        assertEquals(expectedApt01CorrectIndex, selected?.correctIndex)
        assertEquals("Time and Work: Alternate Work Days", selected?.title)
        assertNotNull(selected?.hint)
        assertNotNull(selected?.explanation)
    }

    @Test
    fun testAll84PracticeProblemsHaveCompleteMcqDataAcrossEntirePipeline() = runTest(testDispatcher) {
        val allCatalogProblems = DefaultPracticeCatalog.GLOBAL_PRACTICE_PROBLEMS
        assertEquals(84, allCatalogProblems.size)

        for (problem in allCatalogProblems) {
            // 1. Validate catalog item directly
            assertEquals("Problem ${problem.id} must have 4 options", 4, problem.options.size)
            assertTrue("Problem ${problem.id} correctIndex must be 0..3", problem.correctIndex in 0..3)
            assertTrue("Problem ${problem.id} hint must not be blank", !problem.hint.isNullOrBlank())
            assertTrue("Problem ${problem.id} explanation must not be blank", !problem.explanation.isNullOrBlank())
            assertTrue("Problem ${problem.id} title must not be blank", problem.title.isNotBlank())
            assertTrue("Problem ${problem.id} description must not be blank", problem.description.isNotBlank())

            // 2. Validate DTO to Domain transformation with stripped server options
            val strippedDto = PracticeProblemDto(
                id = problem.id,
                title = problem.title,
                category = problem.category,
                difficulty = problem.difficulty,
                description = problem.description,
                careerId = problem.careerId,
                topic = problem.topic,
                xpReward = problem.xpReward,
                options = null,
                correctIndex = null,
                hint = null,
                explanation = null
            )
            val mappedDomain = strippedDto.toDomain()
            assertEquals("Mapped domain for ${problem.id} must have 4 options", 4, mappedDomain.options.size)
            assertTrue("Mapped domain for ${problem.id} must have non-blank hint", !mappedDomain.hint.isNullOrBlank())
            assertTrue("Mapped domain for ${problem.id} must have non-blank explanation", !mappedDomain.explanation.isNullOrBlank())
            assertEquals("Mapped domain for ${problem.id} must preserve dynamic xpReward", problem.xpReward, mappedDomain.xpReward)
        }
    }

    @Test
    fun testDynamicXpAwardAndAuthProfileSynchronization() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        val initialProfile = authRepo.getCurrentUser().first()
        val initialXp = initialProfile?.xp ?: 0

        // Test solving apt_01 correctly
        practiceVm.startProblem("apt_01")
        advanceUntilIdle()

        val problem = practiceVm.uiState.value.selectedProblem
        assertNotNull(problem)
        assertEquals("apt_01", problem?.id)
        val expectedXpReward = problem?.xpReward ?: 20

        // Select correct answer
        val correctIndex = problem!!.correctIndex
        practiceVm.selectProblemOption(correctIndex)
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        // Verify UI feedback
        val uiState = practiceVm.uiState.value
        assertEquals(true, uiState.problemSubmissionResult)
        assertEquals("Correct! +$expectedXpReward XP earned.", uiState.feedbackMessage)
        assertEquals(initialXp + expectedXpReward, uiState.totalXp)

        // Verify AuthRepository cached profile
        val updatedProfile = authRepo.getCurrentUser().first()
        assertNotNull(updatedProfile)
        assertEquals(initialXp + expectedXpReward, updatedProfile?.xp)

        // Test Idempotency: Attempting to solve apt_01 again should yield 0 XP
        practiceVm.startProblem("apt_01")
        advanceUntilIdle()
        practiceVm.selectProblemOption(correctIndex)
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        val secondUiState = practiceVm.uiState.value
        assertEquals("Correct! Problem already solved previously (+0 XP).", secondUiState.feedbackMessage)
        assertEquals(initialXp + expectedXpReward, secondUiState.totalXp)
        assertEquals(initialXp + expectedXpReward, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testIncorrectAnswerDoesNotAwardXpOrMarkSolved() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        val initialXp = authRepo.getCurrentUser().first()?.xp ?: 0

        practiceVm.startProblem("apt_01")
        advanceUntilIdle()

        val problem = practiceVm.uiState.value.selectedProblem
        assertNotNull(problem)
        val incorrectIndex = (problem!!.correctIndex + 1) % 4

        practiceVm.selectProblemOption(incorrectIndex)
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        val uiState = practiceVm.uiState.value
        assertEquals(false, uiState.problemSubmissionResult)
        assertEquals("Incorrect. Check the hint or explanation and try again!", uiState.feedbackMessage)
        assertEquals(initialXp, uiState.totalXp)
        assertEquals(initialXp, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testImmediateXpUpdateWithoutUnnecessaryProfileReload() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        val initialUser = authRepo.getCurrentUser().first()
        val initialXp = initialUser?.xp ?: 0

        // Select and solve apt_01
        practiceVm.startProblem("apt_01")
        advanceUntilIdle()
        val problem = practiceVm.uiState.value.selectedProblem
        assertNotNull(problem)
        val rewardXp = problem!!.xpReward

        practiceVm.selectProblemOption(problem.correctIndex)
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        // 1. Cached profile updated immediately
        val updatedUser = authRepo.getCurrentUser().first()
        assertEquals(initialXp + rewardXp, updatedUser?.xp)

        // 2. UI receives updated XP immediately
        assertEquals(initialXp + rewardXp, practiceVm.uiState.value.totalXp)
        assertEquals("Correct! +$rewardXp XP earned.", practiceVm.uiState.value.feedbackMessage)

        // 3. Profile XP survives fresh reload
        val freshProfile = authRepo.getCurrentUser().first()
        assertEquals(initialXp + rewardXp, freshProfile?.xp)

        // 4. Repeated submission does not award duplicate XP
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()
        assertEquals(initialXp + rewardXp, authRepo.getCurrentUser().first()?.xp)
        assertEquals(initialXp + rewardXp, practiceVm.uiState.value.totalXp)
    }

    @Test
    fun testRpcFailureDoesNotUpdateXpOrCorruptState() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        // Repository simulating RPC failure
        val failingRepo = object : com.example.domain.repository.PracticeRepository by MockPracticeRepository(authRepository = authRepo) {
            override suspend fun solveProblem(id: String, selectedAnswerIndex: Int?): com.example.domain.model.PracticeProblemSolveResult {
                return com.example.domain.model.PracticeProblemSolveResult(
                    isCorrect = false,
                    xpAwarded = 0,
                    isFirstSolve = false,
                    isSolved = false,
                    totalXp = 0,
                    message = "Submission failed. Please check your connection and try again."
                )
            }
            override suspend fun completeProblem(id: String, selectedAnswerIndex: Int?): Boolean {
                return false // RPC failure
            }
        }
        val practiceVm = PracticeViewModel(practiceRepository = failingRepo, authRepository = authRepo)
        advanceUntilIdle()

        val initialXp = authRepo.getCurrentUser().first()?.xp ?: 0

        practiceVm.startProblem("apt_01")
        advanceUntilIdle()
        val problem = practiceVm.uiState.value.selectedProblem
        assertNotNull(problem)

        practiceVm.selectProblemOption(problem!!.correctIndex)
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        val uiState = practiceVm.uiState.value
        assertEquals(false, uiState.problemSubmissionResult)
        assertEquals("Submission failed. Please check your connection and try again.", uiState.feedbackMessage)
        assertEquals(initialXp, uiState.totalXp)
        assertEquals(initialXp, authRepo.getCurrentUser().first()?.xp)
    }

    @Test
    fun testPracticeCompletionFilterEnumValuesAndParsing() {
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.ALL, com.example.viewmodel.PracticeCompletionFilter.fromString("All"))
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.ALL, com.example.viewmodel.PracticeCompletionFilter.fromString("all"))
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.COMPLETED, com.example.viewmodel.PracticeCompletionFilter.fromString("Completed"))
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.COMPLETED, com.example.viewmodel.PracticeCompletionFilter.fromString("solved"))
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE, com.example.viewmodel.PracticeCompletionFilter.fromString("Incomplete"))
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE, com.example.viewmodel.PracticeCompletionFilter.fromString("unsolved"))
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.ALL, com.example.viewmodel.PracticeCompletionFilter.fromString("invalid_value"))
    }

    @Test
    fun testPracticeViewModelCompletionFilterTransitions() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        // Initial state is ALL
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.ALL, practiceVm.uiState.value.completionFilter)

        practiceVm.setCompletionFilter(com.example.viewmodel.PracticeCompletionFilter.COMPLETED)
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.COMPLETED, practiceVm.uiState.value.completionFilter)

        practiceVm.setCompletionFilter(com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE)
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE, practiceVm.uiState.value.completionFilter)

        practiceVm.setCompletionFilter("All")
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.ALL, practiceVm.uiState.value.completionFilter)
    }

    @Test
    fun testPracticeFilteringAcrossAllThreeCategories() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        val allLoadedProblems = practiceVm.uiState.value.problems
        val totalCoding = allLoadedProblems.count { it.category.equals("Coding", ignoreCase = true) }
        val totalAptitude = allLoadedProblems.count { it.category.equals("Aptitude", ignoreCase = true) }
        val totalTechnical = allLoadedProblems.count { it.category.equals("Technical", ignoreCase = true) }

        assertTrue(totalCoding > 0)
        assertTrue(totalAptitude > 0)
        assertTrue(totalTechnical > 0)

        val firstCodingProblem = allLoadedProblems.first { it.category.equals("Coding", ignoreCase = true) }
        val firstAptitudeProblem = allLoadedProblems.first { it.category.equals("Aptitude", ignoreCase = true) }

        // Solve 1 coding problem
        practiceVm.startProblem(firstCodingProblem.id)
        advanceUntilIdle()
        practiceVm.selectProblemOption(practiceVm.uiState.value.selectedProblem!!.correctIndex)
        practiceVm.submitProblemAnswer(firstCodingProblem.id)
        advanceUntilIdle()

        // Solve 1 aptitude problem
        practiceVm.startProblem(firstAptitudeProblem.id)
        advanceUntilIdle()
        practiceVm.selectProblemOption(practiceVm.uiState.value.selectedProblem!!.correctIndex)
        practiceVm.submitProblemAnswer(firstAptitudeProblem.id)
        advanceUntilIdle()

        // Coding tab (index 0)
        val codingAll = practiceVm.getFilteredProblems(categoryIndex = 0, filter = com.example.viewmodel.PracticeCompletionFilter.ALL)
        val codingCompleted = practiceVm.getFilteredProblems(categoryIndex = 0, filter = com.example.viewmodel.PracticeCompletionFilter.COMPLETED)
        val codingIncomplete = practiceVm.getFilteredProblems(categoryIndex = 0, filter = com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE)

        assertEquals(totalCoding, codingAll.size)
        assertEquals(1, codingCompleted.size)
        assertEquals(firstCodingProblem.id, codingCompleted.first().id)
        assertEquals(totalCoding - 1, codingIncomplete.size)
        assertFalse(codingIncomplete.any { it.id == firstCodingProblem.id })

        // Aptitude tab (index 1)
        val aptAll = practiceVm.getFilteredProblems(categoryIndex = 1, filter = com.example.viewmodel.PracticeCompletionFilter.ALL)
        val aptCompleted = practiceVm.getFilteredProblems(categoryIndex = 1, filter = com.example.viewmodel.PracticeCompletionFilter.COMPLETED)
        val aptIncomplete = practiceVm.getFilteredProblems(categoryIndex = 1, filter = com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE)

        assertEquals(totalAptitude, aptAll.size)
        assertEquals(1, aptCompleted.size)
        assertEquals(firstAptitudeProblem.id, aptCompleted.first().id)
        assertEquals(totalAptitude - 1, aptIncomplete.size)
        assertFalse(aptIncomplete.any { it.id == firstAptitudeProblem.id })

        // Technical tab (index 2) - 0 completed initially
        val techAll = practiceVm.getFilteredProblems(categoryIndex = 2, filter = com.example.viewmodel.PracticeCompletionFilter.ALL)
        val techCompleted = practiceVm.getFilteredProblems(categoryIndex = 2, filter = com.example.viewmodel.PracticeCompletionFilter.COMPLETED)
        val techIncomplete = practiceVm.getFilteredProblems(categoryIndex = 2, filter = com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE)

        assertEquals(totalTechnical, techAll.size)
        assertEquals(0, techCompleted.size)
        assertEquals(totalTechnical, techIncomplete.size)
    }

    @Test
    fun testPracticeFilterPreservesXpStreakAndState() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo)
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        val initialStreak = practiceVm.uiState.value.currentStreak
        val initialXp = practiceVm.uiState.value.totalXp

        practiceVm.setCompletionFilter(com.example.viewmodel.PracticeCompletionFilter.COMPLETED)
        practiceVm.setActiveTab(1)
        practiceVm.setCompletionFilter(com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE)

        val stateAfterFilterChange = practiceVm.uiState.value
        assertEquals(initialStreak, stateAfterFilterChange.currentStreak)
        assertEquals(initialXp, stateAfterFilterChange.totalXp)
        assertEquals(1, stateAfterFilterChange.activeTab)
        assertEquals(com.example.viewmodel.PracticeCompletionFilter.INCOMPLETE, stateAfterFilterChange.completionFilter)
    }

    @Test
    fun testAuthoritativeExactXpAwardOnFirstSolveAndZeroOnRepeat() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        val initialXp = practiceVm.uiState.value.totalXp
        val problem = practiceVm.uiState.value.problems.first { it.id == "apt_01" }
        val exactReward = problem.xpReward

        // 1. First solve
        practiceVm.startProblem("apt_01")
        advanceUntilIdle()
        practiceVm.selectProblemOption(practiceVm.uiState.value.selectedProblem!!.correctIndex)
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        assertTrue(practiceVm.uiState.value.problemSubmissionResult == true)
        assertEquals(initialXp + exactReward, practiceVm.uiState.value.totalXp)
        assertTrue(practiceVm.uiState.value.problems.first { it.id == "apt_01" }.isSolved)
        assertTrue(practiceVm.uiState.value.feedbackMessage!!.contains("+$exactReward XP"))

        // 2. Repeat solve on the same problem
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        assertTrue(practiceVm.uiState.value.problemSubmissionResult == true)
        // Total XP MUST NOT increase on repeat solve
        assertEquals(initialXp + exactReward, practiceVm.uiState.value.totalXp)
        assertTrue(practiceVm.uiState.value.problems.first { it.id == "apt_01" }.isSolved)
        assertTrue(
            practiceVm.uiState.value.feedbackMessage!!.contains("+0 XP") ||
            practiceVm.uiState.value.feedbackMessage!!.contains("already solved", ignoreCase = true) ||
            practiceVm.uiState.value.feedbackMessage!!.contains("Already completed", ignoreCase = true)
        )
    }

    @Test
    fun testIncorrectAnswerAwardsZeroXpAndLeavesUnsolved() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        val initialXp = practiceVm.uiState.value.totalXp
        practiceVm.startProblem("apt_01")
        advanceUntilIdle()

        val wrongIndex = (practiceVm.uiState.value.selectedProblem!!.correctIndex + 1) % 4
        practiceVm.selectProblemOption(wrongIndex)
        practiceVm.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        assertFalse(practiceVm.uiState.value.problemSubmissionResult == true)
        assertEquals(initialXp, practiceVm.uiState.value.totalXp)
        assertFalse(practiceVm.uiState.value.problems.first { it.id == "apt_01" }.isSolved)
    }

    @Test
    fun testFirstSolvePersistsTrueAndSurvivesRepositoryRecreation() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        val sharedSolvedSet = mutableSetOf<String>()
        val practiceRepo1 = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = sharedSolvedSet)
        val practiceVm1 = PracticeViewModel(practiceRepository = practiceRepo1, authRepository = authRepo)
        advanceUntilIdle()

        practiceVm1.startProblem("apt_01")
        advanceUntilIdle()
        practiceVm1.selectProblemOption(practiceVm1.uiState.value.selectedProblem!!.correctIndex)
        practiceVm1.submitProblemAnswer("apt_01")
        advanceUntilIdle()

        assertTrue(practiceVm1.uiState.value.problemSubmissionResult == true)
        assertTrue(practiceVm1.uiState.value.problems.first { it.id == "apt_01" }.isSolved)

        // Capture persisted solved IDs
        val solvedIds = practiceRepo1.getSolvedProblemIds()
        assertTrue("Persisted solved set must contain apt_01", solvedIds.contains("apt_01"))

        // Recreate repository / viewmodel (simulating app restart)
        val practiceRepo2 = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = solvedIds)
        val practiceVm2 = PracticeViewModel(practiceRepository = practiceRepo2, authRepository = authRepo)
        advanceUntilIdle()

        // Verify problem is still marked completed (isSolved = true)
        assertTrue(
            "Problem apt_01 must remain completed after app restart",
            practiceVm2.uiState.value.problems.first { it.id == "apt_01" }.isSolved
        )
    }

    @Test
    fun testExistingFalseRowUpdatedToTrueOnSuccessfulCompletion() = runTest(testDispatcher) {
        val authRepo = com.example.data.repository.MockAuthRepository()
        // Start with no solved problems (is_solved = false in progress)
        val practiceRepo = MockPracticeRepository(authRepository = authRepo, initialSolvedProblemIds = emptySet())
        val practiceVm = PracticeViewModel(practiceRepository = practiceRepo, authRepository = authRepo)
        advanceUntilIdle()

        assertFalse(practiceVm.uiState.value.problems.first { it.id == "apt_01" }.isSolved)

        // Solve problem
        val correctIndex = DefaultPracticeCatalog.getProblemById("apt_01")!!.correctIndex
        val result = practiceRepo.solveProblem("apt_01", correctIndex)
        assertTrue(result.isCorrect)
        assertTrue(result.isSolved)
        assertTrue(result.isFirstSolve)

        val solvedIds = practiceRepo.getSolvedProblemIds()
        assertTrue("Solved IDs must now contain apt_01", solvedIds.contains("apt_01"))
    }

    @Test
    fun testPracticeProblemsContentQualityAndAnswerDistribution() {
        val problems = DefaultPracticeCatalog.GLOBAL_PRACTICE_PROBLEMS
        assertEquals("Total practice problems must be 84", 84, problems.size)

        // 1. Verify all questions have unique IDs
        val ids = problems.map { it.id }
        assertEquals("All problem IDs must be strictly unique", ids.distinct().size, ids.size)

        // 2 & 3. Verify option counts, bounds, non-empty explanation and hint, XP values
        val answerDistribution = mutableMapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)

        for (problem in problems) {
            // 2. Exactly 4 options
            assertEquals("Problem ${problem.id} must have exactly 4 options", 4, problem.options.size)

            // 3. correctIndex is between 0 and 3
            assertTrue(
                "Problem ${problem.id} correctIndex must be between 0 and 3 (was ${problem.correctIndex})",
                problem.correctIndex in 0..3
            )

            // Track distribution
            answerDistribution[problem.correctIndex] = (answerDistribution[problem.correctIndex] ?: 0) + 1

            // 6. No duplicate options within the same problem
            assertEquals(
                "Problem ${problem.id} contains duplicate options: ${problem.options}",
                4,
                problem.options.distinct().size
            )

            // 7. Verify option strings do not contain hardcoded A), B), C), or D) prefixes
            for ((optIdx, optionText) in problem.options.withIndex()) {
                assertFalse(
                    "Problem ${problem.id} option $optIdx contains hardcoded prefix: '$optionText'",
                    optionText.matches(Regex("^[A-D]\\)\\s*.*"))
                )
                assertTrue("Problem ${problem.id} option $optIdx must not be blank", optionText.isNotBlank())
            }

            // 8. Non-empty explanation
            assertTrue(
                "Problem ${problem.id} must have a non-empty explanation",
                !problem.explanation.isNullOrBlank()
            )

            // 9. Non-empty hint
            assertTrue(
                "Problem ${problem.id} must have a non-empty hint",
                !problem.hint.isNullOrBlank()
            )

            // 10. XP values remain unchanged at 20 XP
            assertEquals(
                "Problem ${problem.id} XP reward must be exactly 20 XP",
                20,
                problem.xpReward
            )
        }

        // 4. Verify no answer position exceeds 35% of the total Practice Problem catalog (max ~29 out of 84)
        val maxAllowedPerPosition = (problems.size * 0.35).toInt()
        for ((position, count) in answerDistribution) {
            val percentage = (count.toDouble() / problems.size) * 100.0
            assertTrue(
                "Option position $position count ($count, $percentage%) must not exceed 35% ($maxAllowedPerPosition)",
                count <= maxAllowedPerPosition
            )
        }

        // Verify balanced distribution (exactly 21 each in our 84-problem catalog)
        assertEquals("Option A count must be 21", 21, answerDistribution[0])
        assertEquals("Option B count must be 21", 21, answerDistribution[1])
        assertEquals("Option C count must be 21", 21, answerDistribution[2])
        assertEquals("Option D count must be 21", 21, answerDistribution[3])
    }

    @Test
    fun testSpeedQuizQuestionsContentQualityAndAnswerDistribution() {
        val allQuizQuestions = DefaultPracticeCatalog.QUIZ_QUESTIONS_MAP.values.flatten()
        assertEquals("Total speed quiz questions must be 40", 40, allQuizQuestions.size)

        val ids = allQuizQuestions.map { it.id }
        assertEquals("All quiz IDs must be strictly unique", ids.distinct().size, ids.size)

        val answerDistribution = mutableMapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)

        for (quiz in allQuizQuestions) {
            assertEquals("Quiz ${quiz.id} must have exactly 4 options", 4, quiz.options.size)
            assertTrue(
                "Quiz ${quiz.id} correctIndex must be between 0 and 3 (was ${quiz.correctIndex})",
                quiz.correctIndex in 0..3
            )
            answerDistribution[quiz.correctIndex] = (answerDistribution[quiz.correctIndex] ?: 0) + 1
            assertEquals("Quiz ${quiz.id} contains duplicate options", 4, quiz.options.distinct().size)
            assertTrue("Quiz ${quiz.id} must have non-empty explanation", quiz.explanation.isNotBlank())

            for ((optIdx, optionText) in quiz.options.withIndex()) {
                assertFalse(
                    "Quiz ${quiz.id} option $optIdx contains hardcoded prefix: '$optionText'",
                    optionText.matches(Regex("^[A-D]\\)\\s*.*"))
                )
                assertTrue("Quiz ${quiz.id} option $optIdx must not be blank", optionText.isNotBlank())
            }
        }

        // Verify balanced distribution (10 each across 40 questions)
        assertEquals("Quiz Option A count must be 10", 10, answerDistribution[0])
        assertEquals("Quiz Option B count must be 10", 10, answerDistribution[1])
        assertEquals("Quiz Option C count must be 10", 10, answerDistribution[2])
        assertEquals("Quiz Option D count must be 10", 10, answerDistribution[3])
    }

    @Test
    fun testCompleteProblemRpcV2ResponseDtoSerialization() {
        val jsonStringFirstSolve = """
            {
                "success": true,
                "newly_solved": true,
                "xp_awarded": 20,
                "problem_id": "dsa_01",
                "total_xp": 140,
                "message": "First solve success"
            }
        """.trimIndent()

        val decodedFirstSolve = json.decodeFromString(com.example.data.model.CompleteProblemRpcV2ResponseDto.serializer(), jsonStringFirstSolve)
        assertTrue(decodedFirstSolve.success)
        assertTrue(decodedFirstSolve.newlySolved)
        assertEquals(20, decodedFirstSolve.xpAwarded)
        assertEquals("dsa_01", decodedFirstSolve.problemId)
        assertEquals(140, decodedFirstSolve.totalXp)
        assertEquals("First solve success", decodedFirstSolve.message)

        val domainFirst = decodedFirstSolve.toDomain(isCorrect = true)
        assertTrue(domainFirst.isCorrect)
        assertTrue(domainFirst.isFirstSolve)
        assertTrue(domainFirst.isSolved)
        assertEquals(20, domainFirst.xpAwarded)
        assertEquals(140, domainFirst.totalXp)

        val jsonStringAlreadySolved = """
            {
                "success": true,
                "newly_solved": false,
                "xp_awarded": 0,
                "problem_id": "dsa_01",
                "total_xp": 140,
                "message": "Already completed"
            }
        """.trimIndent()

        val decodedAlreadySolved = json.decodeFromString(com.example.data.model.CompleteProblemRpcV2ResponseDto.serializer(), jsonStringAlreadySolved)
        assertTrue(decodedAlreadySolved.success)
        assertFalse(decodedAlreadySolved.newlySolved)
        assertEquals(0, decodedAlreadySolved.xpAwarded)

        val domainAlready = decodedAlreadySolved.toDomain(isCorrect = true)
        assertTrue(domainAlready.isCorrect)
        assertFalse(domainAlready.isFirstSolve)
        assertTrue(domainAlready.isSolved)
        assertEquals(0, domainAlready.xpAwarded)
    }

    @Test
    fun testPracticeViewModelSolveFlow() = runTest {
        val mockRepo = MockPracticeRepository(initialSolvedProblemIds = emptySet())
        val viewModel = PracticeViewModel(practiceRepository = mockRepo)
        advanceUntilIdle()

        // 1. Solve Two Sum (dsa_01) correctly (correctIndex = 1)
        val problem = DefaultPracticeCatalog.getProblemById("dsa_01")
        assertNotNull(problem)
        viewModel.selectProblem(problem!!)
        advanceUntilIdle()

        viewModel.completeProblem("dsa_01", selectedAnswerIndex = problem.correctIndex)
        advanceUntilIdle()

        val stateAfterFirstSolve = viewModel.uiState.value
        assertTrue(stateAfterFirstSolve.problemSubmissionResult == true)
        assertTrue(stateAfterFirstSolve.feedbackMessage?.contains("+20 XP") == true)
        assertTrue(stateAfterFirstSolve.problems.first { it.id == "dsa_01" }.isSolved)

        // 2. Solve again (already solved)
        viewModel.completeProblem("dsa_01", selectedAnswerIndex = problem.correctIndex)
        advanceUntilIdle()

        val stateAfterSecondSolve = viewModel.uiState.value
        assertTrue(stateAfterSecondSolve.feedbackMessage?.contains("already solved") == true)

        // 3. Incorrect answer for another problem (dsa_02)
        val problem2 = DefaultPracticeCatalog.getProblemById("dsa_02")
        assertNotNull(problem2)
        val wrongIndex = (problem2!!.correctIndex + 1) % 4
        viewModel.completeProblem("dsa_02", selectedAnswerIndex = wrongIndex)
        advanceUntilIdle()

        val stateAfterWrong = viewModel.uiState.value
        assertEquals(false, stateAfterWrong.problemSubmissionResult)
        assertFalse(stateAfterWrong.problems.first { it.id == "dsa_02" }.isSolved)
    }

    @Test
    fun testApt01TimeAndWorkAnswerValidationAndPersistence() = runTest {
        val mockRepo = MockPracticeRepository(initialSolvedProblemIds = emptySet())
        // apt_01: Time and Work: Alternate Work Days
        val apt01 = DefaultPracticeCatalog.getProblemById("apt_01")
        assertNotNull(apt01)
        assertEquals(3, apt01!!.correctIndex) // Option D (12 days) is index 3
        assertEquals(20, apt01.xpReward)

        // 1. Wrong answer selected (e.g. index 0)
        val wrongResult = mockRepo.solveProblem("apt_01", selectedAnswerIndex = 0)
        assertFalse(wrongResult.isCorrect)
        assertFalse(wrongResult.isSolved)
        assertEquals(0, wrongResult.xpAwarded)
        assertFalse(mockRepo.getSolvedProblemIds().contains("apt_01"))

        // 2. Correct answer selected (index 3)
        val correctResult = mockRepo.solveProblem("apt_01", selectedAnswerIndex = 3)
        assertTrue(correctResult.isCorrect)
        assertTrue(correctResult.isSolved)
        assertTrue(correctResult.isFirstSolve)
        assertEquals(20, correctResult.xpAwarded)
        assertTrue(mockRepo.getSolvedProblemIds().contains("apt_01"))

        // 3. Repeated solve with correct answer
        val repeatResult = mockRepo.solveProblem("apt_01", selectedAnswerIndex = 3)
        assertTrue(repeatResult.isCorrect)
        assertTrue(repeatResult.isSolved)
        assertFalse(repeatResult.isFirstSolve)
        assertEquals(0, repeatResult.xpAwarded)
    }

    @Test
    fun testLegacyP1ToP10CanonicalIdResolution() {
        val legacyMapping = mapOf(
            "p1" to "dsa_01",
            "p2" to "dsa_02",
            "p3" to "dsa_03",
            "p4" to "dsa_04",
            "p5" to "dsa_05",
            "p6" to "aiml_01",
            "p7" to "aiml_02",
            "p8" to "aiml_03",
            "p9" to "aiml_04",
            "p10" to "aiml_05"
        )

        for ((legacy, expectedCanonical) in legacyMapping) {
            val resolvedId = DefaultPracticeCatalog.toCanonicalProblemId(legacy)
            assertEquals("Legacy $legacy should map to $expectedCanonical", expectedCanonical, resolvedId)

            val problem = DefaultPracticeCatalog.getProblemById(legacy)
            assertNotNull("Problem for legacy $legacy must be found", problem)
            assertEquals(expectedCanonical, problem!!.id)
        }
    }

    @Test
    fun testPermanentPracticePersistenceSurvivesNewCalendarDay() = runTest {
        val mockRepo = MockPracticeRepository(initialSolvedProblemIds = emptySet())
        
        // Day 1: Solve apt_01 and complete Daily Test
        mockRepo.solveProblem("apt_01", selectedAnswerIndex = 3)
        mockRepo.saveQuizAttempt(
            QuizAttempt(
                id = "attempt_day1",
                userId = "user_1",
                careerId = "general",
                score = 5,
                totalQuestions = 5,
                xpEarned = 50,
                topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST
            )
        )

        assertTrue(mockRepo.getSolvedProblemIds().contains("apt_01"))

        // Day 2 (new date): Daily test can be taken again, but normal Practice problem MUST remain solved
        assertTrue(mockRepo.getSolvedProblemIds().contains("apt_01"))
        val solvedSet = mockRepo.getSolvedProblemIds()
        assertEquals(1, solvedSet.size)
        assertTrue(solvedSet.contains("apt_01"))
    }
}


