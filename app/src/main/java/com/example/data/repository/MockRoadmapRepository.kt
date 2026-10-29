package com.example.data.repository

import com.example.domain.model.CareerRoadmap
import com.example.domain.model.RoadmapStage
import com.example.domain.model.RoadmapTask
import com.example.domain.model.SkillProgressItem
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.RoadmapRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// TODO: Replace MockRoadmapRepository with SupabaseRoadmapRepository
class MockRoadmapRepository(
    private val authRepository: AuthRepository? = null
) : RoadmapRepository {

    // User-specific task progress state (maps to user_task_progress table in Supabase)
    // Completed task IDs for student session
    private val _userCompletedTasks = MutableStateFlow<Set<String>>(
        setOf("t1_1", "t1_2", "t1_3", "t1_4", "t2_1", "t2_2", "t2_3", "t3_1", "t3_2", "sd_1", "sd_2", "sd_3", "sd_5", "da_1", "da_2", "da_3", "da_5")
    )

    override fun getRoadmap(careerId: String): Flow<CareerRoadmap> {
        val baseTemplate = DefaultRoadmapCatalog.getStandardRoadmap(careerId)
            ?: DefaultRoadmapCatalog.getStandardRoadmap("aiml_engineer")!!
        return _userCompletedTasks.map { completedTaskIds ->
            baseTemplate.copy(
                stages = baseTemplate.stages.map { stage ->
                    stage.copy(
                        tasks = stage.tasks.map { task ->
                            task.copy(isCompleted = completedTaskIds.contains(task.id))
                        }
                    )
                }
            )
        }
    }

    override suspend fun toggleTask(careerId: String, stageId: String, taskId: String) {
        _userCompletedTasks.update { currentIds ->
            if (currentIds.contains(taskId)) {
                currentIds - taskId
            } else {
                currentIds + taskId
            }
        }
    }

    override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> {
        return getRoadmap(careerId).map { roadmap ->
            val allTasks = roadmap.stages.flatMap { it.tasks }
            val groupedBySkill = allTasks.groupBy { it.skillCategory }

            groupedBySkill.map { (skillName, tasks) ->
                val completed = tasks.count { it.isCompleted }
                val total = tasks.size
                val percentage = if (total == 0) 0 else ((completed.toFloat() / total.toFloat()) * 100).toInt()
                SkillProgressItem(
                    skillName = skillName,
                    percentage = percentage,
                    completedCount = completed,
                    totalCount = total
                )
            }.sortedByDescending { it.percentage }
        }
    }

    override fun getOverallProgress(careerId: String): Flow<Int> {
        return getRoadmap(careerId).map { roadmap ->
            roadmap.overallProgressPercentage
        }
    }
}
