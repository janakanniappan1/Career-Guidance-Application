package com.example

import com.example.data.repository.LocalKnowledgeBaseRepository
import com.example.data.repository.RawQuestionDto
import com.example.domain.assessment.QuestionFingerprint
import com.example.domain.assessment.QuestionFingerprintManager
import com.example.domain.assessment.QuestionValidator
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.PdfContentUnit
import com.example.domain.model.adaptive.Assessment
import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentQuestion
import com.example.domain.model.adaptive.AssessmentQuestionType
import com.example.domain.model.adaptive.AssessmentResult
import com.example.domain.repository.AdaptiveAssessmentRepository
import com.example.domain.repository.LearningMaterialRepository
import com.example.data.repository.SupabaseAdaptiveAssessmentRepository
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
class AdaptiveAssessmentUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val sampleChunks = listOf(
        KnowledgeChunk(
            id = "chunk-101",
            materialId = "mat-java",
            documentName = "Java Programming.pdf",
            pageNumber = 42,
            text = "Inheritance in Java allows a subclass to inherit fields and methods from a superclass using the 'extends' keyword.",
            topic = "Inheritance",
            concept = "Subclassing"
        ),
        KnowledgeChunk(
            id = "chunk-102",
            materialId = "mat-java",
            documentName = "Java Programming.pdf",
            pageNumber = 56,
            text = "Polymorphism enables an object to take on many forms, commonly through method overriding and method overloading.",
            topic = "Polymorphism",
            concept = "Method Overriding"
        ),
        KnowledgeChunk(
            id = "chunk-103",
            materialId = "mat-java",
            documentName = "Java Programming.pdf",
            pageNumber = 12,
            text = "Variables in Java must be declared with a data type such as int, double, boolean, or String.",
            topic = "Variables",
            concept = "Data Types"
        )
    )

    // =========================================================================
    // 1. QUESTION FINGERPRINTING & DUPLICATE DETECTION TESTS
    // =========================================================================

    @Test
    fun testQuestionFingerprint_normalizesCasePunctuationAndWhitespace() {
        val q1 = "What is Inheritance in Java?"
        val q2 = "what  is, inheritance in java???"
        val q3 = "WHAT IS INHERITANCE IN JAVA"

        val fp1 = QuestionFingerprint.generateFingerprint(q1)
        val fp2 = QuestionFingerprint.generateFingerprint(q2)
        val fp3 = QuestionFingerprint.generateFingerprint(q3)

        assertEquals("what is inheritance in java", fp1)
        assertEquals(fp1, fp2)
        assertEquals(fp1, fp3)
    }

    @Test
    fun testQuestionFingerprintManager_tracksSeenQuestionsInSession() {
        val manager = QuestionFingerprintManager()
        val question = "Explain method overriding in polymorphism."

        assertFalse(manager.isSeen(question))
        assertTrue(manager.record(question))
        assertTrue(manager.isSeen(question))
        // Re-recording returns false (already present)
        assertFalse(manager.record(question))

        manager.clear()
        assertFalse(manager.isSeen(question))
    }

    // =========================================================================
    // 2. STRICT QUESTION VALIDATION TESTS (9 RULES)
    // =========================================================================

    @Test
    fun testQuestionValidator_validMcqQuestion_passes() {
        val validator = QuestionValidator()
        val validIds = setOf("chunk-101")
        val seen = mutableSetOf<String>()

        val validQ = AssessmentQuestion(
            id = "q-1",
            question = "Which keyword is used to inherit a class in Java?",
            type = AssessmentQuestionType.MCQ,
            options = listOf("extends", "implements", "inherits", "super"),
            correctAnswer = "extends",
            explanation = "In Java, the extends keyword establishes class inheritance.",
            topic = "Inheritance",
            concept = "Subclassing",
            difficulty = AssessmentDifficulty.EASY,
            materialId = "mat-java",
            documentName = "Java Programming.pdf",
            pageNumber = 42,
            sourceChunkId = "chunk-101"
        )

        val result = validator.validateQuestion(validQ, validIds, seen)
        assertTrue(result.isValid)
        assertNull(result.reason)
    }

    @Test
    fun testQuestionValidator_rejectsEmptyQuestion() {
        val validator = QuestionValidator()
        val q = AssessmentQuestion(
            id = "q-1",
            question = "   ",
            type = AssessmentQuestionType.MCQ,
            options = listOf("A", "B", "C", "D"),
            correctAnswer = "A",
            explanation = "Exp",
            topic = "Topic",
            materialId = "m1",
            documentName = "d1",
            pageNumber = 1,
            sourceChunkId = "chunk-101"
        )
        val res = validator.validateQuestion(q, setOf("chunk-101"), mutableSetOf())
        assertFalse(res.isValid)
        assertEquals("Question text is empty", res.reason)
    }

    @Test
    fun testQuestionValidator_rejectsMcqWithInvalidOptionCount() {
        val validator = QuestionValidator()
        // 3 options instead of 4
        val q = AssessmentQuestion(
            id = "q-1",
            question = "Sample question?",
            type = AssessmentQuestionType.MCQ,
            options = listOf("extends", "implements", "inherits"),
            correctAnswer = "extends",
            explanation = "Exp",
            topic = "Topic",
            materialId = "m1",
            documentName = "d1",
            pageNumber = 1,
            sourceChunkId = "chunk-101"
        )
        val res = validator.validateQuestion(q, setOf("chunk-101"), mutableSetOf())
        assertFalse(res.isValid)
        assertTrue(res.reason!!.contains("MCQ must have exactly 4 options"))
    }

    @Test
    fun testQuestionValidator_rejectsCorrectAnswerNotInOptions() {
        val validator = QuestionValidator()
        val q = AssessmentQuestion(
            id = "q-1",
            question = "Which keyword is used for inheritance?",
            type = AssessmentQuestionType.MCQ,
            options = listOf("implements", "interface", "super", "this"),
            correctAnswer = "extends", // Not in options
            explanation = "Exp",
            topic = "Inheritance",
            materialId = "m1",
            documentName = "d1",
            pageNumber = 1,
            sourceChunkId = "chunk-101"
        )
        val res = validator.validateQuestion(q, setOf("chunk-101"), mutableSetOf())
        assertFalse(res.isValid)
        assertTrue(res.reason!!.contains("is not in options"))
    }

    @Test
    fun testQuestionValidator_rejectsHallucinatedSourceChunkId() {
        val validator = QuestionValidator()
        val validIds = setOf("chunk-101", "chunk-102")
        val q = AssessmentQuestion(
            id = "q-1",
            question = "Sample question?",
            type = AssessmentQuestionType.MCQ,
            options = listOf("A", "B", "C", "D"),
            correctAnswer = "A",
            explanation = "Exp",
            topic = "Inheritance",
            materialId = "m1",
            documentName = "d1",
            pageNumber = 1,
            sourceChunkId = "fake-hallucinated-id-999" // Does not exist
        )
        val res = validator.validateQuestion(q, validIds, mutableSetOf())
        assertFalse(res.isValid)
        assertTrue(res.reason!!.contains("does not belong to supplied chunks"))
    }

    @Test
    fun testQuestionValidator_rejectsDuplicateQuestionWithinAssessment() {
        val validator = QuestionValidator()
        val validIds = setOf("chunk-101")
        val seen = mutableSetOf("which keyword is used to inherit a class in java")

        val duplicateQ = AssessmentQuestion(
            id = "q-2",
            question = "Which keyword is used to inherit a class in Java?",
            type = AssessmentQuestionType.MCQ,
            options = listOf("extends", "implements", "inherits", "super"),
            correctAnswer = "extends",
            explanation = "Exp",
            topic = "Inheritance",
            materialId = "m1",
            documentName = "d1",
            pageNumber = 1,
            sourceChunkId = "chunk-101"
        )
        val res = validator.validateQuestion(duplicateQ, validIds, seen)
        assertFalse(res.isValid)
        assertEquals("Duplicate question within assessment", res.reason)
    }

    @Test
    fun testQuestionValidator_filtersInvalidAndDuplicateQuestionsFromList() {
        val validator = QuestionValidator()
        val rawQuestions = listOf(
            // Valid Question 1
            AssessmentQuestion(
                id = "q-1",
                question = "What keyword extends a class?",
                type = AssessmentQuestionType.MCQ,
                options = listOf("extends", "implements", "with", "super"),
                correctAnswer = "extends",
                explanation = "Extends is the keyword",
                topic = "Inheritance",
                materialId = "m1",
                documentName = "d1",
                pageNumber = 1,
                sourceChunkId = "chunk-101"
            ),
            // Duplicate of Question 1
            AssessmentQuestion(
                id = "q-2",
                question = "what keyword, extends a class???",
                type = AssessmentQuestionType.MCQ,
                options = listOf("extends", "implements", "with", "super"),
                correctAnswer = "extends",
                explanation = "Extends is the keyword",
                topic = "Inheritance",
                materialId = "m1",
                documentName = "d1",
                pageNumber = 1,
                sourceChunkId = "chunk-101"
            ),
            // Invalid: Hallucinated chunk ID
            AssessmentQuestion(
                id = "q-3",
                question = "What is polymorphism?",
                type = AssessmentQuestionType.MCQ,
                options = listOf("Many forms", "Single form", "No form", "Static"),
                correctAnswer = "Many forms",
                explanation = "Exp",
                topic = "Polymorphism",
                materialId = "m1",
                documentName = "d1",
                pageNumber = 1,
                sourceChunkId = "non-existent-chunk"
            ),
            // Valid Question 2
            AssessmentQuestion(
                id = "q-4",
                question = "What are primitive variables in Java?",
                type = AssessmentQuestionType.SHORT_ANSWER,
                options = emptyList(),
                correctAnswer = "int, double, boolean",
                explanation = "Basic data types in Java",
                topic = "Variables",
                materialId = "m1",
                documentName = "d1",
                pageNumber = 1,
                sourceChunkId = "chunk-103"
            )
        )

        val filtered = validator.filterValidQuestions(rawQuestions, sampleChunks)
        assertEquals(2, filtered.size)
        assertEquals("q-1", filtered[0].id)
        assertEquals("q-4", filtered[1].id)
    }

    // =========================================================================
    // 3. ASSESSMENT SCORING & WEAK-TOPIC ANALYSIS (ACCURACY < 60%)
    // =========================================================================

    @Test
    fun testAssessmentEvaluation_scoresAndIdentifiesWeakTopicsCorrectly() {
        val repo = SupabaseAdaptiveAssessmentRepository()

        val assessment = Assessment(
            id = "test-assessment-1",
            materialId = "mat-java",
            title = "Java Comprehensive Assessment",
            topic = "All Topics",
            questions = listOf(
                // Inheritance: 4 questions (Student gets 2 / 4 = 50% -> WEAK)
                AssessmentQuestion("q1", "Q1?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "A", "E1", "Inheritance", "C1", AssessmentDifficulty.MEDIUM, "m", "d", 1, "c1"),
                AssessmentQuestion("q2", "Q2?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "A", "E2", "Inheritance", "C1", AssessmentDifficulty.MEDIUM, "m", "d", 1, "c1"),
                AssessmentQuestion("q3", "Q3?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "A", "E3", "Inheritance", "C1", AssessmentDifficulty.MEDIUM, "m", "d", 1, "c1"),
                AssessmentQuestion("q4", "Q4?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "A", "E4", "Inheritance", "C1", AssessmentDifficulty.MEDIUM, "m", "d", 1, "c1"),

                // Polymorphism: 3 questions (Student gets 3 / 3 = 100% -> NOT WEAK)
                AssessmentQuestion("q5", "Q5?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "B", "E5", "Polymorphism", "C2", AssessmentDifficulty.MEDIUM, "m", "d", 2, "c2"),
                AssessmentQuestion("q6", "Q6?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "B", "E6", "Polymorphism", "C2", AssessmentDifficulty.MEDIUM, "m", "d", 2, "c2"),
                AssessmentQuestion("q7", "Q7?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "B", "E7", "Polymorphism", "C2", AssessmentDifficulty.MEDIUM, "m", "d", 2, "c2"),

                // Variables: 3 questions (Student gets 2 / 3 = 67% -> NOT WEAK)
                AssessmentQuestion("q8", "Q8?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "C", "E8", "Variables", "C3", AssessmentDifficulty.MEDIUM, "m", "d", 3, "c3"),
                AssessmentQuestion("q9", "Q9?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "C", "E9", "Variables", "C3", AssessmentDifficulty.MEDIUM, "m", "d", 3, "c3"),
                AssessmentQuestion("q10", "Q10?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "C", "E10", "Variables", "C3", AssessmentDifficulty.MEDIUM, "m", "d", 3, "c3")
            )
        )

        val studentAnswers = mapOf(
            // Inheritance: q1 & q2 correct, q3 & q4 incorrect
            "q1" to "A",
            "q2" to "A",
            "q3" to "Wrong",
            "q4" to "Wrong",

            // Polymorphism: all correct
            "q5" to "B",
            "q6" to "B",
            "q7" to "B",

            // Variables: q8 & q9 correct, q10 incorrect
            "q8" to "C",
            "q9" to "C",
            "q10" to "Wrong"
        )

        val result = repo.evaluateAssessment(assessment, studentAnswers)

        assertEquals(10, result.totalQuestions)
        assertEquals(7, result.correctAnswers)
        assertEquals(70, result.scorePercentage) // 7 / 10 = 70%

        // Verify Topic Performance
        val inheritancePerf = result.topicPerformance.first { it.topic == "Inheritance" }
        assertEquals(4, inheritancePerf.totalQuestions)
        assertEquals(2, inheritancePerf.correctAnswers)
        assertEquals(50, inheritancePerf.accuracyPercentage)
        assertTrue(inheritancePerf.isWeak) // 50% < 60%

        val polyPerf = result.topicPerformance.first { it.topic == "Polymorphism" }
        assertEquals(3, polyPerf.totalQuestions)
        assertEquals(3, polyPerf.correctAnswers)
        assertEquals(100, polyPerf.accuracyPercentage)
        assertFalse(polyPerf.isWeak)

        val varPerf = result.topicPerformance.first { it.topic == "Variables" }
        assertEquals(3, varPerf.totalQuestions)
        assertEquals(2, varPerf.correctAnswers)
        assertEquals(66, varPerf.accuracyPercentage)
        assertFalse(varPerf.isWeak)

        // Weak topics: Only Inheritance is < 60%
        assertEquals(listOf("Inheritance"), result.weakTopics)
        assertTrue(result.hasWeakTopics)
    }

    @Test
    fun testWeakTopics_whenAllTopicsAboveThreshold_reportsNoWeakTopics() {
        val repo = SupabaseAdaptiveAssessmentRepository()
        val assessment = Assessment(
            id = "a-high",
            materialId = "m1",
            title = "Title",
            topic = "All Topics",
            questions = listOf(
                AssessmentQuestion("q1", "Q1?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "A", "E", "TopicA", "C", AssessmentDifficulty.EASY, "m", "d", 1, "c1"),
                AssessmentQuestion("q2", "Q2?", AssessmentQuestionType.MCQ, listOf("A", "B", "C", "D"), "A", "E", "TopicB", "C", AssessmentDifficulty.EASY, "m", "d", 1, "c1")
            )
        )
        val answers = mapOf("q1" to "A", "q2" to "A")
        val result = repo.evaluateAssessment(assessment, answers)

        assertEquals(100, result.scorePercentage)
        assertTrue(result.weakTopics.isEmpty())
        assertFalse(result.hasWeakTopics)
    }

    // =========================================================================
    // 4. VIEWMODEL ADAPTIVE ASSESSMENT SESSION FLOW TESTS
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

    private class FakeAssessmentRepository(
        var generateResult: Result<Assessment> = Result.failure(Exception("Not initialized"))
    ) : AdaptiveAssessmentRepository {
        var lastQuestionCount: Int = 0
        var lastScope: String? = null
        var lastDifficulty: AssessmentDifficulty? = null
        var lastContextChunks: List<KnowledgeChunk> = emptyList()

        override suspend fun generateAssessment(
            materialId: String,
            documentName: String,
            scope: String,
            difficulty: AssessmentDifficulty,
            questionCount: Int,
            contextChunks: List<KnowledgeChunk>
        ): Result<Assessment> {
            lastScope = scope
            lastDifficulty = difficulty
            lastQuestionCount = questionCount
            lastContextChunks = contextChunks
            return generateResult
        }

        override fun evaluateAssessment(
            assessment: Assessment,
            studentAnswers: Map<String, String>
        ): AssessmentResult {
            val qResults = assessment.questions.map { q ->
                val selected = studentAnswers[q.id].orEmpty()
                val isCorrect = selected.equals(q.correctAnswer, ignoreCase = true)
                com.example.domain.model.adaptive.QuestionResult(
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
                topicPerformance = emptyList(),
                weakTopics = if (scorePct < 60) listOf(assessment.topic) else emptyList(),
                questionResults = qResults
            )
        }
    }

    @Test
    fun testOpenAssessmentConfig_withoutKnowledgeBase_showsWarning() = runTest {
        val emptyRepo = FakeMaterialRepository(emptyList())
        val kbRepo = LocalKnowledgeBaseRepository()
        val assessmentRepo = FakeAssessmentRepository()

        val viewModel = LearningCompanionViewModel(
            materialRepository = emptyRepo,
            knowledgeBaseRepository = kbRepo,
            adaptiveAssessmentRepository = assessmentRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openAssessmentConfig()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAssessmentConfigVisible)
        assertEquals("Build a Knowledge Base before creating an assessment.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testFullAssessmentWorkflow_configGenerateAnswerSubmitAndResult() = runTest {
        val sampleMaterial = LearningMaterial(
            id = "mat-java",
            name = "Java Programming",
            pageCount = 3,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                PdfContentUnit("u1", "mat-java", "Java Programming.pdf", 42, "Inheritance info"),
                PdfContentUnit("u2", "mat-java", "Java Programming.pdf", 56, "Polymorphism info")
            )
        )

        val materialRepo = FakeMaterialRepository(listOf(sampleMaterial))
        val kbRepo = LocalKnowledgeBaseRepository()
        kbRepo.ingestMaterial(sampleMaterial)

        val mockAssessment = Assessment(
            id = "mock-a-1",
            materialId = "mat-java",
            title = "Java Comprehensive Assessment",
            topic = "All Topics",
            questions = listOf(
                AssessmentQuestion(
                    id = "q-1",
                    question = "Which keyword creates a subclass?",
                    type = AssessmentQuestionType.MCQ,
                    options = listOf("extends", "implements", "inherits", "super"),
                    correctAnswer = "extends",
                    explanation = "Extends is the inheritance keyword in Java.",
                    topic = "Inheritance",
                    concept = "Subclassing",
                    difficulty = AssessmentDifficulty.MEDIUM,
                    materialId = "mat-java",
                    documentName = "Java Programming.pdf",
                    pageNumber = 42,
                    sourceChunkId = "u1"
                ),
                AssessmentQuestion(
                    id = "q-2",
                    question = "What is method overriding?",
                    type = AssessmentQuestionType.MCQ,
                    options = listOf("Dynamic polymorphism", "Static variables", "Package access", "Garbage collection"),
                    correctAnswer = "Dynamic polymorphism",
                    explanation = "Overriding is runtime polymorphism.",
                    topic = "Polymorphism",
                    concept = "Dynamic dispatch",
                    difficulty = AssessmentDifficulty.HARD,
                    materialId = "mat-java",
                    documentName = "Java Programming.pdf",
                    pageNumber = 56,
                    sourceChunkId = "u2"
                )
            )
        )

        val fakeAssessmentRepo = FakeAssessmentRepository(generateResult = Result.success(mockAssessment))

        val viewModel = LearningCompanionViewModel(
            materialRepository = materialRepo,
            knowledgeBaseRepository = kbRepo,
            adaptiveAssessmentRepository = fakeAssessmentRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Open Configuration
        viewModel.openAssessmentConfig(sampleMaterial)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAssessmentConfigVisible)
        assertEquals(sampleMaterial.id, viewModel.uiState.value.selectedAssessmentMaterial?.id)

        // 2. Select difficulty and question count
        viewModel.selectAssessmentDifficulty(AssessmentDifficulty.HARD)
        viewModel.selectAssessmentQuestionCount(5)
        assertEquals(AssessmentDifficulty.HARD, viewModel.uiState.value.selectedAssessmentDifficulty)
        assertEquals(5, viewModel.uiState.value.selectedQuestionCount)

        // 3. Generate Assessment
        viewModel.generateAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAssessmentConfigVisible)
        assertTrue(viewModel.uiState.value.isAssessmentQuizVisible)
        assertEquals(mockAssessment.id, viewModel.uiState.value.activeAssessment?.id)
        assertEquals(0, viewModel.uiState.value.currentQuestionIndex)

        // 4. Answer Question 1
        viewModel.selectAnswer("q-1", "extends")
        assertEquals("extends", viewModel.uiState.value.studentAnswers["q-1"])

        // 5. Navigate to Question 2
        viewModel.nextQuestion()
        assertEquals(1, viewModel.uiState.value.currentQuestionIndex)

        // Answer Question 2
        viewModel.selectAnswer("q-2", "Dynamic polymorphism")

        // 6. Submit Assessment
        viewModel.submitAssessment()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAssessmentQuizVisible)
        assertTrue(viewModel.uiState.value.isAssessmentResultVisible)
        assertNotNull(viewModel.uiState.value.assessmentResult)
        assertEquals(2, viewModel.uiState.value.assessmentResult?.correctAnswers)
        assertEquals(100, viewModel.uiState.value.assessmentResult?.scorePercentage)

        // 7. Verify overall mastery is updated per Weighted Evidence Mastery Model ((50 * 0.6) + (100 * 0.4) = 70%)
        assertEquals(70, viewModel.uiState.value.overallMastery)

        // 8. Close Result
        viewModel.closeAssessmentResult()
        assertFalse(viewModel.uiState.value.isAssessmentResultVisible)
    }

    @Test
    fun testAskTutorAboutWeakTopic_opensTutorWithPreFilledTopicQuery() = runTest {
        val viewModel = LearningCompanionViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.askTutorAboutWeakTopic("Inheritance")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTutorScreenVisible)
        assertFalse(viewModel.uiState.value.isAssessmentResultVisible)
        // Message was sent with "Inheritance" in query
        assertTrue(viewModel.uiState.value.tutorMessages.isNotEmpty())
        assertTrue(viewModel.uiState.value.tutorMessages[0].text.contains("Inheritance"))
    }

    @Test
    fun testQuestionBankMaterial_generatesValidSourceGroundedQuestions() = runTest {
        val kbRepo = LocalKnowledgeBaseRepository()
        val qbMaterial = LearningMaterial(
            id = "mat_22am501",
            name = "22AM501 PT1 Student Question Bank",
            pageCount = 2,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                PdfContentUnit(
                    id = "qb_1",
                    materialId = "mat_22am501",
                    documentName = "22AM501 PT1 Student Question Bank",
                    pageNumber = 1,
                    text = """
                        PART A - Short Answer Questions
                        1. Define Machine Learning and state the role of training experience E, task T, and performance measure P.
                        2. Distinguish between Supervised Learning and Unsupervised Learning with suitable examples.
                        3. What is Inductive Bias in concept learning and why is it necessary for generalization?
                        4. Define Version Space, General Boundary (G), and Specific Boundary (S) in Candidate Elimination.
                    """.trimIndent()
                ),
                PdfContentUnit(
                    id = "qb_2",
                    materialId = "mat_22am501",
                    documentName = "22AM501 PT1 Student Question Bank",
                    pageNumber = 2,
                    text = """
                        PART B - Descriptive Questions
                        5. Explain the ID3 Decision Tree learning algorithm using Entropy and Information Gain metrics.
                        6. Discuss Linear Regression and Gradient Descent optimization for minimizing mean squared error.
                    """.trimIndent()
                )
            )
        )
        kbRepo.ingestMaterial(qbMaterial)
        val chunks = kbRepo.getChunks("mat_22am501")
        assertTrue(chunks.isNotEmpty())

        val repo = SupabaseAdaptiveAssessmentRepository()
        val questions = repo.generateSourceGroundedQuestions(
            contextChunks = chunks,
            scope = "All Topics",
            difficulty = AssessmentDifficulty.MIXED,
            questionCount = 5,
            materialId = qbMaterial.id,
            documentName = qbMaterial.name
        )

        assertEquals(5, questions.size)
        val validator = QuestionValidator()
        val validQuestions = validator.filterValidQuestions(questions, chunks)
        assertEquals(5, validQuestions.size)
    }
}
