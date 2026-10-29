package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.pdf.PdfExtractor
import com.example.data.pdf.PdfExtractorImpl
import com.example.data.repository.LocalLearningMaterialRepository
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.PdfContentUnit
import com.example.viewmodel.LearningCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.InputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfIngestionUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        PdfExtractorImpl.ensureInitialized(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test mock extractor simulating a 3-page textbook PDF with page-level text extraction.
     */
    private fun createStubPdfExtractor(
        pageCount: Int = 3,
        pageTexts: List<String> = listOf(
            "Page 1: Introduction to Data Structures and Algorithms",
            "Page 2: Binary Search Trees and Balancing",
            "Page 3: Graph Traversal Algorithms"
        )
    ): PdfExtractor {
        return object : PdfExtractor {
            override suspend fun extractFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial> {
                val docName = rawFileName?.substringBeforeLast(".") ?: "Textbook"
                val units = (1..pageCount).map { page ->
                    PdfContentUnit(
                        id = "stub_${docName}_p$page",
                        materialId = "stub_$docName",
                        documentName = docName,
                        pageNumber = page,
                        text = pageTexts.getOrElse(page - 1) { "" }
                    )
                }
                return Result.success(
                    LearningMaterial(
                        id = "stub_$docName",
                        name = docName,
                        type = LearningMaterialType.TEXTBOOK_PDF,
                        pageCount = pageCount,
                        status = LearningMaterialStatus.PROCESSED,
                        createdAt = System.currentTimeMillis(),
                        sourceFileName = rawFileName ?: "$docName.pdf",
                        contentUnits = units
                    )
                )
            }

            override suspend fun extractFromStream(
                inputStream: InputStream,
                fileName: String,
                customId: String?
            ): Result<LearningMaterial> {
                val id = customId ?: "mat_stub"
                val docName = fileName.substringBeforeLast(".")
                val units = (1..pageCount).map { page ->
                    PdfContentUnit(
                        id = "${id}_p$page",
                        materialId = id,
                        documentName = docName,
                        pageNumber = page,
                        text = pageTexts.getOrElse(page - 1) { "" }
                    )
                }
                return Result.success(
                    LearningMaterial(
                        id = id,
                        name = docName,
                        type = LearningMaterialType.TEXTBOOK_PDF,
                        pageCount = pageCount,
                        status = LearningMaterialStatus.PROCESSED,
                        createdAt = System.currentTimeMillis(),
                        sourceFileName = fileName,
                        contentUnits = units
                    )
                )
            }
        }
    }

    @Test
    fun testMultiplePageExtraction_pageNumbersAndTextPreserved() = runTest {
        val sampleTexts = listOf(
            "Page 1: Introduction to Neural Networks and Deep Learning",
            "Page 2: Backpropagation and Gradient Descent Optimization",
            "Page 3: Convolutional Architectures for Computer Vision"
        )
        val extractor = createStubPdfExtractor(pageCount = 3, pageTexts = sampleTexts)

        val result = extractor.extractFromStream(ByteArrayInputStream(byteArrayOf(1)), "Deep_Learning_Notes.pdf")
        assertTrue("Extraction should succeed", result.isSuccess)

        val material = result.getOrThrow()
        assertEquals("Deep_Learning_Notes", material.name)
        assertEquals(3, material.pageCount)
        assertEquals(LearningMaterialStatus.PROCESSED, material.status)
        assertEquals(3, material.contentUnits.size)

        // Verify Page Number Preservation (1-indexed)
        for (i in 0 until 3) {
            val unit = material.contentUnits[i]
            assertEquals(i + 1, unit.pageNumber)
            assertEquals(material.id, unit.materialId)
            assertTrue(
                "Page ${i + 1} should contain expected snippet",
                unit.text.contains("Page ${i + 1}")
            )
        }
    }

    @Test
    fun testEmptyPageContentHandling() = runTest {
        // Page 1 has text, Page 2 is empty
        val sampleTexts = listOf("Header text on page 1", "")
        val extractor = createStubPdfExtractor(pageCount = 2, pageTexts = sampleTexts)

        val result = extractor.extractFromStream(ByteArrayInputStream(byteArrayOf(1)), "EmptyPageTest.pdf")
        assertTrue(result.isSuccess)

        val material = result.getOrThrow()
        assertEquals(2, material.pageCount)
        assertEquals(2, material.contentUnits.size)

        // Page 2 should exist with pageNumber = 2 and blank text fallback
        val page2 = material.contentUnits[1]
        assertEquals(2, page2.pageNumber)
        assertEquals("", page2.text)
        assertEquals("[No extractable text on this page]", page2.previewText)
    }

    @Test
    fun testRepository_ingestPdfFromStream_successState() = runTest {
        val extractor = createStubPdfExtractor(
            pageCount = 1,
            pageTexts = listOf("Chapter 1: Basics of Operating Systems")
        )
        val repository = LocalLearningMaterialRepository(extractor)

        val result = repository.ingestPdfFromStream(ByteArrayInputStream(byteArrayOf(1)), "OS_Textbook.pdf")
        assertTrue(result.isSuccess)

        val materials = repository.getMaterialsFlow().first()
        assertEquals(1, materials.size)
        assertEquals(LearningMaterialStatus.PROCESSED, materials[0].status)
        assertEquals(1, materials[0].pageCount)
        assertEquals("OS_Textbook", materials[0].name)
    }

    @Test
    fun testRepository_ingestPdfFromStream_failureState() = runTest {
        val failingExtractor = object : PdfExtractor {
            override suspend fun extractFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial> =
                Result.failure(IllegalStateException("Corrupt PDF file"))

            override suspend fun extractFromStream(
                inputStream: InputStream,
                fileName: String,
                customId: String?
            ): Result<LearningMaterial> =
                Result.failure(IllegalStateException("Could not process this PDF. Please try another file."))
        }

        val repository = LocalLearningMaterialRepository(failingExtractor)
        val result = repository.ingestPdfFromStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), "broken.pdf")
        assertTrue("Repository should return failure", result.isFailure)

        val materials = repository.getMaterialsFlow().first()
        assertEquals(1, materials.size)
        assertEquals(LearningMaterialStatus.FAILED, materials[0].status)
        assertEquals("Could not process this PDF. Please try another file.", materials[0].errorMessage)
    }

    @Test
    fun testViewModel_ingestionFlowUpdatesUiState() = runTest {
        val sampleTexts = listOf("Page 1: Data Structures", "Page 2: Algorithms")
        val extractor = createStubPdfExtractor(pageCount = 2, pageTexts = sampleTexts)
        val repository = LocalLearningMaterialRepository(extractor)
        val viewModel = LearningCompanionViewModel(materialRepository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.ingestPdfStream(ByteArrayInputStream(byteArrayOf(1)), "DSA_Guide.pdf")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessingPdf)
        assertNotNull(state.infoMessage)
        assertTrue(state.infoMessage!!.contains("Textbook processed successfully"))
        assertEquals(1, state.materials.size)
        assertEquals(2, state.materials[0].pageCount)

        // Select for document details preview
        viewModel.onMaterialSelected(state.materials[0])
        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.isDocumentDetailsVisible)
        assertEquals("DSA_Guide", updatedState.selectedMaterialForDetails?.name)
        assertEquals(2, updatedState.selectedMaterialForDetails?.contentUnits?.size)

        viewModel.dismissDocumentDetails()
        assertFalse(viewModel.uiState.value.isDocumentDetailsVisible)
    }

    @Test
    fun testViewModel_ingestionFailureUpdatesErrorState() = runTest {
        val failingExtractor = object : PdfExtractor {
            override suspend fun extractFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial> =
                Result.failure(IllegalStateException("Corrupt PDF file"))

            override suspend fun extractFromStream(
                inputStream: InputStream,
                fileName: String,
                customId: String?
            ): Result<LearningMaterial> =
                Result.failure(IllegalStateException("Parsing error"))
        }

        val repository = LocalLearningMaterialRepository(failingExtractor)
        val viewModel = LearningCompanionViewModel(materialRepository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.ingestPdfStream(ByteArrayInputStream(byteArrayOf(1)), "corrupt.pdf")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessingPdf)
        assertEquals("Could not process this PDF. Please try another file.", state.errorMessage)
        assertEquals(1, state.materials.size)
        assertEquals(LearningMaterialStatus.FAILED, state.materials[0].status)
    }
}
