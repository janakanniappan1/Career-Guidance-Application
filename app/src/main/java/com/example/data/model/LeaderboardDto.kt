package com.example.data.model

import com.example.domain.model.LeaderboardUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object mapping PostgreSQL RPC get_domain_leaderboard return fields.
 */
@Serializable
data class LeaderboardEntryDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("display_name")
    val displayName: String? = "Student",
    @SerialName("domain_id")
    val domainId: String? = null,
    @SerialName("domain_title")
    val domainTitle: String? = null,
    @SerialName("department")
    val department: String? = null,
    @SerialName("year_of_study")
    val yearOfStudy: String? = null,
    @SerialName("xp")
    val xp: Int? = 0,
    @SerialName("completion_percentage")
    val completionPercentage: Int? = 0,
    @SerialName("streak")
    val streak: Int? = 0,
    @SerialName("completed_tasks")
    val completedTasks: Int? = 0,
    @SerialName("total_tasks")
    val totalTasks: Int? = 0
) {
    fun toDomain(currentUserId: String?): LeaderboardUser {
        val nonNullDomainId = domainId ?: ""
        return LeaderboardUser(
            userId = userId,
            displayName = (displayName ?: "Student").ifBlank { "Student" },
            domainId = nonNullDomainId,
            domainTitle = (domainTitle ?: nonNullDomainId).ifBlank { nonNullDomainId },
            department = department ?: "",
            yearOfStudy = yearOfStudy ?: "",
            xp = xp ?: 0,
            completionPercentage = (completionPercentage ?: 0).coerceIn(0, 100),
            streak = (streak ?: 0).coerceAtLeast(0),
            completedTasks = completedTasks ?: 0,
            totalTasks = totalTasks ?: 0,
            isCurrentUser = !currentUserId.isNullOrBlank() && currentUserId == userId
        )
    }
}
