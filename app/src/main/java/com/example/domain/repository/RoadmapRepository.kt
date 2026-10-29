package com.example.domain.repository

import com.example.domain.model.CareerRoadmap
import com.example.domain.model.SkillProgressItem
import kotlinx.coroutines.flow.Flow

// TODO: Replace MockRoadmapRepository with SupabaseRoadmapRepository
interface RoadmapRepository {
    fun getRoadmap(careerId: String): Flow<CareerRoadmap>
    suspend fun toggleTask(careerId: String, stageId: String, taskId: String)
    fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>>
    fun getOverallProgress(careerId: String): Flow<Int>
}
