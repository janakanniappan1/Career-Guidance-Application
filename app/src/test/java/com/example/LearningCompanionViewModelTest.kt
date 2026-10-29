package com.example

import com.example.domain.model.LearningCompanionFeatureType
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.StudentProfile
import com.example.domain.repository.AuthRepository
import com.example.viewmodel.LearningCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
class LearningCompanionViewModelTest {

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
    fun testInitialState_masteryIsZeroAndMaterialsEmpty() = runTest {
        val viewModel = LearningCompanionViewModel(authRepository = null)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.overallMastery)
        assertEquals(0, state.totalMaterialsCount)
        assertTrue(state.materials.isEmpty())
        assertFalse(state.isAddMaterialDialogVisible)
        assertEquals(3, state.availableMaterialTypes.size)
        assertEquals(4, state.availableFeatures.size)
    }

    @Test
    fun testOpenAndDismissAddMaterialDialog() = runTest {
        val viewModel = LearningCompanionViewModel(authRepository = null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openAddMaterialDialog(LearningMaterialType.SLIDES)
        assertEquals(true, viewModel.uiState.value.isAddMaterialDialogVisible)
        assertEquals(LearningMaterialType.SLIDES, viewModel.uiState.value.selectedMaterialType)

        viewModel.selectMaterialType(LearningMaterialType.LECTURE_VIDEO)
        assertEquals(LearningMaterialType.LECTURE_VIDEO, viewModel.uiState.value.selectedMaterialType)

        viewModel.dismissAddMaterialDialog()
        assertFalse(viewModel.uiState.value.isAddMaterialDialogVisible)
    }

    @Test
    fun testFeatureCardClicked_opensTutorOrShowsMessage() = runTest {
        val viewModel = LearningCompanionViewModel(authRepository = null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFeatureCardClicked(LearningCompanionFeatureType.SOURCE_GROUNDED_TUTOR)
        assertTrue(viewModel.uiState.value.isTutorScreenVisible)

        viewModel.closeTutorScreen()
        assertFalse(viewModel.uiState.value.isTutorScreenVisible)

        viewModel.onFeatureCardClicked(LearningCompanionFeatureType.ADAPTIVE_ASSESSMENT)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Build a Knowledge Base before creating an assessment"))
        viewModel.clearErrorMessage()

        viewModel.onFeatureCardClicked(LearningCompanionFeatureType.MY_MASTERY)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isMasteryScreenVisible)

        viewModel.closeMasteryScreen()
        assertFalse(viewModel.uiState.value.isMasteryScreenVisible)
    }

    @Test
    fun testUserContextLoading_updatesProfileGoal() = runTest {
        val mockAuthRepo = com.example.data.repository.MockAuthRepository()
        mockAuthRepo.updateProfile(
            StudentProfile(
                id = "user-123",
                name = "Alex Rivera",
                email = "alex@test.com",
                recommendedCareerId = "aiml_engineer"
            )
        )

        val viewModel = LearningCompanionViewModel(authRepository = mockAuthRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Alex Rivera", state.studentName)
        assertEquals("AI / Machine Learning Engineer", state.careerGoal)
    }

    @Test
    fun testDeleteConfirmationDialog_openAndDismissPreservesData() = runTest {
        val stubPdf = object : com.example.data.pdf.PdfExtractor {
            override suspend fun extractFromUri(uri: android.net.Uri, fileName: String?): Result<com.example.domain.model.LearningMaterial> = Result.failure(Exception())
            override suspend fun extractFromStream(inputStream: java.io.InputStream, fileName: String, customId: String?): Result<com.example.domain.model.LearningMaterial> = Result.failure(Exception())
        }
        val matRepo = com.example.data.repository.LocalLearningMaterialRepository(stubPdf)
        val material = com.example.domain.model.LearningMaterial(
            id = "mat_test_1",
            name = "Test Material.pdf",
            type = LearningMaterialType.TEXTBOOK_PDF,
            pageCount = 5,
            status = com.example.domain.model.LearningMaterialStatus.PROCESSED
        )
        matRepo.addMaterialDirectly(material)

        val viewModel = LearningCompanionViewModel(
            materialRepository = matRepo,
            knowledgeBaseRepository = com.example.data.repository.LocalKnowledgeBaseRepository()
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.materials.size)

        // Request delete
        viewModel.requestDeleteMaterial(material)
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)
        assertEquals("mat_test_1", viewModel.uiState.value.materialToDelete?.id)

        // Dismiss/Cancel dialog
        viewModel.dismissDeleteConfirmation()
        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
        assertNull(viewModel.uiState.value.materialToDelete)

        // Data must remain intact
        assertEquals(1, viewModel.uiState.value.materials.size)
        assertEquals(1, viewModel.uiState.value.totalMaterialsCount)
    }

    @Test
    fun testConfirmDeleteMaterial_removesSelectedAndPreservesUnrelated() = runTest {
        val stubPdf = object : com.example.data.pdf.PdfExtractor {
            override suspend fun extractFromUri(uri: android.net.Uri, fileName: String?): Result<com.example.domain.model.LearningMaterial> = Result.failure(Exception())
            override suspend fun extractFromStream(inputStream: java.io.InputStream, fileName: String, customId: String?): Result<com.example.domain.model.LearningMaterial> = Result.failure(Exception())
        }
        val matRepo = com.example.data.repository.LocalLearningMaterialRepository(stubPdf)
        val kbRepo = com.example.data.repository.LocalKnowledgeBaseRepository()
        val learnerRepo = com.example.data.repository.LocalLearnerModelRepository()

        val matA = com.example.domain.model.LearningMaterial(
            id = "mat_A",
            name = "Machine Learning Notes.pdf",
            type = LearningMaterialType.TEXTBOOK_PDF,
            pageCount = 10,
            status = com.example.domain.model.LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                com.example.domain.model.PdfContentUnit("u_a", "mat_A", "Machine Learning Notes.pdf", 1, "Supervised classification algorithms")
            )
        )
        val matB = com.example.domain.model.LearningMaterial(
            id = "mat_B",
            name = "Operating Systems.pdf",
            type = LearningMaterialType.TEXTBOOK_PDF,
            pageCount = 20,
            status = com.example.domain.model.LearningMaterialStatus.PROCESSED,
            contentUnits = listOf(
                com.example.domain.model.PdfContentUnit("u_b", "mat_B", "Operating Systems.pdf", 1, "Deadlock avoidance with Banker algorithm")
            )
        )

        matRepo.addMaterialDirectly(matA)
        matRepo.addMaterialDirectly(matB)
        kbRepo.ingestMaterial(matA)
        kbRepo.ingestMaterial(matB)

        val viewModel = LearningCompanionViewModel(
            materialRepository = matRepo,
            knowledgeBaseRepository = kbRepo,
            learnerModelRepository = learnerRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.materials.size)
        assertEquals(1, kbRepo.getChunks("mat_A").size)
        assertEquals(1, kbRepo.getChunks("mat_B").size)

        // Request and confirm deletion of mat_A
        viewModel.requestDeleteMaterial(matA)
        viewModel.confirmDeleteMaterial()
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. UI state updated: mat_A removed, mat_B remains
        val state = viewModel.uiState.value
        assertEquals(1, state.materials.size)
        assertEquals("mat_B", state.materials.first().id)
        assertEquals(1, state.totalMaterialsCount)
        assertFalse(state.isDeleteConfirmationVisible)
        assertNull(state.materialToDelete)
        assertFalse(state.isDeletingMaterial)
        assertTrue(state.infoMessage?.contains("deleted successfully") == true)

        // 2. KnowledgeBase updated: only mat_A chunks removed, mat_B preserved
        assertEquals(0, kbRepo.getChunks("mat_A").size)
        assertEquals(1, kbRepo.getChunks("mat_B").size)

        // 3. Retrieval verifies mat_A chunks cannot be retrieved
        val searchA = kbRepo.searchChunks(materialId = null, query = "Supervised")
        assertTrue("Deleted material chunks should not be retrievable", searchA.isEmpty())

        val searchB = kbRepo.searchChunks(materialId = null, query = "Banker")
        assertEquals(1, searchB.size)
        assertEquals("mat_B", searchB.first().materialId)
    }

    @Test
    fun testConfirmDeleteMaterial_handlesRepositoryFailureGracefully() = runTest {
        val failingRepo = object : com.example.domain.repository.LearningMaterialRepository {
            private val state = kotlinx.coroutines.flow.MutableStateFlow(
                listOf(
                    com.example.domain.model.LearningMaterial(
                        id = "mat_fail",
                        name = "Locked Material.pdf",
                        type = LearningMaterialType.TEXTBOOK_PDF,
                        pageCount = 2,
                        status = com.example.domain.model.LearningMaterialStatus.PROCESSED
                    )
                )
            )
            override fun getMaterialsFlow(): Flow<List<com.example.domain.model.LearningMaterial>> = state
            override suspend fun getMaterialById(id: String): com.example.domain.model.LearningMaterial? = state.value.firstOrNull { it.id == id }
            override suspend fun getContentUnitsForMaterial(materialId: String): List<com.example.domain.model.PdfContentUnit> = emptyList()
            override suspend fun ingestPdfFromUri(uri: android.net.Uri, rawFileName: String?): Result<com.example.domain.model.LearningMaterial> = Result.failure(Exception())
            override suspend fun ingestPdfFromStream(inputStream: java.io.InputStream, fileName: String): Result<com.example.domain.model.LearningMaterial> = Result.failure(Exception())
            override suspend fun deleteMaterial(id: String): Result<Unit> = Result.failure(IllegalStateException("Disk write protected"))
        }

        val viewModel = LearningCompanionViewModel(materialRepository = failingRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        val mat = viewModel.uiState.value.materials.first()
        viewModel.requestDeleteMaterial(mat)
        viewModel.confirmDeleteMaterial()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeletingMaterial)
        assertFalse(state.isDeleteConfirmationVisible)
        assertNull(state.materialToDelete)
        assertEquals("Disk write protected", state.errorMessage)
        assertEquals(1, state.materials.size)
    }
}
