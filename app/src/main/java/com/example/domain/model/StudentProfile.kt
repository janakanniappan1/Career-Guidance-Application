package com.example.domain.model

/**
 * Represents an authenticated student profile.
 * Maps directly to the `profiles` table in Supabase (keyed by auth.users.id).
 */
data class StudentProfile(
    val id: String,
    val name: String,
    val email: String,
    val department: String = "",
    val yearOfStudy: String = "",
    val careerInterest: String = "",
    val currentStreak: Int = 0,
    val isAssessmentCompleted: Boolean = false,
    val recommendedCareerId: String? = null,
    val dailyMotivationalMessage: String = "Focus on progress, not perfection. Every skill learned is a step towards your dream career!",
    val xp: Int = 0,
    val createdAt: Long? = null
) {
    /**
     * Determines whether mandatory profile fields are complete.
     * Required for access to career roadmaps and dashboard features:
     * - name
     * - email
     * - department
     * - year of study
     * - career interest
     */
    val isProfileComplete: Boolean
        get() = name.isNotBlank() &&
            email.isNotBlank() &&
            department.isNotBlank() &&
            yearOfStudy.isNotBlank() &&
            careerInterest.isNotBlank()
}
