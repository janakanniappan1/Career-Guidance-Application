package com.example.domain.model

import kotlinx.serialization.Serializable

/**
 * Structured student context for generating personalized AI career guidance.
 * Only non-sensitive, relevant student context is included.
 */
@Serializable
data class CareerGuidanceContext(
    val department: String = "",
    val year: String = "",
    val careerInterest: String = "",
    val career: String,
    val matchPercentage: Int,
    val careerSkills: List<String> = emptyList()
)

/**
 * AI-generated explanation of why the recommended career is suitable for the student.
 */
@Serializable
data class AiCareerGuidance(
    val explanation: String = "",
    val guidance: String = ""
) {
    val displayGuidance: String
        get() = guidance.ifBlank { explanation }
}
