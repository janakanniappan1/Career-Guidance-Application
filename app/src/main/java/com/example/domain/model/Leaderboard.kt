package com.example.domain.model

/**
 * Supported Leaderboard ranking modes for CGA domains.
 */
enum class RankingMode(val displayName: String, val tabLabel: String, val unit: String) {
    XP("XP Ranking", "XP", "XP"),
    COMPLETION("Course Completion", "Completion", "%"),
    STREAK("Streak Ranking", "Streak", "days")
}

/**
 * Participant user representation in domain leaderboard.
 */
data class LeaderboardUser(
    val userId: String,
    val displayName: String,
    val domainId: String,
    val domainTitle: String,
    val department: String = "",
    val yearOfStudy: String = "",
    val xp: Int = 0,
    val completionPercentage: Int = 0,
    val streak: Int = 0,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val isCurrentUser: Boolean = false
)

/**
 * Ranked entry with calculated position and score formatted for the active ranking mode.
 */
data class RankedLeaderboardEntry(
    val rank: Int,
    val user: LeaderboardUser,
    val primaryMetricValue: String,
    val metricUnit: String,
    val rankingMode: RankingMode,
    val isTop3: Boolean = rank in 1..3,
    val isCurrentUser: Boolean = user.isCurrentUser
)

/**
 * Aggregated domain leaderboard containing sorted entries and current user summary.
 */
data class DomainLeaderboard(
    val domainId: String,
    val domainTitle: String,
    val rankingMode: RankingMode,
    val entries: List<RankedLeaderboardEntry>,
    val currentUserEntry: RankedLeaderboardEntry? = null,
    val totalParticipants: Int = entries.size,
    val domainTotalTasks: Int = 0
)
