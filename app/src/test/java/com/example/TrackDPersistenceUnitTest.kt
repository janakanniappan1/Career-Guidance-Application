package com.example

import com.example.data.pdf.PdfExtractor
import com.example.data.repository.LocalKnowledgeBaseRepository
import com.example.data.repository.LocalLearnerModelRepository
import com.example.data.repository.LocalLearningMaterialRepository
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.MaterialSourceType
import com.example.domain.model.PdfContentUnit
import com.example.domain.model.adaptive.AssessmentResult
import com.example.domain.model.adaptive.MasteryCategory
import com.example.domain.model.adaptive.QuestionResult
import com.example.domain.model.adaptive.TopicPerformance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.InputStream

/**
 * Verifies durable persistence of Track D Learning Materials, Knowledge Base Chunks,
 * and Learner Mastery across application restarts (disk-backed JSON serialization).
 */
class TrackDPersistenceUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val stubPdfExtractor = object : PdfExtractor {
        override suspend fun extractFromUri(uri: android.net.Uri, fileName: String?): Result<LearningMaterial> {
            return Result.failure(UnsupportedOperationException("Use addMaterialDirectly in unit test"))
        }

        override suspend fun extractFromStream(
            inputStream: InputStream,
            fileName: String,
            customId: String?
        ): Result<LearningMaterial> {
            return Result.failure(UnsupportedOperationException("Use addMaterialDirectly in unit test"))
        }
    }

    @Test
    fun testLearningMaterialPersistenceAcrossRestarts() = runBlocking {
        val dir = tempFolder.newFolder("materials_test")

        // Instance 1: Save material
        val repo1 = LocalLearningMaterialRepository(
            pdfExtractor = stubPdfExtractor,
            storageDir = dir
        )

        val unit = PdfContentUnit(
            id = "u1",
            materialId = "mat_persist_1",
            documentName = "Algorithms.pdf",
            pageNumber = 10,
            text = "Dijkstra's algorithm finds shortest paths from a source vertex in a weighted graph.",
            sourceType = MaterialSourceType.PDF
        )
        val material = LearningMaterial(
            id = "mat_persist_1",
            name = "Algorithms Textbook",
            type = LearningMaterialType.TEXTBOOK_PDF,
            pageCount = 1,
            status = LearningMaterialStatus.PROCESSED,
            sourceFileName = "Algorithms.pdf",
            contentUnits = listOf(unit),
            sourceType = MaterialSourceType.PDF
        )

        repo1.addMaterialDirectly(material)
        assertEquals(1, repo1.getMaterialsFlow().first().size)

        // Instance 2: Simulate app restart with same storageDir
        val repo2 = LocalLearningMaterialRepository(
            pdfExtractor = stubPdfExtractor,
            storageDir = dir
        )
        val restored = repo2.getMaterialsFlow().first()
        assertEquals(1, restored.size)
        assertEquals("mat_persist_1", restored.first().id)
        assertEquals("Algorithms Textbook", restored.first().name)
        assertEquals(1, restored.first().contentUnits.size)
        assertEquals(10, restored.first().contentUnits.first().pageNumber)
    }

    @Test
    fun testKnowledgeBasePersistenceAcrossRestarts() = runBlocking {
        val dir = tempFolder.newFolder("kb_test")

        val kbRepo1 = LocalKnowledgeBaseRepository(storageDir = dir)
        val unit = PdfContentUnit(
            id = "u_slide_1",
            materialId = "mat_pptx_99",
            documentName = "Networks.pptx",
            pageNumber = 4,
            slideTitle = "TCP Three-Way Handshake",
            text = "TCP establishes reliable connections using SYN, SYN-ACK, and ACK packets.",
            sourceType = MaterialSourceType.PPTX
        )
        val material = LearningMaterial(
            id = "mat_pptx_99",
            name = "Computer Networks Slides",
            type = LearningMaterialType.SLIDES,
            pageCount = 1,
            status = LearningMaterialStatus.PROCESSED,
            sourceFileName = "Networks.pptx",
            contentUnits = listOf(unit),
            sourceType = MaterialSourceType.PPTX
        )

        kbRepo1.ingestMaterial(material)
        assertEquals(1, kbRepo1.getChunks("mat_pptx_99").size)

        // Simulate app restart
        val kbRepo2 = LocalKnowledgeBaseRepository(storageDir = dir)
        val restoredChunks = kbRepo2.getChunks("mat_pptx_99")
        assertEquals(1, restoredChunks.size)
        assertEquals("Networks.pptx • Slide 4", restoredChunks.first().sourceCitation)

        val searchResults = kbRepo2.retrieveRelevantResults("mat_pptx_99", "SYN-ACK handshake")
        assertEquals(1, searchResults.size)
    }

    @Test
    fun testLearnerMasteryPersistenceAcrossRestarts() = runBlocking {
        val dir = tempFolder.newFolder("mastery_test")

        val learnerRepo1 = LocalLearnerModelRepository(storageDir = dir)
        val qResult = QuestionResult(
            questionId = "q1",
            questionText = "What packet starts a TCP handshake?",
            selectedAnswer = "SYN",
            correctAnswer = "SYN",
            correct = true,
            explanation = "SYN initiates the 3-way handshake",
            topic = "Transport Layer",
            concept = "TCP Handshake",
            documentName = "Networks.pptx",
            pageNumber = 4,
            sourceChunkId = "mat_pptx_99_p4_c0",
            sourceType = MaterialSourceType.PPTX
        )
        val assessmentResult = AssessmentResult(
            assessmentId = "ass_1",
            totalQuestions = 1,
            correctAnswers = 1,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("Transport Layer", 1, 1, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(qResult)
        )

        // Initial 50 -> (50 * 0.6) + (100 * 0.4) = 70 (Developing: 60..79)
        learnerRepo1.updateFromAssessment("mat_pptx_99", assessmentResult)
        val initialMastery = learnerRepo1.getMastery("mat_pptx_99", "Transport Layer")
        assertNotNull(initialMastery)
        assertEquals(70, initialMastery?.masteryScore)
        assertEquals(MasteryCategory.DEVELOPING, initialMastery?.category)

        // Simulate app restart
        val learnerRepo2 = LocalLearnerModelRepository(storageDir = dir)
        val restoredMastery = learnerRepo2.getMastery("mat_pptx_99", "Transport Layer")
        assertNotNull(restoredMastery)
        assertEquals(70, restoredMastery?.masteryScore)
        assertEquals(MasteryCategory.DEVELOPING, restoredMastery?.category)
        assertEquals(1, restoredMastery?.questionsAttempted)
        assertEquals(1, restoredMastery?.questionsCorrect)
    }

    @Test
    fun testMaterialDeletionPersistence() = runBlocking {
        val dir = tempFolder.newFolder("deletion_test")

        val matRepo = LocalLearningMaterialRepository(stubPdfExtractor, storageDir = dir)
        val kbRepo = LocalKnowledgeBaseRepository(storageDir = dir)

        val material = LearningMaterial(
            id = "mat_del_1",
            name = "ToDelete.pdf",
            type = LearningMaterialType.TEXTBOOK_PDF,
            pageCount = 1,
            status = LearningMaterialStatus.PROCESSED,
            sourceFileName = "ToDelete.pdf",
            contentUnits = listOf(
                PdfContentUnit("u_del", "mat_del_1", "ToDelete.pdf", 1, "Sample text to delete", MaterialSourceType.PDF)
            )
        )

        matRepo.addMaterialDirectly(material)
        kbRepo.ingestMaterial(material)

        assertEquals(1, matRepo.getMaterialsFlow().first().size)
        assertEquals(1, kbRepo.getChunks("mat_del_1").size)

        // Delete material and its knowledge chunks
        matRepo.deleteMaterial("mat_del_1")
        kbRepo.deleteMaterialKnowledge("mat_del_1")

        assertEquals(0, matRepo.getMaterialsFlow().first().size)
        assertEquals(0, kbRepo.getChunks("mat_del_1").size)

        // Restart repositories and verify empty state persisted
        val matRepo2 = LocalLearningMaterialRepository(stubPdfExtractor, storageDir = dir)
        val kbRepo2 = LocalKnowledgeBaseRepository(storageDir = dir)

        assertEquals(0, matRepo2.getMaterialsFlow().first().size)
        assertEquals(0, kbRepo2.getChunks("mat_del_1").size)
    }

    @Test
    fun testSelectiveMaterialDeletionPersistence_preservesOtherMaterials() = runBlocking {
        val dir = tempFolder.newFolder("selective_deletion_test")

        val matRepo1 = LocalLearningMaterialRepository(stubPdfExtractor, storageDir = dir)
        val kbRepo1 = LocalKnowledgeBaseRepository(storageDir = dir)
        val learnerRepo1 = LocalLearnerModelRepository(storageDir = dir)

        val matA = LearningMaterial(
            id = "mat_keep_A",
            name = "KeepMe.pdf",
            type = LearningMaterialType.TEXTBOOK_PDF,
            pageCount = 1,
            status = LearningMaterialStatus.PROCESSED,
            sourceFileName = "KeepMe.pdf",
            contentUnits = listOf(
                PdfContentUnit("u_keep", "mat_keep_A", "KeepMe.pdf", 1, "Valuable content to keep", MaterialSourceType.PDF)
            )
        )
        val matB = LearningMaterial(
            id = "mat_del_B",
            name = "RemoveMe.pdf",
            type = LearningMaterialType.TEXTBOOK_PDF,
            pageCount = 1,
            status = LearningMaterialStatus.PROCESSED,
            sourceFileName = "RemoveMe.pdf",
            contentUnits = listOf(
                PdfContentUnit("u_del", "mat_del_B", "RemoveMe.pdf", 1, "Temporary content to delete", MaterialSourceType.PDF)
            )
        )

        matRepo1.addMaterialDirectly(matA)
        matRepo1.addMaterialDirectly(matB)
        kbRepo1.ingestMaterial(matA)
        kbRepo1.ingestMaterial(matB)

        // Add mastery for both
        val dummyResultA = AssessmentResult(
            assessmentId = "a1",
            totalQuestions = 1,
            correctAnswers = 1,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("Kept Topic", 1, 1, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q1", "Q?", "A", "A", true, "E", "Kept Topic", "C", "KeepMe.pdf", 1, "mat_keep_A_p1_c0")
            )
        )
        val dummyResultB = AssessmentResult(
            assessmentId = "a2",
            totalQuestions = 1,
            correctAnswers = 1,
            scorePercentage = 100,
            topicPerformance = listOf(TopicPerformance("Deleted Topic", 1, 1, 100)),
            weakTopics = emptyList(),
            questionResults = listOf(
                QuestionResult("q2", "Q?", "B", "B", true, "E", "Deleted Topic", "C", "RemoveMe.pdf", 1, "mat_del_B_p1_c0")
            )
        )
        learnerRepo1.updateFromAssessment("mat_keep_A", dummyResultA)
        learnerRepo1.updateFromAssessment("mat_del_B", dummyResultB)

        assertEquals(2, matRepo1.getMaterialsFlow().first().size)
        assertEquals(1, kbRepo1.getChunks("mat_keep_A").size)
        assertEquals(1, kbRepo1.getChunks("mat_del_B").size)
        assertNotNull(learnerRepo1.getMastery("mat_keep_A", "Kept Topic"))
        assertNotNull(learnerRepo1.getMastery("mat_del_B", "Deleted Topic"))

        // Delete mat_del_B specifically
        matRepo1.deleteMaterial("mat_del_B")
        kbRepo1.deleteMaterialKnowledge("mat_del_B")
        learnerRepo1.deleteMaterialMastery("mat_del_B")

        // In-memory verification
        assertEquals(1, matRepo1.getMaterialsFlow().first().size)
        assertEquals("mat_keep_A", matRepo1.getMaterialsFlow().first().first().id)
        assertEquals(1, kbRepo1.getChunks("mat_keep_A").size)
        assertEquals(0, kbRepo1.getChunks("mat_del_B").size)
        assertNotNull(learnerRepo1.getMastery("mat_keep_A", "Kept Topic"))
        assertEquals(0, learnerRepo1.getAllMastery("mat_del_B").size)

        // Restart repositories and verify persistence on disk
        val matRepo2 = LocalLearningMaterialRepository(stubPdfExtractor, storageDir = dir)
        val kbRepo2 = LocalKnowledgeBaseRepository(storageDir = dir)
        val learnerRepo2 = LocalLearnerModelRepository(storageDir = dir)

        val restoredMats = matRepo2.getMaterialsFlow().first()
        assertEquals(1, restoredMats.size)
        assertEquals("mat_keep_A", restoredMats.first().id)
        assertEquals(1, kbRepo2.getChunks("mat_keep_A").size)
        assertEquals(0, kbRepo2.getChunks("mat_del_B").size)
        assertNotNull(learnerRepo2.getMastery("mat_keep_A", "Kept Topic"))
        assertEquals(0, learnerRepo2.getAllMastery("mat_del_B").size)
    }

    @Test
    fun testCorruptedJsonFileSafety() = runBlocking {
        val dir = tempFolder.newFolder("corrupt_test")

        // Write invalid JSON to persistence files
        java.io.File(dir, "cga_learning_materials.json").writeText("{ corrupt_invalid_json: [ }")
        java.io.File(dir, "cga_knowledge_base.json").writeText("TRUNCATED FILE CONTENT")
        java.io.File(dir, "cga_learner_mastery.json").writeText("???")

        // Repositories must handle corruption gracefully without crashing or throwing
        val matRepo = LocalLearningMaterialRepository(stubPdfExtractor, storageDir = dir)
        val kbRepo = LocalKnowledgeBaseRepository(storageDir = dir)
        val learnerRepo = LocalLearnerModelRepository(storageDir = dir)

        assertEquals(0, matRepo.getMaterialsFlow().first().size)
        assertEquals(0, kbRepo.getAllChunks().size)
        assertEquals(0, learnerRepo.getAllMastery("mat_any").size)
    }

    @Test
    fun testDuplicateChunkPreventionOnRestoration() = runBlocking {
        val dir = tempFolder.newFolder("duplicate_chunk_test")

        val kbRepo = LocalKnowledgeBaseRepository(storageDir = dir)
        val unit = PdfContentUnit("u1", "mat_dup_1", "Guide.pdf", 1, "Content unit for duplicate check", MaterialSourceType.PDF)
        val material = LearningMaterial(
            id = "mat_dup_1",
            name = "Guide.pdf",
            type = LearningMaterialType.TEXTBOOK_PDF,
            pageCount = 1,
            status = LearningMaterialStatus.PROCESSED,
            sourceFileName = "Guide.pdf",
            contentUnits = listOf(unit)
        )

        kbRepo.ingestMaterial(material)
        assertEquals(1, kbRepo.getChunks("mat_dup_1").size)

        // Re-ingest same material
        kbRepo.ingestMaterial(material)
        // Map keys overwrite existing material ID, ensuring 0 duplicates
        assertEquals(1, kbRepo.getChunks("mat_dup_1").size)
    }
}
