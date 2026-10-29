package com.example.domain.model

/**
 * Global roadmap task definition.
 * Maps to `roadmap_tasks` table in Supabase.
 * `isCompleted` is computed at runtime for UI convenience from `user_task_progress`.
 */
data class RoadmapTask(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val skillCategory: String, // e.g. "Python", "SQL", "Machine Learning", "System Design"
    val estimatedHours: Int = 10,
    val resourceType: String = "Course"
)

/**
 * Global roadmap stage template.
 * Maps to `roadmap_stages` table in Supabase.
 */
data class RoadmapStage(
    val id: String,
    val stageName: String,
    val order: Int,
    val subtitle: String,
    val tasks: List<RoadmapTask>
) {
    val completedTasksCount: Int
        get() = tasks.count { it.isCompleted }

    val totalTasksCount: Int
        get() = tasks.size

    val progressPercentage: Int
        get() = if (tasks.isEmpty()) 0 else ((completedTasksCount.toFloat() / totalTasksCount.toFloat()) * 100).toInt()
}

data class SkillProgressItem(
    val skillName: String,
    val percentage: Int,
    val completedCount: Int,
    val totalCount: Int
)

/**
 * Aggregated Career Roadmap template.
 * Maps to `roadmaps` table in Supabase.
 */
data class CareerRoadmap(
    val careerId: String,
    val careerTitle: String,
    val stages: List<RoadmapStage>
) {
    val completedTasksCount: Int
        get() = stages.sumOf { it.completedTasksCount }

    val totalTasksCount: Int
        get() = stages.sumOf { it.totalTasksCount }

    val overallProgressPercentage: Int
        get() {
            if (totalTasksCount == 0) return 0
            return ((completedTasksCount.toFloat() / totalTasksCount.toFloat()) * 100).toInt()
        }
}

/**
 * User-specific task progress item.
 * Maps to `user_task_progress` table in Supabase.
 */
data class UserTaskProgress(
    val id: String? = null,
    val userId: String,
    val taskId: String,
    val isCompleted: Boolean = true,
    val completedAt: Long? = null
)

/**
 * User-specific roadmap overall progress summary.
 * Maps to `user_roadmap_progress` table or view in Supabase.
 */
data class UserRoadmapProgress(
    val id: String? = null,
    val userId: String,
    val careerId: String,
    val overallProgressPercentage: Int = 0,
    val completedTaskIds: Set<String> = emptySet(),
    val lastUpdated: Long? = null
)
