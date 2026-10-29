package com.example.domain.repository

import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.TutorResponse

/**
 * Repository interface for communicating with the secure server-side
 * AI Learning Companion tutor endpoint.
 */
interface LearningTutorRepository {
    suspend fun askTutor(
        question: String,
        contextChunks: List<KnowledgeChunk>
    ): Result<TutorResponse>
}
