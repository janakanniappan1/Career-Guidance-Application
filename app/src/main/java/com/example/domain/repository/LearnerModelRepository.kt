package com.example.domain.repository

import com.example.domain.model.adaptive.AssessmentResult
import com.example.domain.model.adaptive.TopicMastery
import com.example.domain.model.adaptive.TutorLearningSignal
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing learner mastery state,
 * updating topic competencies using the Weighted Evidence Mastery Model,
 * and recording conversation signals from the Source-Grounded AI Tutor.
 */
interface LearnerModelRepository {

    fun getAllMasteryFlow(materialId: String? = null): Flow<List<TopicMastery>>

    suspend fun getMastery(materialId: String, topic: String): TopicMastery?

    suspend fun getAllMastery(materialId: String): List<TopicMastery>

    suspend fun updateFromAssessment(materialId: String, result: AssessmentResult): List<TopicMastery>

    suspend fun getWeakTopics(materialId: String): List<TopicMastery>

    suspend fun getStrongTopics(materialId: String): List<TopicMastery>

    suspend fun getRecommendedTopic(materialId: String): TopicMastery?

    suspend fun recordTutorSignal(materialId: String, signal: TutorLearningSignal)

    suspend fun deleteMaterialMastery(materialId: String): Result<Unit> = Result.success(Unit)

    suspend fun clearMastery()
}
