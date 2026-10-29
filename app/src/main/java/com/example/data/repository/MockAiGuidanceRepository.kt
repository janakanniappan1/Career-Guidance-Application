package com.example.data.repository

import com.example.domain.model.AiCareerGuidance
import com.example.domain.model.CareerGuidanceContext
import com.example.domain.repository.AiGuidanceRepository
import kotlinx.coroutines.delay

/**
 * Mock implementation of AiGuidanceRepository for testing and local development.
 */
class MockAiGuidanceRepository(
    var shouldFail: Boolean = false,
    var delayMs: Long = 500L
) : AiGuidanceRepository {

    override suspend fun generateCareerGuidance(
        context: CareerGuidanceContext
    ): Result<AiCareerGuidance> {
        if (delayMs > 0) {
            delay(delayMs)
        }

        if (shouldFail) {
            return Result.failure(Exception("Mock simulated guidance failure"))
        }

        val deptMention = if (context.department.isNotBlank()) "in ${context.department}" else "in technology"
        val yearMention = if (context.year.isNotBlank()) "at the ${context.year} stage" else "in your studies"
        val skillsMention = if (context.careerSkills.isNotEmpty()) {
            "focusing on skills like ${context.careerSkills.take(2).joinToString(" and ")}"
        } else {
            "building relevant technical competencies"
        }

        val explanation = "${context.career} is a strong match (${context.matchPercentage}%) for you because your assessment indicates " +
                "a natural alignment with analytical problem-solving and structured technical thinking. " +
                "Given your background $deptMention $yearMention, " +
                "$skillsMention will provide a clear progression toward excelling in this field."

        return Result.success(AiCareerGuidance(explanation = explanation))
    }
}
