package com.example

import com.example.data.repository.LocalKnowledgeBaseRepository
import com.example.data.repository.LocalLearnerModelRepository
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.PdfContentUnit
import com.example.domain.model.adaptive.Assessment
import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentQuestion
import com.example.domain.model.adaptive.AssessmentQuestionType
import com.example.domain.model.adaptive.AssessmentResult
import com.example.domain.model.adaptive.ConfidenceLevel
import com.example.domain.model.adaptive.MasteryCategory
import com.example.domain.model.adaptive.QuestionResult
import com.example.domain.model.adaptive.TopicPerformance
import com.example.domain.model.adaptive.TutorLearningSignal
import com.example.domain.model.adaptive.TutorSignalType
import com.example.domain.repository.AdaptiveAssessmentRepository
import com.example.domain.repository.LearningMaterialRepository
import com.example.viewmodel.LearningCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class LearnerModelUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 1. INITIAL NO-HISTORY STATE & NO FAKE MASTERY
    // =========================================================================

    @Test
    fun testInitialState_hasNoHistoryAndZeroMastery() = runTest {
        val learnerRepo = LocalLearnerModelRepository()
        val all = learnerRepo.getAllMastery("mat-1")
        assertTrue(all.isEmpty())

        val recommended = learnerRepo.getRecommendedTopic("mat-1")
        assertNull(recommended)
    }

    @Test
    fun testViewModel_newStudentHasNoFakeMastery() = runTest {
        val learnerRepo = LocalLearnerModelRepository()
        val viewModel = LearningCompanionViewModel(
            authRepository = null,
            learnerModelRepository = learnerRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.overallMastery)
        assertFalse(state.hasMasteryHistory)
        assertTrue(state.topicMasteries.isEmpty())
        assertNull(state.recommendedTopicMastery)
    }

    // =========================================================================
    // 2. WEIGHTED EVIDENCE MASTERY MODEL ALGORITHM & INITIAL = 50%
    // =========================================================================

    @Test
    fun testFirstAssessment_usesBaselineFiftyPercentAndWeightedFormula() = runTest {
        val learnerRepo = LocalLearnerModelRepository()

        // 80% accuracy on first assessment (4/5 correct)
        // oldMastery = 50 (baseline)
        // newMastery = (50 * 0.6) + (80 * 0.4) = 30 + 32 = 62%
        val assessmentResult = AssessmentResult(
            assessmentId = "a-1",
            totalQuestions = 5,
            correctAnswers = 4,
            scorePercentage = 80,
            topicPerformance = listOf(TopicPerformance("Inheritance", 5, 4, 80)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q1", "Q1", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q2", "Q2", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q3", "Q3", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q4", "Q4", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q5", "Q5", "B", "A", false, "E", "Inheritance", "C", "D", 1, "c1")
            )
        )

        val updated = learnerRepo.updateFromAssessment("mat-java", assessmentResult)
        assertEquals(1, updated.size)
        val inheritanceMastery = updated.first()

        assertEquals("Inheritance", inheritanceMastery.topic)
        assertEquals(62, inheritanceMastery.masteryScore) // Exactly 62%
        assertEquals(5, inheritanceMastery.questionsAttempted)
        assertEquals(4, inheritanceMastery.questionsCorrect)
        assertEquals(80, inheritanceMastery.recentAccuracy)
        assertEquals(MasteryCategory.DEVELOPING, inheritanceMastery.category) // 60–79 is Developing
    }

    @Test
    fun testSubsequentAssessment_updatesFromPreviousMastery() = runTest {
        val learnerRepo = LocalLearnerModelRepository()

        // Assessment 1: 80% accuracy -> mastery becomes 62%
        val res1 = AssessmentResult(
            assessmentId = "a-1",
            totalQuestions = 5,
            correctAnswers = 4,
            scorePercentage = 80,
            topicPerformance = listOf(TopicPerformance("Inheritance", 5, 4, 80)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q1", "Q1", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q2", "Q2", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q3", "Q3", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q4", "Q4", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q5", "Q5", "B", "A", false, "E", "Inheritance", "C", "D", 1, "c1")
            )
        )
        learnerRepo.updateFromAssessment("mat-java", res1)

        // Assessment 2: 100% accuracy (2/2 correct)
        // oldMastery = 62
        // newMastery = (62 * 0.6) + (100 * 0.4) = 37.2 + 40 = 77.2 -> 77%
        val res2 = AssessmentResult(
            assessmentId = "a-2",
            totalQuestions = 2,
            correctAnswers = 2,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("Inheritance", 2, 2, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q6", "Q6", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1"),
                QuestionResult("q7", "Q7", "A", "A", true, "E", "Inheritance", "C", "D", 1, "c1")
            )
        )
        val updated2 = learnerRepo.updateFromAssessment("mat-java", res2)
        val updatedMastery = updated2.first { it.topic == "Inheritance" }

        assertEquals(77, updatedMastery.masteryScore)
        assertEquals(7, updatedMastery.questionsAttempted)
        assertEquals(6, updatedMastery.questionsCorrect)
        assertEquals(100, updatedMastery.recentAccuracy)
    }

    // =========================================================================
    // 3. MASTERY CLAMPING BETWEEN 0 AND 100
    // =========================================================================

    @Test
    fun testMasteryClamping_neverExceeds100OrDropsBelow0() = runTest {
        val learnerRepo = LocalLearnerModelRepository()

        // Consecutive 100% scores
        val perfectResult = AssessmentResult(
            assessmentId = "a-perf",
            totalQuestions = 1,
            correctAnswers = 1,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("PerfectTopic", 1, 1, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q1", "Q1", "A", "A", true, "E", "PerfectTopic", "C", "D", 1, "c1")
            )
        )

        // Run 10 times to approach 100
        for (i in 1..10) {
            learnerRepo.updateFromAssessment("mat-java", perfectResult)
        }

        val highMastery = learnerRepo.getMastery("mat-java", "PerfectTopic")?.masteryScore ?: 0
        assertTrue(highMastery in 0..100)
        assertEquals(100, highMastery)

        // Consecutive 0% scores
        val zeroResult = AssessmentResult(
            assessmentId = "a-zero",
            totalQuestions = 1,
            correctAnswers = 0,
            scorePercentage = 0,
            topicPerformance = listOf(TopicPerformance("ZeroTopic", 1, 0, 0)),
            weakTopics = listOf("ZeroTopic"),
            questionResults = listOf(
                QuestionResult("q2", "Q2", "B", "A", false, "E", "ZeroTopic", "C", "D", 1, "c1")
            )
        )

        for (i in 1..10) {
            learnerRepo.updateFromAssessment("mat-java", zeroResult)
        }

        val lowMastery = learnerRepo.getMastery("mat-java", "ZeroTopic")?.masteryScore ?: -1
        assertTrue(lowMastery in 0..100)
        assertEquals(0, lowMastery)
    }

    // =========================================================================
    // 4. TOPIC-LEVEL UPDATES & UNASSESSED TOPICS PRESERVATION
    // =========================================================================

    @Test
    fun testTopicSpecificUpdates_unassessedTopicsRemainUnchanged() = runTest {
        val learnerRepo = LocalLearnerModelRepository()

        // Pre-populate Topic A
        val resA = AssessmentResult(
            assessmentId = "a-1",
            totalQuestions = 1,
            correctAnswers = 1,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("TopicA", 1, 1, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q1", "Q1", "A", "A", true, "E", "TopicA", "C", "D", 1, "c1")
            )
        )
        learnerRepo.updateFromAssessment("mat-1", resA)
        val topicAMasteryBefore = learnerRepo.getMastery("mat-1", "TopicA")?.masteryScore

        // Now run an assessment containing ONLY Topic B
        val resB = AssessmentResult(
            assessmentId = "a-2",
            totalQuestions = 1,
            correctAnswers = 0,
            scorePercentage = 0,
            topicPerformance = listOf(TopicPerformance("TopicB", 1, 0, 0)),
            weakTopics = listOf("TopicB"),
            questionResults = listOf(
                QuestionResult("q2", "Q2", "W", "C", false, "E", "TopicB", "C", "D", 1, "c1")
            )
        )
        learnerRepo.updateFromAssessment("mat-1", resB)

        val topicAMasteryAfter = learnerRepo.getMastery("mat-1", "TopicA")?.masteryScore
        val topicBMastery = learnerRepo.getMastery("mat-1", "TopicB")?.masteryScore

        // Topic A must not change when Topic B was assessed
        assertEquals(topicAMasteryBefore, topicAMasteryAfter)
        assertNotNull(topicBMastery)
    }

    // =========================================================================
    // 5. WEAK & STRONG TOPIC IDENTIFICATION AND RECOMMENDED FOCUS
    // =========================================================================

    @Test
    fun testWeakAndStrongTopicsCategorization() = runTest {
        val learnerRepo = LocalLearnerModelRepository()

        // Topic 1: 0% accuracy -> (50 * 0.6) + (0 * 0.4) = 30% -> WEAK (category NEEDS_PRACTICE)
        val resWeak = AssessmentResult(
            assessmentId = "a-w",
            totalQuestions = 2,
            correctAnswers = 0,
            scorePercentage = 0,
            topicPerformance = listOf(TopicPerformance("WeakTopic", 2, 0, 0)),
            weakTopics = listOf("WeakTopic"),
            questionResults = listOf(
                QuestionResult("q1", "Q1", "X", "A", false, "E", "WeakTopic", "C", "D", 1, "c1"),
                QuestionResult("q2", "Q2", "X", "A", false, "E", "WeakTopic", "C", "D", 1, "c1")
            )
        )
        learnerRepo.updateFromAssessment("mat-1", resWeak)

        // Topic 2: 100% accuracy twice -> 70% then 82% -> STRONG
        val resStrong = AssessmentResult(
            assessmentId = "a-s",
            totalQuestions = 2,
            correctAnswers = 2,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("StrongTopic", 2, 2, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q3", "Q3", "A", "A", true, "E", "StrongTopic", "C", "D", 1, "c1"),
                QuestionResult("q4", "Q4", "A", "A", true, "E", "StrongTopic", "C", "D", 1, "c1")
            )
        )
        learnerRepo.updateFromAssessment("mat-1", resStrong)
        learnerRepo.updateFromAssessment("mat-1", resStrong) // (70 * 0.6) + (100 * 0.4) = 42 + 40 = 82%

        val weakList = learnerRepo.getWeakTopics("mat-1")
        val strongList = learnerRepo.getStrongTopics("mat-1")

        assertEquals(1, weakList.size)
        assertEquals("WeakTopic", weakList[0].topic)
        assertEquals(MasteryCategory.NEEDS_PRACTICE, weakList[0].category)

        assertEquals(1, strongList.size)
        assertEquals("StrongTopic", strongList[0].topic)
        assertEquals(MasteryCategory.STRONG, strongList[0].category)

        // Recommendation: If any topic < 60, recommend the lowest
        val recommended = learnerRepo.getRecommendedTopic("mat-1")
        assertNotNull(recommended)
        assertEquals("WeakTopic", recommended?.topic)
    }

    @Test
    fun testRecommendedTopic_whenAllAbove60_recommendsDevelopingTopic() = runTest {
        val learnerRepo = LocalLearnerModelRepository()

        // Topic 1: 65% (Developing)
        // Topic 2: 85% (Strong)
        val res = AssessmentResult(
            assessmentId = "a-dev",
            totalQuestions = 2,
            correctAnswers = 2,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("DevTopic", 1, 1, 100), TopicPerformance("StrongTopic", 1, 1, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q1", "Q1", "A", "A", true, "E", "DevTopic", "C", "D", 1, "c1"),
                QuestionResult("q2", "Q2", "A", "A", true, "E", "StrongTopic", "C", "D", 1, "c1")
            )
        )
        learnerRepo.updateFromAssessment("mat-1", res) // DevTopic=70% (Developing), StrongTopic=70%

        val recommended = learnerRepo.getRecommendedTopic("mat-1")
        assertNotNull(recommended)
        // Since both < 80, recommended is one of the below 80 topics
        assertTrue(recommended?.masteryScore ?: 100 < 80)
    }

    // =========================================================================
    // 6. TUTOR CONVERSATION LEARNING SIGNALS & CONFIDENCE ADJUSTMENTS
    // =========================================================================

    @Test
    fun testTutorSignals_repeatedClarificationLowersConfidenceToLow() = runTest {
        val learnerRepo = LocalLearnerModelRepository()

        // Create initial topic mastery
        val res = AssessmentResult(
            assessmentId = "a-1",
            totalQuestions = 1,
            correctAnswers = 1,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("Recursion", 1, 1, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q1", "Q1", "A", "A", true, "E", "Recursion", "C", "D", 1, "c1")
            )
        )
        learnerRepo.updateFromAssessment("mat-1", res)
        assertEquals(ConfidenceLevel.HIGH, learnerRepo.getMastery("mat-1", "Recursion")?.confidenceLevel)

        // First clarification signal
        learnerRepo.recordTutorSignal(
            "mat-1",
            TutorLearningSignal("Recursion", System.currentTimeMillis(), TutorSignalType.ASKED_FOR_CLARIFICATION)
        )
        // Baseline mastery is NOT deflated
        assertEquals(70, learnerRepo.getMastery("mat-1", "Recursion")?.masteryScore)

        // Second clarification signal -> confidence becomes LOW
        learnerRepo.recordTutorSignal(
            "mat-1",
            TutorLearningSignal("Recursion", System.currentTimeMillis(), TutorSignalType.ASKED_FOR_CLARIFICATION)
        )

        val updatedMastery = learnerRepo.getMastery("mat-1", "Recursion")
        assertEquals(ConfidenceLevel.LOW, updatedMastery?.confidenceLevel)
        // Mastery score remains intact (not falsely punished)
        assertEquals(70, updatedMastery?.masteryScore)
    }

    // =========================================================================
    // 7. END-TO-END VIEWMODEL INTEGRATION TEST
    // =========================================================================

    private class FakeMaterialRepository(
        initialMaterials: List<LearningMaterial> = emptyList()
    ) : LearningMaterialRepository {
        private val state = MutableStateFlow(initialMaterials)
        override fun getMaterialsFlow(): Flow<List<LearningMaterial>> = state
        override suspend fun getMaterialById(id: String): LearningMaterial? = state.value.firstOrNull { it.id == id }
        override suspend fun getContentUnitsForMaterial(materialId: String): List<PdfContentUnit> =
            state.value.firstOrNull { it.id == materialId }?.contentUnits ?: emptyList()
        override suspend fun ingestPdfFromUri(uri: android.net.Uri, rawFileName: String?): Result<LearningMaterial> = Result.failure(NotImplementedError())
        override suspend fun ingestPdfFromStream(inputStream: java.io.InputStream, fileName: String): Result<LearningMaterial> = Result.failure(NotImplementedError())
        override suspend fun deleteMaterial(id: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeAdaptiveAssessmentRepo(
        val assessmentToReturn: Assessment
    ) : AdaptiveAssessmentRepository {
        override suspend fun generateAssessment(
            materialId: String,
            documentName: String,
            scope: String,
            difficulty: AssessmentDifficulty,
            questionCount: Int,
            contextChunks: List<KnowledgeChunk>
        ): Result<Assessment> = Result.success(assessmentToReturn)

        override fun evaluateAssessment(
            assessment: Assessment,
            studentAnswers: Map<String, String>
        ): AssessmentResult {
            val qResults = assessment.questions.map { q ->
                val selected = studentAnswers[q.id].orEmpty()
                val isCorrect = selected.equals(q.correctAnswer, ignoreCase = true)
                QuestionResult(
                    questionId = q.id,
                    questionText = q.question,
                    selectedAnswer = selected,
                    correctAnswer = q.correctAnswer,
                    correct = isCorrect,
                    explanation = q.explanation,
                    topic = q.topic,
                    concept = q.concept,
                    documentName = q.documentName,
                    pageNumber = q.pageNumber,
                    sourceChunkId = q.sourceChunkId
                )
            }
            val correct = qResults.count { it.correct }
            val scorePct = (correct * 100) / assessment.questions.size
            return AssessmentResult(
                assessmentId = assessment.id,
                totalQuestions = assessment.questions.size,
                correctAnswers = correct,
                scorePercentage = scorePct,
                topicPerformance = listOf(TopicPerformance(assessment.topic, assessment.questions.size, correct, scorePct)),
                weakTopics = if (scorePct < 60) listOf(assessment.topic) else emptyList(),
                questionResults = qResults
            )
        }
    }

    @Test
    fun testViewModel_assessmentSubmissionImmediatelyUpdatesMasteryAndRecommendation() = runTest {
        val sampleMat = LearningMaterial(
            id = "mat-java",
            name = "Java Guide",
            pageCount = 1,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(PdfContentUnit("u1", "mat-java", "Java.pdf", 1, "Polymorphism text"))
        )
        val matRepo = FakeMaterialRepository(listOf(sampleMat))
        val kbRepo = LocalKnowledgeBaseRepository()
        kbRepo.ingestMaterial(sampleMat)

        val testAssessment = Assessment(
            id = "test-a",
            materialId = "mat-java",
            title = "Polymorphism Quiz",
            topic = "Polymorphism",
            questions = listOf(
                AssessmentQuestion("q1", "Q1?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "A", "Exp", "Polymorphism", "C", AssessmentDifficulty.EASY, "mat-java", "Java.pdf", 1, "u1")
            )
        )
        val assessmentRepo = FakeAdaptiveAssessmentRepo(testAssessment)
        val learnerRepo = LocalLearnerModelRepository()

        val viewModel = LearningCompanionViewModel(
            materialRepository = matRepo,
            knowledgeBaseRepository = kbRepo,
            adaptiveAssessmentRepository = assessmentRepo,
            learnerModelRepository = learnerRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Initial State: No history
        assertFalse(viewModel.uiState.value.hasMasteryHistory)
        assertEquals(0, viewModel.uiState.value.overallMastery)

        // 2. Open & Generate Assessment
        viewModel.openAssessmentConfig(sampleMat)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.generateAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Answer correctly & submit
        viewModel.selectAnswer("q1", "A")
        viewModel.submitAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        // 4. Verify mastery was immediately updated in UI State
        val state = viewModel.uiState.value
        assertTrue(state.hasMasteryHistory)
        assertEquals(1, state.topicMasteries.size)
        assertEquals("Polymorphism", state.topicMasteries[0].topic)
        assertEquals(70, state.topicMasteries[0].masteryScore) // (50 * 0.6) + (100 * 0.4) = 30 + 40 = 70%
        assertEquals(70, state.overallMastery)
    }

    @Test
    fun testMasteryScreenNavigationAndPracticeAction() = runTest {
        val viewModel = LearningCompanionViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Open Mastery Screen
        viewModel.openMasteryScreen()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isMasteryScreenVisible)

        // Trigger Practice Topic from Mastery Screen
        viewModel.practiceTopicFromMastery("Inheritance")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isMasteryScreenVisible)
        assertTrue(viewModel.uiState.value.isAssessmentConfigVisible)
        assertEquals("Inheritance", viewModel.uiState.value.selectedAssessmentTopic)
    }
}
