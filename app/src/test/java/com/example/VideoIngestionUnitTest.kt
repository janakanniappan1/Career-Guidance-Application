package com.example

import com.example.data.video.VideoExtractorImpl
import com.example.domain.knowledge.KnowledgeChunker
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.MaterialSourceType
import com.example.domain.model.PdfContentUnit
import com.example.domain.model.TutorSource
import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentQuestion
import com.example.domain.model.adaptive.QuestionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class VideoIngestionUnitTest {

    @Test
    fun testVideoMaterialMetadataAndType() {
        val material = LearningMaterial(
            id = "mat_video_1",
            name = "Java Concurrency Lecture",
            type = LearningMaterialType.LECTURE_VIDEO,
            pageCount = 4,
            sourceFileName = "Java_Concurrency.mp4",
            sourceType = MaterialSourceType.VIDEO
        )

        assertEquals("mat_video_1", material.id)
        assertEquals(LearningMaterialType.LECTURE_VIDEO, material.type)
        assertEquals(MaterialSourceType.VIDEO, material.sourceType)
        assertEquals("🎥", material.type.emoji)
        assertEquals("Lecture Video", material.type.displayName)
    }

    @Test
    fun testTimestampFormattingAndUnitLabels() {
        val unit = PdfContentUnit(
            id = "unit_v1",
            materialId = "mat_v1",
            documentName = "Operating Systems Lecture",
            pageNumber = 1,
            text = "Discussion of process scheduling and threads.",
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 272000L, // 04:32
            endTimeMs = 310000L   // 05:10
        )

        assertEquals("04:32–05:10", unit.unitLabel)

        val tutorSource = TutorSource(
            materialId = "mat_v1",
            documentName = "Operating Systems Lecture.mp4",
            pageNumber = 1,
            chunkId = "chunk_v1",
            previewText = "Thread context switching",
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 272000L,
            endTimeMs = 310000L
        )

        assertEquals("04:32–05:10", tutorSource.unitLabel)
        assertEquals("Operating Systems Lecture.mp4 • 04:32–05:10", tutorSource.displayCitation)
    }

    @Test
    fun testVideoChunkingAndTimestampPreservation() {
        val chunker: KnowledgeChunker = com.example.domain.knowledge.PageAwareKnowledgeChunker()

        val videoUnit = PdfContentUnit(
            id = "mat_v1_seg_1",
            materialId = "mat_v1",
            documentName = "Database Indexing",
            pageNumber = 1,
            text = "B-Tree index structure enables logarithmic lookup time for relational queries.",
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 120000L, // 02:00
            endTimeMs = 150000L   // 02:30
        )

        val chunks = chunker.chunkUnits(listOf(videoUnit))

        assertEquals(1, chunks.size)
        val chunk = chunks.first()
        assertEquals(MaterialSourceType.VIDEO, chunk.sourceType)
        assertEquals(120000L, chunk.startTimeMs)
        assertEquals(150000L, chunk.endTimeMs)
        assertEquals("02:00–02:30", chunk.unitLabel)
        assertEquals("Database Indexing • 02:00–02:30", chunk.sourceCitation)
    }

    @Test
    fun testVideoExtractorStreamWithImportedTranscript() = runBlocking {
        val extractor = VideoExtractorImpl(context = null)
        val mockStream = ByteArrayInputStream("dummy video stream".toByteArray())
        val sampleTranscript = """
            Welcome to the lecture on Distributed System Consensus and Raft.
            
            Raft decomposes consensus into leader election, log replication, and safety.
        """.trimIndent()

        val result = extractor.extractFromStream(
            inputStream = mockStream,
            fileName = "Distributed_Systems_Lecture.mp4",
            customId = "mat_ds_1",
            importedTranscriptText = sampleTranscript
        )

        assertTrue(result.isSuccess)
        val material = result.getOrThrow()
        assertEquals("mat_ds_1", material.id)
        assertEquals("Distributed Systems Lecture", material.name)
        assertEquals(MaterialSourceType.VIDEO, material.sourceType)
        assertEquals(2, material.contentUnits.size)

        val firstUnit = material.contentUnits.first()
        assertEquals(MaterialSourceType.VIDEO, firstUnit.sourceType)
        assertTrue(firstUnit.text.contains("Distributed System Consensus"))
    }

    @Test
    fun testEmptyTranscriptVideoIngestionFailsWithoutFabrication() = runBlocking {
        val extractor = VideoExtractorImpl(context = null)
        val mockStream = ByteArrayInputStream("dummy video stream".toByteArray())

        // Without speech-to-text output or imported transcript, video ingestion MUST fail rather than fabricating fake text
        val result = extractor.extractFromStream(
            inputStream = mockStream,
            fileName = "Silent_Lecture.mp4",
            customId = "mat_silent_1",
            importedTranscriptText = null
        )

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("No speech transcript recognized") == true)
    }

    @Test
    fun testAssessmentQuestionVideoSourceAttribution() {
        val question = AssessmentQuestion(
            id = "q_v1",
            question = "What is the primary advantage of B-Tree indexing?",
            correctAnswer = "Logarithmic lookup time",
            explanation = "As discussed in the lecture video segment, B-Trees maintain balanced height.",
            topic = "Database Indexing",
            materialId = "mat_v1",
            documentName = "Database_Lecture.mp4",
            pageNumber = 1,
            sourceChunkId = "chunk_v1",
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 120000L,
            endTimeMs = 150000L
        )

        assertEquals("02:00–02:30", question.unitLabel)
        assertEquals("Database_Lecture.mp4 • 02:00–02:30", question.sourceCitation)

        val tutorSource = question.toTutorSource
        assertEquals(MaterialSourceType.VIDEO, tutorSource.sourceType)
        assertEquals("Database_Lecture.mp4 • 02:00–02:30", tutorSource.displayCitation)

        val result = QuestionResult(
            questionId = question.id,
            questionText = question.question,
            selectedAnswer = "Logarithmic lookup time",
            correctAnswer = question.correctAnswer,
            correct = true,
            explanation = question.explanation,
            topic = question.topic,
            concept = "B-Trees",
            documentName = question.documentName,
            pageNumber = question.pageNumber,
            sourceChunkId = question.sourceChunkId,
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 120000L,
            endTimeMs = 150000L
        )

        assertEquals("02:00–02:30", result.unitLabel)
        assertEquals("Database_Lecture.mp4 • 02:00–02:30", result.sourceCitation)
    }

    @Test
    fun testOnlyActualRecognizedTextIsIndexedIntoKnowledgeBase() {
        val chunker = com.example.domain.knowledge.PageAwareKnowledgeChunker()

        // 1. Verify empty content unit is not created or chunked into searchable content
        val emptyUnit = PdfContentUnit(
            id = "unit_empty",
            materialId = "mat_v2",
            documentName = "Empty_Lecture.mp4",
            pageNumber = 1,
            text = "   ",
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 0L,
            endTimeMs = 30000L
        )

        val chunks = chunker.chunkUnits(listOf(emptyUnit))
        assertTrue("Empty units must not produce indexed knowledge chunks", chunks.isEmpty())

        // 2. Real transcript unit produces indexed chunks with accurate source IDs and timestamps
        val validUnit = PdfContentUnit(
            id = "unit_valid_1",
            materialId = "mat_v2",
            documentName = "Operating_Systems.mp4",
            pageNumber = 1,
            text = "A mutex ensures mutual exclusion by allowing only one thread to enter the critical section at any time.",
            sourceType = MaterialSourceType.VIDEO,
            startTimeMs = 45000L,
            endTimeMs = 75000L
        )

        val validChunks = chunker.chunkUnits(listOf(validUnit))
        assertEquals(1, validChunks.size)
        val validChunk = validChunks.first()
        assertEquals("mat_v2_p1_c0", validChunk.id)
        assertEquals("Operating_Systems.mp4 • 00:45–01:15", validChunk.sourceCitation)
        assertTrue(validChunk.text.contains("mutual exclusion"))
    }

    @Test
    fun testImportedVttTranscriptParsing() = runBlocking {
        val extractor = VideoExtractorImpl(context = null)
        val mockStream = ByteArrayInputStream("dummy stream".toByteArray())
        val vttContent = """
            WEBVTT
            
            00:00.000 --> 00:15.000
            Introduction to Machine Learning algorithms and model evaluation.
            
            00:15.000 --> 00:30.000
            Supervised learning relies on labeled training pairs of inputs and ground truth targets.
        """.trimIndent()

        val result = extractor.extractFromStream(
            inputStream = mockStream,
            fileName = "ML_Lecture.mp4",
            customId = "mat_ml_1",
            importedTranscriptText = vttContent
        )

        assertTrue(result.isSuccess)
        val material = result.getOrThrow()
        assertEquals(2, material.contentUnits.size)

        val unit1 = material.contentUnits[0]
        assertEquals("Introduction to Machine Learning algorithms and model evaluation.", unit1.text)

        val unit2 = material.contentUnits[1]
        assertEquals("Supervised learning relies on labeled training pairs of inputs and ground truth targets.", unit2.text)
    }
}
