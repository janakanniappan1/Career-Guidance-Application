package com.example.domain.repository

import com.example.domain.model.AssessmentAttempt
import com.example.domain.model.AssessmentQuestion

// TODO: Replace MockAssessmentRepository with SupabaseAssessmentRepository
interface AssessmentRepository {
    fun getAssessmentQuestions(): List<AssessmentQuestion>
    suspend fun loadQuestions(): Result<List<AssessmentQuestion>> = Result.success(getAssessmentQuestions())
    suspend fun saveAnswer(questionId: Int, optionId: String)
    suspend fun getUserAnswers(): Map<Int, String>
    suspend fun saveAssessmentAttempt(attempt: AssessmentAttempt): Result<Unit>
    suspend fun clearAnswers()
}
