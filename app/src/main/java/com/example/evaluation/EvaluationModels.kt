package com.example.evaluation

import com.example.domain.model.MaterialSourceType
import kotlinx.serialization.Serializable

/**
 * Synthetic evaluation case schema for reproducible benchmarking.
 */
@Serializable
data class EvaluationTestCase(
    val id: String,
    val query: String,
    val expectedAnswerKeywords: List<String>,
    val expectedSourceChunkIds: List<String>,
    val expectedSourceLocations: List<String>,
    val relevantTopic: String,
    val isAnswerable: Boolean,
    val isCrossMaterial: Boolean = false,
    val expectedRefusalBehavior: Boolean = !isAnswerable
)

/**
 * Metric results container for reproducible benchmark reporting.
 */
@Serializable
data class RetrievalEvaluationMetrics(
    val totalQueries: Int,
    val answerableQueries: Int,
    val unsupportedQueries: Int,
    val precisionAt3: Double,
    val recallAt3: Double,
    val precisionAt5: Double,
    val recallAt5: Double,
    val meanReciprocalRank: Double,
    val citationSourceIdAccuracy: Double,
    val unsupportedRefusalRate: Double
)

@Serializable
data class AssessmentEvaluationMetrics(
    val totalQuestionsEvaluated: Int,
    val validFormatCount: Int,
    val validFormatRate: Double,
    val mcqOptionMatchCount: Int,
    val mcqOptionMatchRate: Double,
    val validSourceChunkCount: Int,
    val validSourceChunkRate: Double,
    val duplicateQuestionCount: Int,
    val questionRepetitionRate: Double,
    val scoringAccuracyRate: Double,
    val weakTopicIdentificationRate: Double
)

@Serializable
data class MasteryEvaluationMetrics(
    val totalProfilesTested: Int,
    val correctMasteryUpdateRate: Double,
    val unassessedTopicsPreservedRate: Double,
    val recommendationAccuracyRate: Double
)

@Serializable
data class SystemEvaluationReport(
    val timestamp: Long,
    val datasetSize: Int,
    val retrievalMetrics: RetrievalEvaluationMetrics,
    val assessmentMetrics: AssessmentEvaluationMetrics,
    val masteryMetrics: MasteryEvaluationMetrics
)
