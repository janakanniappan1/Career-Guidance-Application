package com.example.data.repository

import com.example.domain.model.DomainLeaderboard
import com.example.domain.model.LeaderboardUser
import com.example.domain.model.RankedLeaderboardEntry
import com.example.domain.model.RankingMode
import com.example.domain.repository.LeaderboardRepository
import com.example.util.CareerCanonicalUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockLeaderboardRepository(
    private val currentUserId: String = "usr_student_demo"
) : LeaderboardRepository {

    override fun getLeaderboard(
        domainId: String,
        rankingMode: RankingMode
    ): Flow<DomainLeaderboard> = flow {
        emit(calculateLeaderboard(domainId, rankingMode))
    }

    override suspend fun refreshLeaderboard(
        domainId: String,
        rankingMode: RankingMode
    ): DomainLeaderboard {
        return calculateLeaderboard(domainId, rankingMode)
    }

    private fun calculateLeaderboard(
        domainId: String,
        rankingMode: RankingMode
    ): DomainLeaderboard {
        val canonicalDomainId = CareerCanonicalUtil.toCanonicalCareerId(domainId)
        val domainTitle = CareerCanonicalUtil.getCareerTitle(canonicalDomainId)
        val baseline = DefaultLeaderboardCatalog.getBaselinePeersForDomain(canonicalDomainId).toMutableList()

        val mockCurrentUser = LeaderboardUser(
            userId = currentUserId,
            displayName = "Student (You)",
            domainId = canonicalDomainId,
            domainTitle = domainTitle,
            department = "Computer Science",
            yearOfStudy = "3rd Year",
            xp = 2100,
            completionPercentage = 78,
            streak = 15,
            completedTasks = 16,
            totalTasks = 20,
            isCurrentUser = true
        )

        baseline.add(mockCurrentUser)

        val sorted = when (rankingMode) {
            RankingMode.XP -> baseline.sortedWith(
                compareByDescending<LeaderboardUser> { it.xp }
                    .thenByDescending { it.completionPercentage }
                    .thenByDescending { it.streak }
                    .thenBy { it.displayName }
            )
            RankingMode.COMPLETION -> baseline.sortedWith(
                compareByDescending<LeaderboardUser> { it.completionPercentage }
                    .thenByDescending { it.xp }
                    .thenByDescending { it.streak }
                    .thenBy { it.displayName }
            )
            RankingMode.STREAK -> baseline.sortedWith(
                compareByDescending<LeaderboardUser> { it.streak }
                    .thenByDescending { it.xp }
                    .thenByDescending { it.completionPercentage }
                    .thenBy { it.displayName }
            )
        }

        val entries = sorted.mapIndexed { index, user ->
            val rank = index + 1
            val metric = when (rankingMode) {
                RankingMode.XP -> "${user.xp} XP"
                RankingMode.COMPLETION -> "${user.completionPercentage}%"
                RankingMode.STREAK -> "${user.streak} ${if (user.streak == 1) "day" else "days"}"
            }
            RankedLeaderboardEntry(
                rank = rank,
                user = user,
                primaryMetricValue = metric,
                metricUnit = rankingMode.unit,
                rankingMode = rankingMode,
                isTop3 = rank in 1..3,
                isCurrentUser = user.isCurrentUser
            )
        }

        return DomainLeaderboard(
            domainId = canonicalDomainId,
            domainTitle = domainTitle,
            rankingMode = rankingMode,
            entries = entries,
            currentUserEntry = entries.firstOrNull { it.isCurrentUser },
            totalParticipants = entries.size,
            domainTotalTasks = 20
        )
    }
}
