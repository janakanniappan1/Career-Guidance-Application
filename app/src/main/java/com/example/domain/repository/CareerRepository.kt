package com.example.domain.repository

import com.example.domain.model.CareerRecommendation
import kotlinx.coroutines.flow.Flow

// TODO: Replace MockCareerRepository with SupabaseCareerRepository
interface CareerRepository {
    suspend fun calculateRecommendation(answers: Map<Int, String>): CareerRecommendation
    fun getLatestRecommendation(): Flow<CareerRecommendation?>
    suspend fun getCareerDetails(careerId: String): CareerRecommendation?
    suspend fun saveRecommendation(recommendation: CareerRecommendation)
    fun getAllCareers(): List<CareerRecommendation>
    suspend fun loadCareers(): Result<List<CareerRecommendation>> = Result.success(getAllCareers())
    suspend fun loadLatestRecommendation(userId: String? = null): Result<CareerRecommendation?> = Result.success(null)
}
