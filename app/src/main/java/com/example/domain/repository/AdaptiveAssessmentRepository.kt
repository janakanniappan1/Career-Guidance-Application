package com.example.domain.repository

import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.adaptive.Assessment
import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentResult

/**
 * Repository interface for Adaptive Assessment question generation,
 * strict validation, and scoring.
 */
interface AdaptiveAssessmentRepository {

    suspend fun generateAssessment(
        materialId: String,
        documentName: String,
        scope: String,
        difficulty: AssessmentDifficulty,
        questionCount: Int,
        contextChunks: List<KnowledgeChunk>
    ): Result<Assessment>

    fun evaluateAssessment(
        assessment: Assessment,
        studentAnswers: Map<String, String>
    ): AssessmentResult
}
