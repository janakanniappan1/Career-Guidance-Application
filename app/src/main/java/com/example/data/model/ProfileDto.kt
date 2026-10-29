package com.example.data.model

import com.example.domain.model.StudentProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for mapping the Supabase `public.profiles` table.
 * Adheres strictly to PostgreSQL column naming conventions (snake_case).
 */
@Serializable
data class ProfileDto(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String = "",
    @SerialName("email")
    val email: String = "",
    @SerialName("department")
    val department: String = "",
    @SerialName("year_of_study")
    val yearOfStudy: String? = null,
    @SerialName("career_interest")
    val careerInterest: String? = null,
    @SerialName("current_streak")
    val currentStreak: Int? = 0,
    @SerialName("is_assessment_completed")
    val isAssessmentCompleted: Boolean? = false,
    @SerialName("recommended_career_id")
    val recommendedCareerId: String? = null,
    @SerialName("daily_motivational_message")
    val dailyMotivationalMessage: String? = null,
    @SerialName("xp")
    val xp: Int? = 0,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(): StudentProfile {
        return StudentProfile(
            id = id,
            name = name.ifBlank { "Student" },
            email = email,
            department = department,
            yearOfStudy = yearOfStudy ?: "",
            careerInterest = careerInterest ?: "",
            currentStreak = (currentStreak ?: 0).coerceAtLeast(0),
            isAssessmentCompleted = isAssessmentCompleted ?: false,
            recommendedCareerId = recommendedCareerId,
            dailyMotivationalMessage = dailyMotivationalMessage?.ifBlank {
                "Focus on progress, not perfection. Every skill learned is a step towards your dream career!"
            } ?: "Focus on progress, not perfection. Every skill learned is a step towards your dream career!",
            xp = xp ?: 0,
            createdAt = try {
                if (!createdAt.isNullOrBlank()) {
                    java.time.Instant.parse(createdAt).toEpochMilli()
                } else null
            } catch (_: Throwable) {
                null
            }
        )
    }

    companion object {
        fun fromDomain(profile: StudentProfile): ProfileDto {
            return ProfileDto(
                id = profile.id,
                name = profile.name,
                email = profile.email,
                department = profile.department,
                yearOfStudy = profile.yearOfStudy,
                careerInterest = profile.careerInterest,
                currentStreak = profile.currentStreak,
                isAssessmentCompleted = profile.isAssessmentCompleted,
                recommendedCareerId = profile.recommendedCareerId,
                dailyMotivationalMessage = profile.dailyMotivationalMessage,
                xp = profile.xp,
                createdAt = profile.createdAt?.let {
                    java.time.Instant.ofEpochMilli(it).toString()
                }
            )
        }
    }
}

/**
 * Parameter DTO for invoking the atomic PostgreSQL RPC function `increment_profile_xp`.
 */
@Serializable
data class IncrementProfileXpParams(
    @SerialName("p_user_id")
    val userId: String,
    @SerialName("p_amount")
    val amount: Int
)
