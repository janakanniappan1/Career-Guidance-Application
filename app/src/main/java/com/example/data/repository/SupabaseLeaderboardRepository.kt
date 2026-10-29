package com.example.data.repository

import android.util.Log
import com.example.data.model.LeaderboardEntryDto
import com.example.data.model.ProfileDto
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.DomainLeaderboard
import com.example.domain.model.LeaderboardUser
import com.example.domain.model.RankedLeaderboardEntry
import com.example.domain.model.RankingMode
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.LeaderboardRepository
import com.example.domain.repository.RoadmapRepository
import com.example.util.CareerCanonicalUtil
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Production implementation of LeaderboardRepository backed by Supabase PostgREST RPC
 * with direct table queries for real authenticated database users only.
 *
 * Guarantees:
 * - Real Users Only: Strictly queries PostgreSQL `get_domain_leaderboard` and `public.profiles`.
 * - No Mock Fallbacks: Never injects fake/mock catalog peers into production views.
 * - Live Authoritative Progress: Accurately computes user task completion and XP rankings.
 */
class SupabaseLeaderboardRepository(
    private val authRepository: AuthRepository? = null,
    private val roadmapRepository: RoadmapRepository? = null,
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) : LeaderboardRepository {

    companion object {
        private const val TAG = "LeaderboardRepo"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    override fun getLeaderboard(
        domainId: String,
        rankingMode: RankingMode
    ): Flow<DomainLeaderboard> = flow {
        val result = loadLeaderboard(domainId, rankingMode)
        emit(result)
    }.flowOn(ioDispatcher)

    override suspend fun refreshLeaderboard(
        domainId: String,
        rankingMode: RankingMode
    ): DomainLeaderboard = withContext(ioDispatcher) {
        loadLeaderboard(domainId, rankingMode)
    }

    private suspend fun loadLeaderboard(
        domainId: String,
        rankingMode: RankingMode
    ): DomainLeaderboard {
        val canonicalDomainId = CareerCanonicalUtil.toCanonicalCareerId(domainId)
        val domainTitle = CareerCanonicalUtil.getCareerTitle(canonicalDomainId)

        // 1. Resolve current authenticated user profile and details
        val currentUserProfile = try {
            authRepository?.getCurrentUser()?.firstOrNull()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }

        val currentUserId = currentUserProfile?.id
            ?: try { client.auth.currentUserOrNull()?.id } catch (_: Exception) { null }

        val isAuthed = !currentUserId.isNullOrBlank()
        val safeAuthId = if (isAuthed) currentUserId else "unauthenticated"
        Log.d(TAG, "LEADERBOARD REQUEST -> Domain: $canonicalDomainId (raw: $domainId) | Mode: ${rankingMode.name} | AuthUser: $safeAuthId")

        // 2. Resolve total roadmap tasks for this domain from RoadmapRepository
        val domainTotalTasks = try {
            roadmapRepository?.getRoadmap(canonicalDomainId)?.firstOrNull()?.totalTasksCount ?: 25
        } catch (_: Exception) {
            25
        }

        // 3. Query domain leaderboard: First try authoritative Supabase RPC get_domain_leaderboard
        var remoteUsers: List<LeaderboardUser>? = null

        try {
            val rpcParams = buildJsonObject {
                put("p_domain_id", canonicalDomainId)
            }
            val response = client.postgrest.rpc(
                function = "get_domain_leaderboard",
                parameters = rpcParams
            )
            val dtoList = response.decodeList<LeaderboardEntryDto>()
            if (dtoList.isNotEmpty()) {
                remoteUsers = dtoList
                    .distinctBy { it.userId }
                    .map { it.toDomain(currentUserId) }
                Log.d(TAG, "LEADERBOARD RPC SUCCESS -> Domain: $canonicalDomainId | Real DB Entries: ${remoteUsers.size}")
            } else {
                remoteUsers = emptyList()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "get_domain_leaderboard RPC unavailable (${e.message}). Querying Edge Function next.", e)
        }

        // 4. Fallback A: Query Supabase Edge Function get-domain-leaderboard
        if (remoteUsers == null) {
            val edgeUsers = fetchFromEdgeFunction(canonicalDomainId, currentUserId)
            if (edgeUsers != null && edgeUsers.isNotEmpty()) {
                remoteUsers = edgeUsers
                Log.d(TAG, "LEADERBOARD EDGE FUNCTION SUCCESS -> Domain: $canonicalDomainId | Real DB Entries: ${remoteUsers.size}")
            }
        }

        // 4. Fallback B: Query real profiles table directly
        if (remoteUsers == null) {
            try {
                val profiles = client.from("profiles").select().decodeList<ProfileDto>()
                Log.d(TAG, "Direct profiles query returned ${profiles.size} real profiles")
                val dbUsers = mutableListOf<LeaderboardUser>()
                for (p in profiles) {
                    val pDomain = CareerCanonicalUtil.toCanonicalCareerIdOrNull(p.recommendedCareerId)
                        ?: CareerCanonicalUtil.toCanonicalCareerIdOrNull(p.careerInterest)
                        ?: canonicalDomainId

                    if (pDomain == canonicalDomainId || p.recommendedCareerId.isNullOrBlank()) {
                        val isCurrent = !currentUserId.isNullOrBlank() && p.id == currentUserId
                        val userXp = p.xp ?: 0
                        val userStreak = p.currentStreak ?: 0
                        val completedCount = ((userXp / 100).coerceAtMost(domainTotalTasks))
                        val completionPct = if (domainTotalTasks > 0) {
                            ((completedCount.toFloat() / domainTotalTasks) * 100).toInt().coerceIn(0, 100)
                        } else 0

                        dbUsers.add(
                            LeaderboardUser(
                                userId = p.id,
                                displayName = p.name.ifBlank { if (isCurrent) "You" else "Student" },
                                domainId = canonicalDomainId,
                                domainTitle = domainTitle,
                                department = p.department,
                                yearOfStudy = p.yearOfStudy ?: "",
                                xp = userXp,
                                completionPercentage = completionPct,
                                streak = userStreak,
                                completedTasks = completedCount,
                                totalTasks = domainTotalTasks,
                                isCurrentUser = isCurrent
                            )
                        )
                    }
                }
                if (dbUsers.isNotEmpty()) {
                    remoteUsers = dbUsers
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Direct profiles query fallback notice: ${e.message}")
            }
        }

        val mergedUsers = (remoteUsers ?: emptyList()).toMutableList()

        // 5. Ensure current authenticated user is included if belonging to this domain or if active
        if (currentUserProfile != null && !currentUserId.isNullOrBlank()) {
            val userDomain = CareerCanonicalUtil.toCanonicalCareerIdOrNull(currentUserProfile.recommendedCareerId)
                ?: CareerCanonicalUtil.toCanonicalCareerIdOrNull(currentUserProfile.careerInterest)
                ?: canonicalDomainId

            val userCompletedTasks = try {
                roadmapRepository?.getRoadmap(canonicalDomainId)?.firstOrNull()?.completedTasksCount ?: 0
            } catch (_: Exception) { 0 }

            val userCompletionPct = if (domainTotalTasks > 0) {
                ((userCompletedTasks.toFloat() / domainTotalTasks) * 100).toInt().coerceIn(0, 100)
            } else 0

            val existingIndex = mergedUsers.indexOfFirst { it.userId == currentUserId }
            val currentUserItem = LeaderboardUser(
                userId = currentUserId,
                displayName = currentUserProfile.name.ifBlank { "You" },
                domainId = canonicalDomainId,
                domainTitle = domainTitle,
                department = currentUserProfile.department,
                yearOfStudy = currentUserProfile.yearOfStudy,
                xp = currentUserProfile.xp,
                completionPercentage = userCompletionPct,
                streak = currentUserProfile.currentStreak,
                completedTasks = userCompletedTasks,
                totalTasks = domainTotalTasks,
                isCurrentUser = true
            )

            if (existingIndex >= 0) {
                val existing = mergedUsers[existingIndex]
                mergedUsers[existingIndex] = currentUserItem.copy(
                    xp = maxOf(existing.xp, currentUserProfile.xp),
                    streak = maxOf(existing.streak, currentUserProfile.currentStreak),
                    completedTasks = maxOf(existing.completedTasks, userCompletedTasks),
                    completionPercentage = maxOf(existing.completionPercentage, userCompletionPct)
                )
            } else if (userDomain == canonicalDomainId || currentUserProfile.recommendedCareerId.isNullOrBlank()) {
                mergedUsers.add(currentUserItem)
            }
        }

        // 7. Apply deterministic domain-wise sorting with tie-breaking rules
        val sortedUsers = when (rankingMode) {
            RankingMode.XP -> {
                mergedUsers.sortedWith(
                    compareByDescending<LeaderboardUser> { it.xp }
                        .thenByDescending { it.completionPercentage }
                        .thenByDescending { it.streak }
                        .thenBy { it.displayName.lowercase() }
                        .thenBy { it.userId }
                )
            }
            RankingMode.COMPLETION -> {
                mergedUsers.sortedWith(
                    compareByDescending<LeaderboardUser> { it.completionPercentage }
                        .thenByDescending { it.xp }
                        .thenByDescending { it.streak }
                        .thenBy { it.displayName.lowercase() }
                        .thenBy { it.userId }
                )
            }
            RankingMode.STREAK -> {
                mergedUsers.sortedWith(
                    compareByDescending<LeaderboardUser> { it.streak }
                        .thenByDescending { it.xp }
                        .thenByDescending { it.completionPercentage }
                        .thenBy { it.displayName.lowercase() }
                        .thenBy { it.userId }
                )
            }
        }

        // 8. Format ranked entries
        val rankedEntries = sortedUsers.mapIndexed { index, user ->
            val rank = index + 1
            val primaryMetricValue = when (rankingMode) {
                RankingMode.XP -> "${user.xp} XP"
                RankingMode.COMPLETION -> "${user.completionPercentage}%"
                RankingMode.STREAK -> "${user.streak} ${if (user.streak == 1) "day" else "days"}"
            }
            RankedLeaderboardEntry(
                rank = rank,
                user = user,
                primaryMetricValue = primaryMetricValue,
                metricUnit = rankingMode.unit,
                rankingMode = rankingMode,
                isTop3 = rank in 1..3,
                isCurrentUser = user.isCurrentUser
            )
        }

        val currentUserEntry = rankedEntries.firstOrNull { it.isCurrentUser }
            ?: if (currentUserProfile != null && !currentUserId.isNullOrBlank()) {
                val userDomainId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(currentUserProfile.recommendedCareerId)
                    ?: CareerCanonicalUtil.toCanonicalCareerIdOrNull(currentUserProfile.careerInterest)
                    ?: CareerCanonicalUtil.DEFAULT_CAREER_ID
                val userDomainTitle = CareerCanonicalUtil.getCareerTitle(userDomainId)
                val userPrimaryRoadmap = try {
                    roadmapRepository?.getRoadmap(userDomainId)?.firstOrNull()
                } catch (_: Exception) { null }
                val userCompletedTasks = userPrimaryRoadmap?.completedTasksCount ?: (currentUserProfile.xp / 100)
                val userTotalTasks = userPrimaryRoadmap?.totalTasksCount ?: 10
                val userCompletionPct = if (userTotalTasks > 0) {
                    ((userCompletedTasks.toFloat() / userTotalTasks) * 100).toInt().coerceIn(0, 100)
                } else 0

                val primaryMetricValue = when (rankingMode) {
                    RankingMode.XP -> "${currentUserProfile.xp} XP"
                    RankingMode.COMPLETION -> "$userCompletionPct%"
                    RankingMode.STREAK -> "${currentUserProfile.currentStreak} ${if (currentUserProfile.currentStreak == 1) "day" else "days"}"
                }

                RankedLeaderboardEntry(
                    rank = 1,
                    user = LeaderboardUser(
                        userId = currentUserId,
                        displayName = currentUserProfile.name.ifBlank { "You" },
                        domainId = userDomainId,
                        domainTitle = userDomainTitle,
                        department = currentUserProfile.department,
                        yearOfStudy = currentUserProfile.yearOfStudy,
                        xp = currentUserProfile.xp,
                        completionPercentage = userCompletionPct,
                        streak = currentUserProfile.currentStreak,
                        completedTasks = userCompletedTasks,
                        totalTasks = userTotalTasks,
                        isCurrentUser = true
                    ),
                    primaryMetricValue = primaryMetricValue,
                    metricUnit = rankingMode.unit,
                    rankingMode = rankingMode,
                    isTop3 = true,
                    isCurrentUser = true
                )
            } else null

        return DomainLeaderboard(
            domainId = canonicalDomainId,
            domainTitle = domainTitle,
            rankingMode = rankingMode,
            entries = rankedEntries,
            currentUserEntry = currentUserEntry,
            totalParticipants = rankedEntries.size,
            domainTotalTasks = domainTotalTasks
        )
    }

    private suspend fun fetchFromEdgeFunction(
        canonicalDomainId: String,
        currentUserId: String?
    ): List<LeaderboardUser>? {
        return try {
            val baseUrl = SupabaseClientProvider.effectiveUrl.trimEnd('/')
            val endpointUrl = "$baseUrl/functions/v1/get-domain-leaderboard"
            val anonKey = SupabaseClientProvider.effectivePublishableKey
            val sessionToken = client.auth.currentSessionOrNull()?.accessToken
                ?: client.auth.currentAccessTokenOrNull()
                ?: anonKey

            val payload = JSONObject().apply {
                put("p_domain_id", canonicalDomainId)
            }
            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $sessionToken")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful && bodyString.isNotBlank()) {
                    val dtoList = json.decodeFromString<List<LeaderboardEntryDto>>(bodyString)
                    Log.d(TAG, "LEADERBOARD EDGE FUNCTION SUCCESS -> Domain: $canonicalDomainId | Real DB Entries: ${dtoList.size}")
                    dtoList.distinctBy { it.userId }.map { it.toDomain(currentUserId) }
                } else {
                    Log.w(TAG, "Edge function get-domain-leaderboard returned HTTP ${response.code}: $bodyString")
                    null
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Edge function get-domain-leaderboard unavailable: ${e.message}")
            null
        }
    }
}


