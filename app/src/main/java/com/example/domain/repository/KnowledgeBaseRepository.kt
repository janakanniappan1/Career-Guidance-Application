package com.example.domain.repository

import com.example.domain.model.KnowledgeBaseStats
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningMaterial
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing knowledge base chunks, topics, concepts,
 * and deterministic keyword-based source retrieval.
 */
interface KnowledgeBaseRepository {
    fun getChunksFlow(materialId: String? = null): Flow<List<KnowledgeChunk>>
    suspend fun ingestMaterial(material: LearningMaterial): Result<List<KnowledgeChunk>>
    suspend fun getChunks(materialId: String): List<KnowledgeChunk>
    suspend fun getAllChunks(): List<KnowledgeChunk>
    suspend fun getChunksByTopic(materialId: String, topic: String): List<KnowledgeChunk>
    suspend fun searchChunks(materialId: String?, query: String): List<KnowledgeChunk>
    suspend fun retrieveRelevantResults(materialId: String?, query: String, limit: Int = 10): List<com.example.domain.model.RetrievalResult> = emptyList()
    suspend fun getTopics(materialId: String): List<String>
    suspend fun getConcepts(materialId: String): List<String>
    suspend fun getStats(materialId: String): KnowledgeBaseStats?
    suspend fun getAllStats(): List<KnowledgeBaseStats>
    suspend fun deleteMaterialKnowledge(materialId: String): Result<Unit>
}
