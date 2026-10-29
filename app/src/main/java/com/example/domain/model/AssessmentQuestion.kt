package com.example.domain.model

/**
 * Global option for assessment questions.
 * Maps to `assessment_options` table in Supabase.
 */
data class AssessmentOption(
    val id: String,
    val text: String,
    // Weights corresponding to career keys:
    // software_dev, aiml_engineer, data_analyst, data_scientist, uiux_designer, cyber_analyst, cloud_engineer
    val scores: Map<String, Int>
)

/**
 * Global assessment question template.
 * Maps to `assessment_questions` table in Supabase.
 */
data class AssessmentQuestion(
    val id: Int,
    val questionNumber: Int,
    val category: String,
    val questionText: String,
    val description: String = "",
    val options: List<AssessmentOption>
)

data class AssessmentAnswer(
    val questionId: Int,
    val selectedOptionId: String
)

/**
 * User-specific assessment submission and result.
 * Maps to `assessment_attempts` table in Supabase.
 */
data class AssessmentAttempt(
    val id: String? = null,
    val userId: String,
    val answers: Map<Int, String> = emptyMap(),
    val topCareerId: String,
    val matchPercentage: Int,
    val completedAt: Long? = null
)
