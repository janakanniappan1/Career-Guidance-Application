package com.example.domain.model

import kotlinx.serialization.Serializable

/**
 * Compact student roadmap context for generating personalized roadmap guidance.
 * Derived dynamically from the authenticated student's actual roadmap progress.
 */
@Serializable
data class RoadmapGuidanceContext(
    val career: String,
    val overallProgressPercentage: Int,
    val completedTasks: List<String> = emptyList(),
    val pendingTasks: List<String> = emptyList(),
    val currentStage: String = "",
    val nextTasks: List<String> = emptyList(),
    val skillCategories: List<String> = emptyList()
)

/**
 * AI-generated guidance advising the student on next priorities based on their actual roadmap progress.
 */
@Serializable
data class AiRoadmapGuidance(
    val guidance: String
)
