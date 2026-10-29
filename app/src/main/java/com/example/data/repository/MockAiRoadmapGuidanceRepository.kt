package com.example.data.repository

import com.example.domain.model.AiRoadmapGuidance
import com.example.domain.model.RoadmapGuidanceContext
import com.example.domain.repository.AiRoadmapGuidanceRepository
import kotlinx.coroutines.delay

/**
 * Mock implementation of AiRoadmapGuidanceRepository for testing and local development.
 */
class MockAiRoadmapGuidanceRepository(
    var shouldFail: Boolean = false,
    var delayMs: Long = 500L
) : AiRoadmapGuidanceRepository {

    override suspend fun generateRoadmapGuidance(
        context: RoadmapGuidanceContext
    ): Result<AiRoadmapGuidance> {
        if (delayMs > 0) {
            delay(delayMs)
        }

        if (shouldFail) {
            return Result.failure(Exception("Mock simulated roadmap guidance failure"))
        }

        val completedMention = if (context.completedTasks.isNotEmpty()) {
            "Having already completed checkpoints like ${context.completedTasks.take(2).joinToString(" and ")}, you have built solid foundational momentum."
        } else {
            "As you are beginning this curriculum, establishing regular study habits will help you maintain momentum."
        }

        val nextMention = if (context.nextTasks.isNotEmpty()) {
            "Your immediate next focus should be on ${context.nextTasks.first()}, as it forms the direct prerequisite for subsequent milestones."
        } else if (context.pendingTasks.isNotEmpty()) {
            "Your immediate next focus should be on ${context.pendingTasks.first()}."
        } else {
            "You have completed all primary roadmap checkpoints for this curriculum."
        }

        val stageMention = if (context.currentStage.isNotBlank()) {
            "You are currently progressing through the ${context.currentStage} stage at ${context.overallProgressPercentage}% completion."
        } else {
            "You have completed ${context.overallProgressPercentage}% of your total path."
        }

        val guidance = "$stageMention $completedMention $nextMention Consistent daily practice on these active checkpoints will keep you on track for campus placements."

        return Result.success(AiRoadmapGuidance(guidance = guidance))
    }
}
