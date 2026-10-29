package com.example

import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.PdfContentUnit
import com.example.domain.model.TutorMessageRole
import com.example.domain.model.TutorResponse
import com.example.domain.model.TutorSource
import com.example.domain.repository.KnowledgeBaseRepository
import com.example.domain.repository.LearningMaterialRepository
import com.example.domain.repository.LearningTutorRepository
import com.example.data.repository.LocalKnowledgeBaseRepository
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
class SourceGroundedTutorUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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

    private class FakeTutorRepository(
        var responseToReturn: Result<TutorResponse> = Result.success(
            TutorResponse(
                answer = "Binary search runs in O(log n) time complexity.",
                sources = listOf(
                    TutorSource(
                        materialId = "mat-1",
                        documentName = "Algorithms.pdf",
                        pageNumber = 14,
                        chunkId = "chunk-1"
                    )
                ),
                grounded = true
            )
        )
    ) : LearningTutorRepository {
        var lastQuestion: String? = null
        var lastContextChunks: List<KnowledgeChunk> = emptyList()

        override suspend fun askTutor(
            question: String,
            contextChunks: List<KnowledgeChunk>
        ): Result<TutorResponse> {
            lastQuestion = question
            lastContextChunks = contextChunks
            return responseToReturn
        }
    }

    @Test
    fun testTutorRefusal_whenNoMaterialsUploaded() = runTest {
        val emptyMaterialRepo = FakeMaterialRepository(emptyList())
        val kbRepo = LocalKnowledgeBaseRepository()
        val tutorRepo = FakeTutorRepository()

        val viewModel = LearningCompanionViewModel(
            materialRepository = emptyMaterialRepo,
            knowledgeBaseRepository = kbRepo,
            tutorRepository = tutorRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTutorInput("What is binary search?")
        viewModel.askTutor()
        testDispatcher.scheduler.advanceUntilIdle()

        val messages = viewModel.uiState.value.tutorMessages
        assertEquals(2, messages.size)
        val userMsg = messages[0]
        val assistantMsg = messages[1]

        assertEquals(TutorMessageRole.USER, userMsg.role)
        assertEquals("What is binary search?", userMsg.text)

        assertEquals(TutorMessageRole.ASSISTANT, assistantMsg.role)
        assertFalse(assistantMsg.isGrounded)
        assertTrue(assistantMsg.text.contains("I couldn't find this topic in your uploaded learning material"))
        assertNotNull(assistantMsg.unsupportedReason)
        // Ensure Gemini backend was NOT called unnecessarily
        assertNull(tutorRepo.lastQuestion)
    }

    @Test
    fun testTutorRefusal_whenQueryNotCoveredInMaterials() = runTest {
        val sampleMaterial = LearningMaterial(
            id = "mat-algo",
            name = "Algorithms Textbook",
            pageCount = 2,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                PdfContentUnit(
                    id = "unit-1",
                    materialId = "mat-algo",
                    documentName = "Algorithms.pdf",
                    pageNumber = 1,
                    text = "Sorting algorithms include Bubble Sort, Merge Sort, and Quick Sort."
                )
            )
        )
        val materialRepo = FakeMaterialRepository(listOf(sampleMaterial))
        val kbRepo = LocalKnowledgeBaseRepository()
        kbRepo.ingestMaterial(sampleMaterial)
        val tutorRepo = FakeTutorRepository()

        val viewModel = LearningCompanionViewModel(
            materialRepository = materialRepo,
            knowledgeBaseRepository = kbRepo,
            tutorRepository = tutorRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Ask about a completely unrelated query not present in the material
        viewModel.updateTutorInput("Explain the French Revolution and Napoleon.")
        viewModel.askTutor()
        testDispatcher.scheduler.advanceUntilIdle()

        val messages = viewModel.uiState.value.tutorMessages
        assertEquals(2, messages.size)
        val assistantMsg = messages[1]

        assertEquals(TutorMessageRole.ASSISTANT, assistantMsg.role)
        assertFalse(assistantMsg.isGrounded)
        assertEquals("I couldn't find this topic in your uploaded learning material.", assistantMsg.text)
        assertEquals("⚠️ Not covered in uploaded material", assistantMsg.unsupportedReason)
        // Backend not called since no chunks were found
        assertNull(tutorRepo.lastQuestion)
    }

    @Test
    fun testGroundedAnswer_retrievesRelevantChunksAndDisplaysSources() = runTest {
        val sampleMaterial = LearningMaterial(
            id = "mat-algo",
            name = "Algorithms Textbook",
            pageCount = 2,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                PdfContentUnit(
                    id = "unit-1",
                    materialId = "mat-algo",
                    documentName = "Algorithms.pdf",
                    pageNumber = 42,
                    text = "Binary Search Algorithm: A divide and conquer method with O(log n) performance."
                )
            )
        )
        val materialRepo = FakeMaterialRepository(listOf(sampleMaterial))
        val kbRepo = LocalKnowledgeBaseRepository()
        kbRepo.ingestMaterial(sampleMaterial)

        val tutorRepo = FakeTutorRepository(
            responseToReturn = Result.success(
                TutorResponse(
                    answer = "Binary search is a divide and conquer technique that runs in O(log n) time.",
                    sources = listOf(
                        TutorSource(
                            materialId = "mat-algo",
                            documentName = "Algorithms.pdf",
                            pageNumber = 42,
                            chunkId = "chunk-42",
                            previewText = "Binary Search Algorithm: A divide and conquer method..."
                        )
                    ),
                    grounded = true
                )
            )
        )

        val viewModel = LearningCompanionViewModel(
            materialRepository = materialRepo,
            knowledgeBaseRepository = kbRepo,
            tutorRepository = tutorRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTutorInput("What is binary search algorithm?")
        viewModel.askTutor()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify retrieval passed relevant chunks to backend
        assertNotNull(tutorRepo.lastQuestion)
        assertTrue(tutorRepo.lastContextChunks.isNotEmpty())
        assertEquals(42, tutorRepo.lastContextChunks[0].pageNumber)
        assertEquals("Algorithms.pdf", tutorRepo.lastContextChunks[0].documentName)

        // Verify assistant message in UI state
        val messages = viewModel.uiState.value.tutorMessages
        assertEquals(2, messages.size)
        val assistantMsg = messages[1]

        assertTrue(assistantMsg.isGrounded)
        assertNull(assistantMsg.unsupportedReason)
        assertEquals(1, assistantMsg.sources.size)
        assertEquals(42, assistantMsg.sources[0].pageNumber)
        assertEquals("Algorithms.pdf", assistantMsg.sources[0].documentName)
        assertEquals("Algorithms.pdf • Page 42", assistantMsg.sources[0].displayCitation)
    }

    @Test
    fun testSourcePreviewModal_opensWhenSourceClicked() = runTest {
        val viewModel = LearningCompanionViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val source = TutorSource(
            materialId = "mat-1",
            documentName = "DataStructures.pdf",
            pageNumber = 15,
            chunkId = "c-15",
            previewText = "Trees and binary heaps hierarchy"
        )

        viewModel.openTutorSourcePreview(source)

        val state = viewModel.uiState.value
        assertTrue(state.isSourcePreviewVisible)
        assertNotNull(state.selectedSourcePreviewChunk)
        assertEquals(15, state.selectedSourcePreviewChunk?.pageNumber)
        assertEquals("DataStructures.pdf", state.selectedSourcePreviewChunk?.documentName)

        viewModel.dismissSourcePreview()
        assertFalse(viewModel.uiState.value.isSourcePreviewVisible)
        assertNull(viewModel.uiState.value.selectedSourcePreviewChunk)
    }

    @Test
    fun testBlankQuestion_doesNotSendMessage() = runTest {
        val viewModel = LearningCompanionViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTutorInput("   ")
        viewModel.askTutor()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tutorMessages.isEmpty())
        assertFalse(viewModel.uiState.value.isTutorSearching)
        assertFalse(viewModel.uiState.value.isTutorGenerating)
    }

    @Test
    fun testAcronymAndConversationalRetrieval_tellMeAboutML_andTrafficRefusal() = runTest {
        val kbRepo = LocalKnowledgeBaseRepository()
        val material = LearningMaterial(
            id = "mat_22am501",
            name = "22AM501 PT1 Student Question Bank",
            pageCount = 2,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                PdfContentUnit(
                    id = "qb_p1",
                    materialId = "mat_22am501",
                    documentName = "22AM501 PT1 Student Question Bank.pdf",
                    pageNumber = 1,
                    text = "PART A\n1. Define Machine Learning and compare supervised learning with unsupervised learning.\n2. Explain Find-S algorithm and Version Space in concept learning."
                ),
                PdfContentUnit(
                    id = "qb_p2",
                    materialId = "mat_22am501",
                    documentName = "22AM501 PT1 Student Question Bank.pdf",
                    pageNumber = 2,
                    text = "PART B\n3. Illustrate Decision Tree ID3 algorithm using Information Gain and Entropy."
                )
            )
        )
        kbRepo.ingestMaterial(material)

        // 1. "tell me about ML" -> must match "Machine Learning" chunk on Page 1
        val mlChunks = kbRepo.searchChunks(materialId = null, query = "tell me about ML")
        assertTrue("Query 'tell me about ML' should retrieve Machine Learning chunk", mlChunks.isNotEmpty())
        assertEquals(1, mlChunks.first().pageNumber)

        // 2. "machine learning" -> must match Page 1
        val fullPhraseChunks = kbRepo.searchChunks(materialId = null, query = "machine learning")
        assertTrue("Query 'machine learning' should retrieve Page 1", fullPhraseChunks.isNotEmpty())
        assertEquals(1, fullPhraseChunks.first().pageNumber)

        // 3. "traffic" -> absent in ML question bank, must return empty
        val trafficChunks = kbRepo.searchChunks(materialId = null, query = "traffic")
        assertTrue("Query 'traffic' should return no chunks for ML question bank", trafficChunks.isEmpty())

        // 4. Verify grounded tutor synthesis produces valid cited answer from question bank chunk
        val tutorRepo = com.example.data.repository.SupabaseLearningTutorRepository()
        val synthesized = tutorRepo.synthesizeGroundedTutorResponse("tell me about ML", mlChunks)
        assertTrue(synthesized.grounded)
        assertTrue(synthesized.answer.isNotBlank())
        assertTrue(synthesized.sources.isNotEmpty())
        assertEquals("mat_22am501_p1_c0", synthesized.sources.first().chunkId)
        assertEquals(1, synthesized.sources.first().pageNumber)
    }
}
