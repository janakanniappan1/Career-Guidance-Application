package com.example.domain.repository

import com.example.domain.model.DomainLeaderboard
import com.example.domain.model.RankingMode
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for retrieving and ranking domain-wise CGA leaderboard data.
 */
interface LeaderboardRepository {
    /**
     * Observes live leaderboard state for the selected career/domain and ranking mode.
     */
    fun getLeaderboard(
        domainId: String,
        rankingMode: RankingMode
    ): Flow<DomainLeaderboard>

    /**
     * Forces a refresh from Supabase.
     */
    suspend fun refreshLeaderboard(
        domainId: String,
        rankingMode: RankingMode
    ): DomainLeaderboard
}
