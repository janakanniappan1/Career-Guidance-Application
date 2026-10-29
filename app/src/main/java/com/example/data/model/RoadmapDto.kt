package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for Roadmap tables in Supabase:
 * - roadmaps
 * - roadmap_stages
 * - roadmap_tasks
 * - user_task_progress
 *
 * Adheres strictly to the PostgreSQL schema discovered in Supabase PostgREST:
 * - public.roadmaps (id, career_id, title, created_at, updated_at)
 * - public.roadmap_stages (id, roadmap_id, stage_name, stage_order, subtitle, created_at)
 * - public.roadmap_tasks (id, stage_id, title, description, skill_category, estimated_hours, resource_type, task_order, created_at)
 * - public.user_task_progress (id, user_id, task_id, is_completed, completed_at, created_at, updated_at)
 */

@Serializable
data class RoadmapDto(
    @SerialName("id") val id: String,
    @SerialName("career_id") val careerId: String,
    @SerialName("title") val title: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RoadmapStageDto(
    @SerialName("id") val id: String,
    @SerialName("roadmap_id") val roadmapId: String,
    @SerialName("stage_name") val stageName: String,
    @SerialName("stage_order") val stageOrder: Int = 1,
    @SerialName("subtitle") val subtitle: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RoadmapTaskDto(
    @SerialName("id") val id: String,
    @SerialName("stage_id") val stageId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("skill_category") val skillCategory: String? = null,
    @SerialName("estimated_hours") val estimatedHours: Int? = 10,
    @SerialName("resource_type") val resourceType: String? = "Course",
    @SerialName("task_order") val taskOrder: Int? = 1,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UserTaskProgressDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("task_id") val taskId: String,
    @SerialName("is_completed") val isCompleted: Boolean = true,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
