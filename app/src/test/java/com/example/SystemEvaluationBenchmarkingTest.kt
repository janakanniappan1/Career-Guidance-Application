package com.example

import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentQuestion
import com.example.domain.model.adaptive.AssessmentQuestionType
import com.example.evaluation.EvaluationDataset
import com.example.evaluation.SystemBenchmarkRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * System Evaluation & Benchmarking Regression Test Suite (Step 10).
 * Verifies:
 * 1. Evaluation dataset integrity (>= 30 test cases across PDF, PPTX, Video).
 * 2. Precision@K calculation with known expected values.
 * 3. Recall@K calculation with known expected values.
 * 4. MRR calculation with known expected values.
 * 5. Citation source ID accuracy calculation.
 * 6. Unsupported-query refusal calculation.
 * 7. Assessment validation calculations.
 * 8. Duplicate question detection and repetition rate.
 * 9. Weighted Evidence Mastery Model update correctness.
 * 10. Reproducible evaluation report generation.
 */
class SystemEvaluationBenchmarkingTest {

    private lateinit var runner: SystemBenchmarkRunner

    @Before
    fun setUp() {
        runner = SystemBenchmarkRunner()
    }

    @Test
    fun testEvaluationDatasetIntegrity() {
        val testCases = EvaluationDataset.testCases
        assertTrue("Dataset must contain at least 30 test cases", testCases.size >= 30)

        val answerable = testCases.filter { it.isAnswerable }
        val crossMaterial = testCases.filter { it.isCrossMaterial }
        val unsupported = testCases.filter { !it.isAnswerable }

        assertEquals(20, answerable.size)
        assertEquals(10, crossMaterial.size)
        assertEquals(10, unsupported.size)

        // Verify all 3 multimodal learning materials are present
        assertEquals(3, EvaluationDataset.allMaterials.size)
        val sourceTypes = EvaluationDataset.allMaterials.map { it.sourceType }.toSet()
        assertEquals(3, sourceTypes.size)

        // Verify test cases have valid IDs and queries
        testCases.forEach { tc ->
            assertTrue(tc.id.isNotBlank())
            assertTrue(tc.query.isNotBlank())
            if (tc.isAnswerable) {
                assertTrue(tc.expectedSourceChunkIds.isNotEmpty())
                assertTrue(tc.expectedSourceLocations.isNotEmpty())
            }
        }
    }

    @Test
    fun testPrecisionAtKCalculation() {
        val retrieved = listOf("chunk_1", "chunk_2", "chunk_3", "chunk_4", "chunk_5")
        val expected = listOf("chunk_1", "chunk_3", "chunk_7")

        // In top 3: chunk_1, chunk_2, chunk_3 -> 2 out of 3 match -> Precision@3 = 2/3 ≈ 0.6667
        val p3 = runner.calculatePrecisionAtK(retrieved, expected, 3)
        assertEquals(2.0 / 3.0, p3, 0.001)

        // In top 5: chunk_1, chunk_2, chunk_3, chunk_4, chunk_5 -> 2 out of 5 match -> Precision@5 = 2/5 = 0.4
        val p5 = runner.calculatePrecisionAtK(retrieved, expected, 5)
        assertEquals(2.0 / 5.0, p5, 0.001)

        // Zero matches
        val pZero = runner.calculatePrecisionAtK(retrieved, listOf("chunk_99"), 3)
        assertEquals(0.0, pZero, 0.001)
    }

    @Test
    fun testRecallAtKCalculation() {
        val retrieved = listOf("chunk_1", "chunk_2", "chunk_3", "chunk_4", "chunk_5")
        val expected = listOf("chunk_1", "chunk_3", "chunk_7") // 3 total relevant expected

        // In top 3: chunk_1 and chunk_3 match -> 2 out of 3 expected -> Recall@3 = 2/3 ≈ 0.6667
        val r3 = runner.calculateRecallAtK(retrieved, expected, 3)
        assertEquals(2.0 / 3.0, r3, 0.001)

        // In top 5: chunk_1 and chunk_3 match -> 2 out of 3 expected -> Recall@5 = 2/3 ≈ 0.6667
        val r5 = runner.calculateRecallAtK(retrieved, expected, 5)
        assertEquals(2.0 / 3.0, r5, 0.001)

        // If retrieved contains all expected
        val rAll = runner.calculateRecallAtK(listOf("chunk_1", "chunk_3", "chunk_7"), expected, 3)
        assertEquals(1.0, rAll, 0.001)
    }

    @Test
    fun testMeanReciprocalRankCalculation() {
        // First match at rank 1 -> RR = 1.0
        val rrRank1 = runner.calculateReciprocalRank(listOf("c1", "c2", "c3"), listOf("c1"))
        assertEquals(1.0, rrRank1, 0.001)

        // First match at rank 2 -> RR = 0.5
        val rrRank2 = runner.calculateReciprocalRank(listOf("c0", "c1", "c3"), listOf("c1"))
        assertEquals(0.5, rrRank2, 0.001)

        // First match at rank 3 -> RR = 1/3 ≈ 0.3333
        val rrRank3 = runner.calculateReciprocalRank(listOf("c0", "c2", "c1"), listOf("c1"))
        assertEquals(1.0 / 3.0, rrRank3, 0.001)

        // No match -> RR = 0.0
        val rrNone = runner.calculateReciprocalRank(listOf("c0", "c2", "c3"), listOf("c1"))
        assertEquals(0.0, rrNone, 0.001)
    }

