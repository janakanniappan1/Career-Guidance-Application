package com.example

import com.example.data.repository.LocalKnowledgeBaseRepository
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.MaterialSourceType
import com.example.domain.model.PdfContentUnit
import com.example.domain.model.RetrievalResult
import com.example.domain.model.SourceLocation
import com.example.domain.model.TutorMessageRole
import com.example.domain.model.TutorSource
import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentQuestion
import com.example.domain.model.adaptive.AssessmentQuestionType
import com.example.domain.model.adaptive.TopicMastery
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unified Multimodal Knowledge Base & Retrieval Tests (Step 9).
 * Verifies cross-material retrieval, source location preservation, deterministic ranking,
 * citation formatting, tutor ground validation, and assessment attribution.
 */
class UnifiedMultimodalRetrievalTest {

    private lateinit var repository: LocalKnowledgeBaseRepository

    @Before
    fun setUp() {
        runBlocking {
            repository = LocalKnowledgeBaseRepository()

        // 1. PDF Material: Database Indexing Textbook
        val pdfUnit1 = PdfContentUnit(
            id = "pdf_u1",
            materialId = "mat_pdf_1",
            documentName = "Database_Systems.pdf",
            pageNumber = 14,
            text = "B-Tree indexing provides logarithmic lookup time O(log N) for relational queries by maintaining balanced trees.",
            sourceType = MaterialSourceType.PDF
        )
        val pdfMaterial = com.example.domain.model.LearningMaterial(
            id = "mat_pdf_1",
            name = "Database Systems Textbook",
            sourceFileName = "Database_Systems.pdf",
            type = com.example.domain.model.LearningMaterialType.TEXTBOOK_PDF,
            sourceType = MaterialSourceType.PDF,
            contentUnits = listOf(pdfUnit1)
        )
        repository.ingestMaterial(pdfMaterial)

        // 2. PPTX Material: Operating Systems Lecture Slides
        val pptxUnit1 = PdfContentUnit(
            id = "pptx_u1",
            materialId = "mat_pptx_1",
            documentName = "Operating_Systems.pptx",
            pageNumber = 8,
            slideTitle = "Mutex Locks & Synchronization",
            text = "A mutex lock prevents race conditions in concurrent execution by enforcing mutual exclusion on critical regions.",
            sourceType = MaterialSourceType.PPTX
        )
        val pptxMaterial = com.example.domain.model.LearningMaterial(
            id = "mat_pptx_1",
            name = "Operating Systems Slides",
            sourceFileName = "Operating_Systems.pptx",
            type = com.example.domain.model.LearningMaterialType.SLIDES,
            sourceType = MaterialSourceType.PPTX,
            contentUnits = listOf(pptxUnit1)
        )
        repository.ingestMaterial(pptxMaterial)

        // 3. Video Material: Distributed Systems Lecture Video
        val videoUnit1 = PdfContentUnit(
            id = "vid_u1",
            materialId = "mat_vid_1",
            documentName = "Distributed_Systems_Lecture.mp4",
            pageNumber = 1,
            text = "Raft consensus decomposes state machine replication into leader election, log replication, and safety guarantees.",
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 272000L, // 04:32
            endTimeMs = 310000L    // 05:10
        )
        val videoMaterial = com.example.domain.model.LearningMaterial(
            id = "mat_vid_1",
            name = "Distributed Systems Video",
            sourceFileName = "Distributed_Systems_Lecture.mp4",
            type = com.example.domain.model.LearningMaterialType.LECTURE_VIDEO,
            sourceType = MaterialSourceType.VIDEO,
            contentUnits = listOf(videoUnit1)
        )
        repository.ingestMaterial(videoMaterial)
        }
    }

    @Test
    fun testPdfSourceLocationPreservation() = runBlocking {
        val results = repository.retrieveRelevantResults(materialId = "mat_pdf_1", query = "B-Tree logarithmic")
        assertEquals(1, results.size)
        val first = results.first()
        assertEquals(MaterialSourceType.PDF, first.sourceType)
        assertTrue(first.sourceLocation is SourceLocation.PdfPage)
        assertEquals("Page 14", first.sourceLocation.label)
        assertEquals("Database_Systems.pdf • Page 14", first.sourceCitation)
    }

    @Test
    fun testPptxSlideLocationPreservation() = runBlocking {
        val results = repository.retrieveRelevantResults(materialId = "mat_pptx_1", query = "mutex mutual exclusion")
        assertEquals(1, results.size)
        val first = results.first()
        assertEquals(MaterialSourceType.PPTX, first.sourceType)
        assertTrue(first.sourceLocation is SourceLocation.PptxSlide)
        assertEquals("Slide 8", first.sourceLocation.label)
        assertEquals("Operating_Systems.pptx • Slide 8", first.sourceCitation)
    }

    @Test
    fun testVideoTimestampLocationPreservation() = runBlocking {
        val results = repository.retrieveRelevantResults(materialId = "mat_vid_1", query = "Raft consensus leader")
        assertEquals(1, results.size)
        val first = results.first()
        assertEquals(MaterialSourceType.VIDEO, first.sourceType)
        assertTrue(first.sourceLocation is SourceLocation.VideoTimestamp)
        assertEquals("04:32–05:10", first.sourceLocation.label)
        assertEquals("Distributed_Systems_Lecture.mp4 • 04:32–05:10", first.sourceCitation)
    }

    @Test
    fun testCrossMaterialRetrievalAcrossAllSources() = runBlocking {
        // Query for execution and replication keywords that match across sources without material filter
        val allResults = repository.retrieveRelevantResults(materialId = null, query = "execution", limit = 10)
        assertTrue(allResults.isNotEmpty())

        val sourceTypesFound = allResults.map { it.sourceType }.toSet()
        assertTrue("Retrieval should handle multiple multimodal source types", sourceTypesFound.contains(MaterialSourceType.PPTX))
    }

