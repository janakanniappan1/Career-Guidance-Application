package com.example.domain.model

data class OtherCareerMatch(
    val careerId: String,
    val title: String,
    val matchPercentage: Int,
    val shortDesc: String = ""
)

/**
 * Global career profile definition.
 * Maps to `careers` table in Supabase.
 */
data class Career(
    val id: String,
    val title: String,
    val roleOverview: String,
    val averageSalary: String,
    val jobGrowth: String,
    val requiredSkills: List<String>
)

/**
 * User-specific career recommendation output from assessment.
 * Maps to `user_career_recommendations` table in Supabase.
 */
data class CareerRecommendation(
    val id: String? = null,
    val userId: String? = null,
    val careerId: String,
    val title: String,
    val matchPercentage: Int,
    val reasonForRecommendation: String,
    val otherSuitableCareers: List<OtherCareerMatch> = emptyList(),
    val recommendedSkills: List<String> = emptyList(),
    val roleOverview: String = "",
    val averageSalary: String = "",
    val jobGrowth: String = "",
    val createdAt: Long? = null
)