    @Test
    fun testRetrievalBenchmarkExecution() {
        val metrics = runner.runRetrievalBenchmark()

        assertEquals(30, metrics.totalQueries)
        assertEquals(20, metrics.answerableQueries)
        assertEquals(10, metrics.unsupportedQueries)

        // Precision and Recall bounds
        assertTrue("Precision@3 must be in [0.0, 1.0]", metrics.precisionAt3 in 0.0..1.0)
        assertTrue("Recall@3 must be in [0.0, 1.0]", metrics.recallAt3 in 0.0..1.0)
        assertTrue("MRR must be >= 0.70 for high relevance ranking", metrics.meanReciprocalRank >= 0.70)
        assertTrue("Unsupported query refusal rate must be 100%", metrics.unsupportedRefusalRate == 1.0)
        assertTrue("Citation accuracy must be > 0.0", metrics.citationSourceIdAccuracy > 0.0)
    }

    @Test
    fun testAssessmentValidationAndDuplicateRate() {
        val sampleQuestions = listOf(
            AssessmentQuestion(
                id = "q1",
                question = "What condition does a mutex lock prevent?",
                type = AssessmentQuestionType.MCQ,
                options = listOf("Race conditions", "Deadlocks only", "Paging", "Thrashing"),
                correctAnswer = "Race conditions",
                explanation = "Mutual exclusion prevents concurrent race conditions",
                topic = "Concurrency",
                materialId = "mat1",
                documentName = "doc1.pdf",
                pageNumber = 1,
                sourceChunkId = "chunk1"
            ),
            AssessmentQuestion(
                id = "q2",
                question = "What condition does a mutex lock prevent?", // Duplicate!
                type = AssessmentQuestionType.MCQ,
                options = listOf("Race conditions", "Deadlocks only", "Paging", "Thrashing"),
                correctAnswer = "Race conditions",
                explanation = "Mutual exclusion prevents concurrent race conditions",
                topic = "Concurrency",
                materialId = "mat1",
                documentName = "doc1.pdf",
                pageNumber = 1,
                sourceChunkId = "chunk1"
            ),
            AssessmentQuestion(
                id = "q3",
                question = "What does the CAP theorem state?",
                type = AssessmentQuestionType.MCQ,
                options = listOf("At most two of C, A, P", "All three always", "None", "Only consistency"),
                correctAnswer = "At most two of C, A, P",
                explanation = "A distributed data store can satisfy at most two guarantees simultaneously",
                topic = "Distributed Systems",
                materialId = "mat2",
                documentName = "video.mp4",
                pageNumber = 1,
                sourceChunkId = "chunk2"
            )
        )

        val metrics = runner.runAssessmentBenchmark(sampleQuestions)
        assertEquals(3, metrics.totalQuestionsEvaluated)
        assertEquals(1, metrics.duplicateQuestionCount)
        assertEquals(1.0 / 3.0, metrics.questionRepetitionRate, 0.001)
        assertEquals(1.0, metrics.validFormatRate, 0.001)
        assertEquals(1.0, metrics.mcqOptionMatchRate, 0.001)
        assertEquals(1.0, metrics.validSourceChunkRate, 0.001)
    }

    @Test
    fun testMasteryEvaluationCorrectness() {
        val metrics = runner.runMasteryBenchmark()
        assertEquals(1.0, metrics.correctMasteryUpdateRate, 0.001)
        assertEquals(1.0, metrics.unassessedTopicsPreservedRate, 0.001)
        assertEquals(1.0, metrics.recommendationAccuracyRate, 0.001)
    }

    @Test
    fun testFullReportGeneration() {
        val sampleQuestions = listOf(
            AssessmentQuestion(
                id = "q1",
                question = "What is Raft leader election?",
                type = AssessmentQuestionType.MCQ,
                options = listOf("Election algorithm", "Paging", "Hashing", "Sorting"),
                correctAnswer = "Election algorithm",
                explanation = "Elects single leader in Raft term",
                topic = "Distributed Consensus",
                materialId = "mat_vid",
                documentName = "lecture.mp4",
                pageNumber = 1,
                sourceChunkId = "synth_mat_vid_p1_c0"
            )
        )

        val report = runner.generateFullReport(sampleQuestions)
        assertNotNull(report)
        assertEquals(30, report.datasetSize)
        assertTrue(report.retrievalMetrics.totalQueries == 30)
        assertTrue(report.retrievalMetrics.unsupportedRefusalRate == 1.0)
        assertTrue(report.assessmentMetrics.questionRepetitionRate == 0.0)
        assertTrue(report.masteryMetrics.correctMasteryUpdateRate == 1.0)
    }
}