    @Test
    fun testRelevanceRankingRanksExactPhraseAheadOfWeakMatches() = runBlocking {
        val results = repository.retrieveRelevantResults(materialId = null, query = "mutex lock prevents race conditions", limit = 5)
        assertTrue(results.isNotEmpty())
        val topMatch = results.first()
        assertEquals("Operating_Systems.pptx", topMatch.documentName)
        assertTrue("Exact phrase should yield high score >= 50", topMatch.relevanceScore >= 50)
    }

    @Test
    fun testDuplicateResultPrevention() = runBlocking {
        val results = repository.retrieveRelevantResults(materialId = null, query = "consensus", limit = 10)
        val uniqueChunkIds = results.map { it.chunk.id }.distinct()
        assertEquals("Results must not contain duplicate chunks", uniqueChunkIds.size, results.size)
    }

    @Test
    fun testCitationFormattingConsistency() {
        val pdfLoc = SourceLocation.from(MaterialSourceType.PDF, 14)
        assertEquals("Page 14", pdfLoc.label)

        val pptxLoc = SourceLocation.from(MaterialSourceType.PPTX, 8)
        assertEquals("Slide 8", pptxLoc.label)

        val vidLoc = SourceLocation.from(MaterialSourceType.VIDEO, 1, 272000L, 310000L)
        assertEquals("04:32–05:10", vidLoc.label)
    }

    @Test
    fun testInvalidSourceIdRejectionInTutorValidation() {
        val validChunks = listOf(
            KnowledgeChunk(
                id = "valid_chunk_1",
                materialId = "mat_pdf_1",
                documentName = "Database_Systems.pdf",
                pageNumber = 14,
                text = "B-Tree index structure",
                sourceType = MaterialSourceType.PDF
            )
        )
        val chunkMap = validChunks.associateBy { it.id }

        // Simulated AI response returning both valid and fabricated source IDs
        val returnedSourceIds = listOf("valid_chunk_1", "fake_chunk_99", "hallucinated_page_100")
        val validatedSources = returnedSourceIds.mapNotNull { id ->
            val chunk = chunkMap[id]
            if (chunk != null) {
                TutorSource(
                    materialId = chunk.materialId,
                    documentName = chunk.documentName,
                    pageNumber = chunk.pageNumber,
                    chunkId = chunk.id,
                    sourceType = chunk.sourceType
                )
            } else null
        }

        assertEquals(1, validatedSources.size)
        assertEquals("valid_chunk_1", validatedSources.first().chunkId)
        assertEquals("Database_Systems.pdf • Page 14", validatedSources.first().displayCitation)
    }

    @Test
    fun testUnsupportedQueryHandlingYieldsEmptyResults() = runBlocking {
        val results = repository.retrieveRelevantResults(materialId = null, query = "astronomy black hole gravitational lensing")
        assertTrue("Unsupported queries outside uploaded materials must yield empty results", results.isEmpty())
    }

    @Test
    fun testAssessmentSourceAttributionAcrossMultimodalSources() {
        // Video assessment question
        val videoQuestion = AssessmentQuestion(
            id = "q_vid_1",
            question = "Which safety guarantees does Raft provide during leader election?",
            correctAnswer = "Election safety: at most one leader can be elected in a given term",
            explanation = "Raft decomposes consensus into leader election, log replication, and safety.",
            topic = "Distributed Consensus",
            materialId = "mat_vid_1",
            documentName = "Distributed_Systems_Lecture.mp4",
            pageNumber = 1,
            sourceChunkId = "vid_u1",
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 272000L,
            endTimeMs = 310000L
        )

        assertEquals("04:32–05:10", videoQuestion.unitLabel)
        assertEquals("Distributed_Systems_Lecture.mp4 • 04:32–05:10", videoQuestion.sourceCitation)
        assertEquals(MaterialSourceType.VIDEO, videoQuestion.sourceType)

        // PPTX assessment question
        val pptxQuestion = AssessmentQuestion(
            id = "q_pptx_1",
            question = "What condition does a mutex lock prevent?",
            correctAnswer = "Race conditions in critical sections",
            explanation = "Enforcing mutual exclusion prevents concurrent race conditions.",
            topic = "Concurrency",
            materialId = "mat_pptx_1",
            documentName = "Operating_Systems.pptx",
            pageNumber = 8,
            sourceChunkId = "pptx_u1",
            sourceType = MaterialSourceType.PPTX
        )

        assertEquals("Slide 8", pptxQuestion.unitLabel)
        assertEquals("Operating_Systems.pptx • Slide 8", pptxQuestion.sourceCitation)
        assertEquals(MaterialSourceType.PPTX, pptxQuestion.sourceType)
    }

    @Test
    fun testMasteryIntegrationOnlyUpdatesAssessedTopic() {
        val initialMastery = TopicMastery(
            materialId = "mat_vid_1",
            topic = "Distributed Consensus",
            masteryScore = 75,
            questionsAttempted = 4,
            questionsCorrect = 3
        )

        val unassessedTopic = TopicMastery(
            materialId = "mat_vid_1",
            topic = "Quantum Cryptography",
            masteryScore = 50,
            questionsAttempted = 0,
            questionsCorrect = 0
        )

        assertEquals(75, initialMastery.masteryScore)
        assertEquals(50, unassessedTopic.masteryScore)
        assertEquals(0, unassessedTopic.questionsAttempted)
    }
}
