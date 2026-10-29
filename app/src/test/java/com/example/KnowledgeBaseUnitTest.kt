package com.example

import com.example.data.repository.LocalKnowledgeBaseRepository
import com.example.domain.knowledge.DeterministicKnowledgeTagger
import com.example.domain.knowledge.PageAwareKnowledgeChunker
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.PdfContentUnit
import com.example.viewmodel.LearningCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeBaseUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testChunkCreation_andPageNumberPreservation() {
        val chunker = PageAwareKnowledgeChunker(tagger = DeterministicKnowledgeTagger())
        val units = listOf(
            PdfContentUnit(
                id = "doc1_p1",
                materialId = "doc1",
                documentName = "Operating Systems",
                pageNumber = 1,
                text = "Chapter 1: Process Management\nA process is a program in execution."
            ),
            PdfContentUnit(
                id = "doc1_p2",
                materialId = "doc1",
                documentName = "Operating Systems",
                pageNumber = 2,
                text = "Section 1.2: CPU Scheduling\nPreemptive and Non-Preemptive scheduling algorithms."
            )
        )

        val chunks = chunker.chunkUnits(units)
        assertEquals(2, chunks.size)

        // Verify Page Number Preservation
        assertEquals(1, chunks[0].pageNumber)
        assertEquals("doc1", chunks[0].materialId)
        assertEquals("Operating Systems", chunks[0].documentName)
        assertEquals("Operating Systems • Page 1", chunks[0].sourceCitation)
        assertTrue(chunks[0].text.contains("process is a program in execution"))

        assertEquals(2, chunks[1].pageNumber)
        assertEquals("Operating Systems • Page 2", chunks[1].sourceCitation)
        assertTrue(chunks[1].text.contains("CPU Scheduling"))
    }

    @Test
    fun testDocumentIsolation() {
        val chunker = PageAwareKnowledgeChunker()
        val unitsDocA = listOf(
            PdfContentUnit("a_p1", "matA", "Book A", 1, "Content from Document A page 1")
        )
        val unitsDocB = listOf(
            PdfContentUnit("b_p1", "matB", "Book B", 1, "Content from Document B page 1")
        )

        val chunks = chunker.chunkUnits(unitsDocA + unitsDocB)
        assertEquals(2, chunks.size)

        assertEquals("matA", chunks[0].materialId)
        assertEquals("Book A", chunks[0].documentName)

        assertEquals("matB", chunks[1].materialId)
        assertEquals("Book B", chunks[1].documentName)
    }

    @Test
    fun testEmptyPageHandling_noEmptyChunksCreated() {
        val chunker = PageAwareKnowledgeChunker()
        val units = listOf(
            PdfContentUnit("p1", "mat1", "Doc", 1, "Real content on page 1"),
            PdfContentUnit("p2", "mat1", "Doc", 2, "   \n\n  "),
            PdfContentUnit("p3", "mat1", "Doc", 3, "")
        )

        val chunks = chunker.chunkUnits(units)
        assertEquals("Only non-empty pages should yield knowledge chunks", 1, chunks.size)
        assertEquals(1, chunks[0].pageNumber)
    }

    @Test
    fun testLargeTextSplitting_preservesPageNumberAcrossSubchunks() {
        val chunker = PageAwareKnowledgeChunker(maxChunkCharSize = 200)
        val longParagraph1 = "Paragraph 1: A very detailed explanation about neural network optimization and backpropagation with gradient descent. " .repeat(3)
        val longParagraph2 = "Paragraph 2: Detailed discussion regarding convolutional layers, pooling filters, and activation functions like ReLU." .repeat(3)
        val unit = PdfContentUnit(
            id = "deep_p4",
            materialId = "deep_doc",
            documentName = "Deep Learning",
            pageNumber = 4,
            text = "$longParagraph1\n\n$longParagraph2"
        )

        val chunks = chunker.chunkSingleUnit(unit)
        assertTrue("Large text should be split into multiple chunks", chunks.size > 1)

        // All split subchunks MUST retain pageNumber = 4
        for (chunk in chunks) {
            assertEquals(4, chunk.pageNumber)
            assertEquals("deep_doc", chunk.materialId)
            assertEquals("Deep Learning", chunk.documentName)
            assertEquals("Deep Learning • Page 4", chunk.sourceCitation)
        }
    }

    @Test
    fun testKnowledgeBaseRepository_ingestAndRetrieveTopics() = runTest {
        val repository = LocalKnowledgeBaseRepository()
        val material = LearningMaterial(
            id = "mat_java",
            name = "Java Programming",
            pageCount = 3,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                PdfContentUnit("j1", "mat_java", "Java Programming", 1, "Chapter 1: Object Oriented Programming\nClasses and Objects"),
                PdfContentUnit("j2", "mat_java", "Java Programming", 2, "Chapter 2: Exception Handling\nTry, catch and finally blocks"),
                PdfContentUnit("j3", "mat_java", "Java Programming", 3, "Chapter 2: Custom Exceptions\nThrowing custom business exceptions")
            )
        )

        val result = repository.ingestMaterial(material)
        assertTrue(result.isSuccess)

        val topics = repository.getTopics("mat_java")
        assertTrue(topics.isNotEmpty())

        val stats = repository.getStats("mat_java")
        assertNotNull(stats)
        assertEquals(3, stats?.totalChunks)
        assertTrue(stats?.totalTopics ?: 0 >= 2)
    }

    @Test
    fun testDeterministicKeywordSearch_andRelevanceRanking() = runTest {
        val repository = LocalKnowledgeBaseRepository()
        val material = LearningMaterial(
            id = "mat_python",
            name = "Python Guide",
            pageCount = 3,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                PdfContentUnit("py1", "mat_python", "Python Guide", 1, "Variables in Python store reference values. A variable is dynamically typed."),
                PdfContentUnit("py2", "mat_python", "Python Guide", 2, "Control Flow: If statements and for loops in Python."),
                PdfContentUnit("py3", "mat_python", "Python Guide", 3, "Functions and Lambda expressions: Passing arguments to functions.")
            )
        )

        repository.ingestMaterial(material)

        // Search for "variable"
        val results = repository.searchChunks("mat_python", "variable")
        assertEquals(1, results.size)
        assertEquals(1, results[0].pageNumber)
        assertEquals("Python Guide • Page 1", results[0].sourceCitation)
        assertTrue(results[0].text.contains("Variables in Python"))

        // Search for "Python" should return all 3 sorted by term frequency
        val pythonResults = repository.searchChunks("mat_python", "python")
        assertEquals(3, pythonResults.size)
    }

    @Test
    fun testViewModel_knowledgeBaseExplorationFlow() = runTest {
        val kbRepo = LocalKnowledgeBaseRepository()
        val viewModel = LearningCompanionViewModel(knowledgeBaseRepository = kbRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val material = LearningMaterial(
            id = "mat_ml",
            name = "Machine Learning",
            pageCount = 2,
            status = LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                PdfContentUnit("m1", "mat_ml", "Machine Learning", 1, "Chapter 1: Supervised Learning\nLinear regression and classification"),
                PdfContentUnit("m2", "mat_ml", "Machine Learning", 2, "Chapter 2: Unsupervised Learning\nK-Means clustering and PCA")
            )
        )

        viewModel.buildKnowledgeBase(material)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.infoMessage)
        assertTrue(state.knowledgeBaseStatsMap.containsKey("mat_ml"))

        // Open Knowledge Base
        viewModel.openKnowledgeBase(material)
        testDispatcher.scheduler.advanceUntilIdle()

        val kbState = viewModel.uiState.value
        assertTrue(kbState.isKnowledgeBaseViewerVisible)
        assertEquals(2, kbState.kbChunks.size)

        // Test search in ViewModel
        viewModel.searchKnowledgeBase("clustering")
        testDispatcher.scheduler.advanceUntilIdle()

        val searchState = viewModel.uiState.value
        assertEquals(1, searchState.searchResults.size)
        assertEquals(2, searchState.searchResults[0].pageNumber)

        // Open Source Preview
        viewModel.openSourcePreview(searchState.searchResults[0])
        val previewState = viewModel.uiState.value
        assertTrue(previewState.isSourcePreviewVisible)
        assertEquals(2, previewState.selectedSourcePreviewChunk?.pageNumber)
        assertEquals("Machine Learning • Page 2", previewState.selectedSourcePreviewChunk?.sourceCitation)

        viewModel.dismissSourcePreview()
        assertFalse(viewModel.uiState.value.isSourcePreviewVisible)

        viewModel.closeKnowledgeBase()
        assertFalse(viewModel.uiState.value.isKnowledgeBaseViewerVisible)
    }
}
