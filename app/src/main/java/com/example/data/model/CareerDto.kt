package com.example.data.model

import com.example.domain.model.CareerRecommendation
import com.example.domain.model.OtherCareerMatch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for Career tables in Supabase:
 * - careers
 * - skills
 * - career_skills
 * - career_recommendations
 * - career_recommendation_matches
 */

@Serializable
data class CareerDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("role_overview") val roleOverview: String? = null,
    @SerialName("average_salary") val averageSalary: String? = null,
    @SerialName("job_growth") val jobGrowth: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("reason_for_recommendation") val reasonForRecommendation: String? = null,
    @SerialName("required_skills") val requiredSkills: List<String>? = null
)

@Serializable
data class SkillDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String? = null
)

@Serializable
data class CareerSkillDto(
    @SerialName("id") val id: String? = null,
    @SerialName("career_id") val careerId: String,
    @SerialName("skill_id") val skillId: String? = null,
    @SerialName("skill_name") val skillName: String? = null
)

@Serializable
data class CareerRecommendationDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String = "",
    @SerialName("career_id") val careerId: String = "",
    @SerialName("match_percentage") val matchPercentage: Int = 0,
    @SerialName("reason_for_recommendation") val reasonForRecommendation: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val createdAtEpoch: Long?
        get() = try {
            if (!createdAt.isNullOrBlank()) {
                java.time.Instant.parse(createdAt).toEpochMilli()
            } else null
        } catch (_: Throwable) {
            null
        }
}

@Serializable
data class CareerRecommendationMatchDto(
    @SerialName("id") val id: String? = null,
    @SerialName("recommendation_id") val recommendationId: String = "",
    @SerialName("career_id") val careerId: String = "",
    @SerialName("match_percentage") val matchPercentage: Int = 0,
    @SerialName("short_desc") val shortDesc: String? = null
)
