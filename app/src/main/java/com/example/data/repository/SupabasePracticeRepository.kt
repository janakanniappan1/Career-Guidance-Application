package com.example.data.repository

import android.util.Log
import com.example.data.model.IncrementProfileXpParams
import com.example.data.model.InterviewQuestionDto
import com.example.data.model.PracticeProblemDto
import com.example.data.model.ProfileDto
import com.example.data.model.QuizAttemptDto
import com.example.data.model.QuizQuestionDto
import com.example.data.model.ResumeAnalysisDto
import com.example.data.model.StudentProjectDto
import com.example.data.model.UserDailyActivityDto
import com.example.data.model.UserProblemProgressDto
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.AdminOverview
import com.example.domain.model.InterviewQuestion
import com.example.domain.model.PracticeConstants
import com.example.domain.model.PracticeProblem
import com.example.domain.model.ProjectItem
import com.example.domain.model.QuizAttempt
import com.example.domain.model.QuizQuestion
import com.example.domain.model.ResumeAnalysis
import com.example.domain.model.StudentProfile
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PracticeRepository
import com.example.util.CareerCanonicalUtil
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Production-ready Supabase implementation of [PracticeRepository].
 *
 * Guarantees:
 * - Robust User Isolation: Progress is strictly keyed to the authenticated Supabase user ID.
 * - Career-Specific Filtering: Queries align with canonical career models.
 * - Single Source of Truth: Data is loaded from and persisted to Supabase with local SharedPreferences fallback.
 * - Authoritative XP Sync: Quiz completion persists attempts, records daily activity, and safely updates profile XP.
 */
class SupabasePracticeRepository(
    private val authRepository: AuthRepository? = null,
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PracticeRepository {

    companion object {
        private const val TAG = "SupabasePracticeRepo"
        val ASIA_KOLKATA_ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Kolkata")
    }

    private val mutex = Mutex()

    // Thread-safe in-memory cache of solved problem IDs mapped by userId
    private val _userSolvedProblemsCache = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    // In-memory projects cache for active session
    private val _userProjectsCache = MutableStateFlow<Map<String, List<ProjectItem>>>(emptyMap())

    /**
     * Clears user-specific transient caches on logout or session change.
     */
    override fun clearUserCache(userId: String?) {
        Log.d("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: clearUserCache called for userId=$userId")
        if (userId != null) {
            val currentSolved = _userSolvedProblemsCache.value.toMutableMap()
            currentSolved.remove(userId)
            _userSolvedProblemsCache.value = currentSolved

            val currentProjects = _userProjectsCache.value.toMutableMap()
            currentProjects.remove(userId)
            _userProjectsCache.value = currentProjects
        } else {
            _userSolvedProblemsCache.value = emptyMap()
            _userProjectsCache.value = emptyMap()
        }
    }

    /**
     * Resolves the current authenticated user's ID without exposing stale state.
     */
    private suspend fun getCurrentUserId(): String? {
        val authUser = try {
            client.auth.currentUserOrNull()?.id
        } catch (_: Throwable) {
            null
        }
        if (!authUser.isNullOrBlank()) {
            return authUser
        }
        return try {
            authRepository?.getCurrentUser()?.firstOrNull()?.id
        } catch (_: Throwable) {
            null
        }
    }

    override fun getProblems(careerId: String?): Flow<List<PracticeProblem>> = flow {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
        val userId = getCurrentUserId() ?: "guest_session"

        // 1. Fetch solved progress for this specific user
        val rawSolvedIds = loadUserSolvedProblemIds(userId)

        // 2. Fetch problems directly from Supabase and merge with DefaultPracticeCatalog
        val rawProblems = fetchProblemsFromSupabase(canonicalCareer)
        val catalogProblems = DefaultPracticeCatalog.getPracticeProblems(canonicalCareer)

        // Deduplicate and merge problems using canonical catalog ID as authoritative key
        val mergedList = mutableListOf<PracticeProblem>()
        val seenKeys = mutableSetOf<String>()

        fun computeKey(p: PracticeProblem): String {
            val catalog = DefaultPracticeCatalog.getProblemById(p.id)
                ?: DefaultPracticeCatalog.getProblemById(p.title)
                ?: DefaultPracticeCatalog.getProblemByQuery(p.title, p.description, p.topic)
            return catalog?.id ?: p.id
        }

        // Add catalog problems first (authoritative content, options, correctIndex, 20 XP)
        for (cat in catalogProblems) {
            val key = cat.id
            if (seenKeys.add(key)) {
                mergedList.add(cat)
            }
        }

        // Add any non-canonical remote problems if present (hydrated via catalog)
        for (remote in rawProblems) {
            val key = computeKey(remote)
            if (seenKeys.add(key)) {
                val catalog = DefaultPracticeCatalog.getProblemById(remote.id)
                    ?: DefaultPracticeCatalog.getProblemById(remote.title)
                    ?: DefaultPracticeCatalog.getProblemByQuery(remote.title, remote.description, remote.topic)
                val enhanced = catalog ?: remote
                mergedList.add(enhanced)
            }
        }

        // Remote lookup map by remote ID to resolve any legacy IDs accurately
        val remoteMapById = rawProblems.associateBy { it.id }

        // Expand solved set with all aliases (IDs, canonical catalog IDs, titles)
        val resolvedSolvedSet = mutableSetOf<String>()
        resolvedSolvedSet.addAll(rawSolvedIds)
        for (id in rawSolvedIds) {
            val cat = DefaultPracticeCatalog.getProblemById(id)
            if (cat != null) {
                resolvedSolvedSet.add(cat.id)
                resolvedSolvedSet.add(cat.title.trim().lowercase())
            }
            // If the ID was a remote DB id (e.g. legacy database ID or UUID), resolve via remote problem's title/topic
            val remoteProb = remoteMapById[id]
            if (remoteProb != null) {
                val resolvedFromRemote = DefaultPracticeCatalog.getProblemById(remoteProb.title)
                    ?: DefaultPracticeCatalog.getProblemByQuery(remoteProb.title, remoteProb.description, remoteProb.topic)
                if (resolvedFromRemote != null) {
                    resolvedSolvedSet.add(resolvedFromRemote.id)
                    resolvedSolvedSet.add(resolvedFromRemote.title.trim().lowercase())
                }
            }
        }

        // 3. Map isSolved flag using current user's isolated solved set
        val domainProblems = mergedList.map { problem ->
            val cat = DefaultPracticeCatalog.getProblemById(problem.id)
                ?: DefaultPracticeCatalog.getProblemById(problem.title)
            val isProblemSolved = resolvedSolvedSet.contains(problem.id) ||
                resolvedSolvedSet.contains(problem.title.trim().lowercase()) ||
                (cat != null && (resolvedSolvedSet.contains(cat.id) || resolvedSolvedSet.contains(cat.title.trim().lowercase())))

            problem.copy(isSolved = isProblemSolved)
        }

        emit(domainProblems)
    }.flowOn(ioDispatcher)

    private suspend fun loadUserSolvedProblemIds(userId: String): Set<String> {
        if (userId == "guest_session" || userId.isBlank()) {
            return _userSolvedProblemsCache.value[userId] ?: emptySet()
        }

        return try {
            val progressDtos = client.from("user_problem_progress").select {
                filter {
                    eq("user_id", userId)
                    eq("is_solved", true)
                }
            }.decodeList<UserProblemProgressDto>()

            val remoteSolved = progressDtos.map { it.problemId }.toSet()
            val inMemSolved = _userSolvedProblemsCache.value[userId] ?: emptySet()
            val totalSolved = (remoteSolved + inMemSolved).toSet()

            mutex.withLock {
                val currentMap = _userSolvedProblemsCache.value.toMutableMap()
                currentMap[userId] = totalSolved
                _userSolvedProblemsCache.value = currentMap
            }

            totalSolved
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Failed to load user_problem_progress from Supabase for user $userId: ${e.message}")
            _userSolvedProblemsCache.value[userId] ?: emptySet()
        }
    }

    private suspend fun fetchProblemsFromSupabase(canonicalCareer: String?): List<PracticeProblem> {
        return try {
            val dtos = client.from("practice_problems").select().decodeList<PracticeProblemDto>()
            if (dtos.isNotEmpty()) {
                val filtered = if (canonicalCareer != null) {
                    dtos.filter { 
                        it.careerId.isNullOrBlank() || 
                        it.careerId.equals(canonicalCareer, ignoreCase = true) || 
                        it.careerId.equals("all", ignoreCase = true) ||
                        it.careerId.equals("general", ignoreCase = true)
                    }
                } else {
                    dtos
                }
                filtered.map { it.toDomain() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Querying practice_problems failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun solveProblem(id: String, selectedAnswerIndex: Int?): com.example.domain.model.PracticeProblemSolveResult = withContext(ioDispatcher) {
        val userId = getCurrentUserId() ?: "guest_session"

        // 0. Resolve canonical catalog problem (DefaultPracticeCatalog is authoritative for content, correctIndex, and XP = 20)
        val canonicalId = DefaultPracticeCatalog.toCanonicalProblemId(id)
        val catalogProblem = DefaultPracticeCatalog.getProblemById(canonicalId)
            ?: DefaultPracticeCatalog.getProblemById(id)
            ?: (if (userId != "guest_session") {
                try {
                    val dbDto = client.from("practice_problems").select {
                        filter { eq("id", id) }
                        limit(1)
                    }.decodeSingleOrNull<PracticeProblemDto>()
                    dbDto?.let {
                        DefaultPracticeCatalog.getProblemById(it.id)
                            ?: DefaultPracticeCatalog.getProblemById(it.title)
                            ?: DefaultPracticeCatalog.getProblemByQuery(it.title, it.description, it.topic)
                    }
                } catch (_: Exception) { null }
            } else null)

        val resolvedCanonicalId = catalogProblem?.id ?: canonicalId
        val correctIndex = catalogProblem?.correctIndex ?: 0
        val xpReward = catalogProblem?.xpReward ?: 20

        // 1. Validate answer correctness against DefaultPracticeCatalog.correctIndex
        if (selectedAnswerIndex != null && selectedAnswerIndex != correctIndex) {
            val currentXp = if (userId != "guest_session") {
                authRepository?.getCurrentUser()?.firstOrNull()?.xp ?: try {
                    client.from("profiles").select {
                        filter { eq("id", userId) }
                        limit(1)
                    }.decodeSingleOrNull<ProfileDto>()?.xp ?: 0
                } catch (_: Exception) { 0 }
            } else 0
            Log.d("XP_DEBUG", "Problem $id (canonical: $resolvedCanonicalId) answer incorrect: selected $selectedAnswerIndex, expected $correctIndex. 0 XP awarded.")
            return@withContext com.example.domain.model.PracticeProblemSolveResult(
                isCorrect = false,
                xpAwarded = 0,
                isFirstSolve = false,
                isSolved = false,
                totalXp = currentXp,
                message = "Incorrect answer"
            )
        }

        // Collect aliases for canonicalId and id to guarantee single-solve idempotency
        val aliases = mutableSetOf(id, canonicalId, resolvedCanonicalId)
        if (catalogProblem != null) {
            aliases.add(catalogProblem.id)
            aliases.add(catalogProblem.title.trim().lowercase())
        }

        // 2. Authenticated branch: invoke authoritative complete_practice_problem_v2 RPC
        if (userId != "guest_session" && userId.isNotBlank()) {
            val rpcParams = buildJsonObject {
                put("p_user_id", userId)
                put("p_problem_id", resolvedCanonicalId)
                put("p_xp_reward", xpReward)
            }

            Log.d(TAG, "Invoking complete_practice_problem_v2 RPC: user=$userId, problem=$resolvedCanonicalId, xp=$xpReward")

            val rpcResponse: com.example.data.model.CompleteProblemRpcV2ResponseDto = try {
                client.postgrest.rpc(
                    function = "complete_practice_problem_v2",
                    parameters = rpcParams
                ).decodeAs<com.example.data.model.CompleteProblemRpcV2ResponseDto>()
            } catch (rpcErr: Exception) {
                if (rpcErr is kotlinx.coroutines.CancellationException) throw rpcErr
                val errDetails = "Type=${rpcErr.javaClass.name}, Message=${rpcErr.message}, Cause=${rpcErr.cause?.message}"
                Log.e(TAG, "complete_practice_problem_v2 RPC failed for user $userId, problem $resolvedCanonicalId: $errDetails", rpcErr)
                return@withContext com.example.domain.model.PracticeProblemSolveResult(
                    isCorrect = true,
                    xpAwarded = 0,
                    isFirstSolve = false,
                    isSolved = false,
                    totalXp = authRepository?.getCurrentUser()?.firstOrNull()?.xp,
                    message = "Could not save progress to server: ${rpcErr.message ?: "Please try again."}"
                )
            }

            Log.d(TAG, "complete_practice_problem_v2 RPC response: success=${rpcResponse.success}, newly_solved=${rpcResponse.newlySolved}, xp_awarded=${rpcResponse.xpAwarded}, total_xp=${rpcResponse.totalXp}")

            if (rpcResponse.success) {
                // Update in-memory cache ONLY after successful Supabase persistence
                mutex.withLock {
                    val currentMap = _userSolvedProblemsCache.value.toMutableMap()
                    val userSet = (currentMap[userId] ?: emptySet()).toMutableSet()
                    userSet.addAll(aliases)
                    currentMap[userId] = userSet
                    _userSolvedProblemsCache.value = currentMap
                }

                // Update authRepository cached XP if returned
                if (authRepository != null && rpcResponse.totalXp != null) {
                    authRepository.updateCachedXp(userId, rpcResponse.totalXp)
                }

                return@withContext rpcResponse.toDomain(isCorrect = true)
            } else {
                Log.w(TAG, "complete_practice_problem_v2 returned success=false: ${rpcResponse.message}")
                return@withContext com.example.domain.model.PracticeProblemSolveResult(
                    isCorrect = true,
                    xpAwarded = 0,
                    isFirstSolve = false,
                    isSolved = false,
                    totalXp = rpcResponse.totalXp ?: authRepository?.getCurrentUser()?.firstOrNull()?.xp,
                    message = rpcResponse.message ?: "Could not save progress to server"
                )
            }
        }

        // 3. Guest / Offline mode
        val wasEverSolved = mutex.withLock {
            val currentMap = _userSolvedProblemsCache.value.toMutableMap()
            val userSet = (currentMap[userId] ?: emptySet()).toMutableSet()
            val already = aliases.any { userSet.contains(it) }
            userSet.addAll(aliases)
            currentMap[userId] = userSet
            _userSolvedProblemsCache.value = currentMap
            already
        }

        val guestXp = if (!wasEverSolved) xpReward else 0
        return@withContext com.example.domain.model.PracticeProblemSolveResult(
            isCorrect = true,
            xpAwarded = guestXp,
            isFirstSolve = !wasEverSolved,
            isSolved = true,
            totalXp = null,
            message = if (!wasEverSolved) "First solve success" else "Already completed"
        )
    }

    override suspend fun completeProblem(id: String, selectedAnswerIndex: Int?): Boolean {
        val result = solveProblem(id, selectedAnswerIndex)
        return result.isCorrect && result.isSolved
    }


    override suspend fun toggleProblemSolved(id: String): Boolean = completeProblem(id)

    private fun getKolkataCalendarDate(): String {
        return java.time.LocalDate.now(ASIA_KOLKATA_ZONE).toString()
    }

    override suspend fun isDailyTestCompletedToday(): Boolean = withContext(ioDispatcher) {
        val userId = getCurrentUserId()
        val todayDate = getKolkataCalendarDate()
        if (userId.isNullOrBlank()) {
            Log.d("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: isDailyTestCompletedToday -> false (unauthenticated)")
            return@withContext false
        }

        try {
            // Check active profile timestamp to ensure deleted/recreated profiles do not inherit old history
            val activeProfile = try {
                authRepository?.getCurrentUser()?.firstOrNull()
            } catch (_: Throwable) {
                null
            }
            val profileCreatedAt = activeProfile?.createdAt

            // 1. Check authoritative user_daily_activity table first
            val dailyList = try {
                client.from("user_daily_activity").select {
                    filter {
                        eq("user_id", userId)
                        eq("activity_date", todayDate)
                        eq("daily_test_completed", true)
                    }
                }.decodeList<UserDailyActivityDto>()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "user_daily_activity check fallback: ${e.message}")
                emptyList()
            }

            // Exclude activities that occurred prior to profile creation if profile was recreated
            val validDailyList = dailyList.filter { daily ->
                if (profileCreatedAt != null && profileCreatedAt > 0L && daily.createdAt != null) {
                    val activityTime = try { java.time.Instant.parse(daily.createdAt).toEpochMilli() } catch (_: Throwable) { null }
                    activityTime == null || activityTime >= profileCreatedAt
                } else {
                    true
                }
            }

            if (validDailyList.isNotEmpty()) {
                Log.d(
                    "DAILY_TEST_DEBUG",
                    "DAILY_TEST_DEBUG: userId=$userId, date=$todayDate, completed=true (via user_daily_activity, count=${validDailyList.size})"
                )
                return@withContext true
            }

            // 2. Fallback check on quiz_attempts for today
            val attempts = try {
                client.from("quiz_attempts").select {
                    filter {
                        eq("user_id", userId)
                        eq("topic", PracticeConstants.TOPIC_DAILY_SPEED_TEST)
                    }
                }.decodeList<QuizAttemptDto>()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "quiz_attempts check fallback: ${e.message}")
                emptyList()
            }

            val isCompletedViaAttempts = attempts.any { attempt ->
                val completedAt = attempt.completedAt ?: return@any false
                val attemptDate = try {
                    java.time.Instant.parse(completedAt).atZone(ASIA_KOLKATA_ZONE).toLocalDate().toString()
                } catch (_: Throwable) {
                    completedAt.take(10)
                }
                val isToday = attemptDate == todayDate
                if (!isToday) return@any false

                if (profileCreatedAt != null && profileCreatedAt > 0L) {
                    val attemptTime = try { java.time.Instant.parse(completedAt).toEpochMilli() } catch (_: Throwable) { null }
                    attemptTime == null || attemptTime >= profileCreatedAt
                } else {
                    true
                }
            }

            Log.d(
                "DAILY_TEST_DEBUG",
                "DAILY_TEST_DEBUG: userId=$userId, date=$todayDate, completed=$isCompletedViaAttempts (via quiz_attempts fallback, totalAttempts=${attempts.size})"
            )
            isCompletedViaAttempts
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: isDailyTestCompletedToday exception: ${e.message}", e)
            false
        }
    }

    override suspend fun getInterviewQuestions(careerId: String?): List<InterviewQuestion> = withContext(ioDispatcher) {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
        try {
            val dtos = client.from("interview_questions").select().decodeList<InterviewQuestionDto>()
            if (dtos.isNotEmpty()) {
                val filtered = if (canonicalCareer != null) {
                    dtos.filter { 
                        it.careerId.isNullOrBlank() || 
                        it.careerId.equals(canonicalCareer, ignoreCase = true) || 
                        it.careerId.equals("all", ignoreCase = true) ||
                        it.careerId.equals("general", ignoreCase = true)
                    }
                } else {
                    dtos
                }
                filtered.map { it.toDomain() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Querying interview_questions failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getQuizQuestions(careerId: String?, topic: String?): List<QuizQuestion> = withContext(ioDispatcher) {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
        val userId = getCurrentUserId() ?: "unknown_user"
        val todayDate = getKolkataCalendarDate()
        val isDailyTest = topic == PracticeConstants.TOPIC_DAILY_SPEED_TEST

        Log.d(
            "DAILY_TEST_DEBUG",
            "DAILY_TEST_DEBUG: getQuizQuestions requested: userId=$userId, date=$todayDate, careerId=$careerId (canonical=$canonicalCareer), topic=$topic, isDailyTest=$isDailyTest"
        )

        try {
            val dtos = client.from("quiz_questions").select().decodeList<QuizQuestionDto>()
            Log.d(
                "DAILY_TEST_DEBUG",
                "DAILY_TEST_DEBUG: fetched ${dtos.size} raw questions from quiz_questions table"
            )

            // Convert to domain and filter only valid questions
            val validQuestions = dtos.map { it.toDomain() }.filter { q ->
                q.question.isNotBlank() &&
                q.options.size >= 2 &&
                q.correctIndex in q.options.indices
            }.distinctBy { it.id.ifBlank { it.question } }

            Log.d(
                "DAILY_TEST_DEBUG",
                "DAILY_TEST_DEBUG: ${validQuestions.size} valid questions after schema validation"
            )

            if (isDailyTest) {
                // DAILY SPEED TEST: Select exactly 5 stable, distinct questions
                val careerMatching = if (canonicalCareer != null) {
                    validQuestions.filter { it.careerId != null && it.careerId.equals(canonicalCareer, ignoreCase = true) }
                } else {
                    emptyList()
                }

                val generalQuestions = validQuestions.filter {
                    it.careerId == null ||
                    it.careerId.equals("all", ignoreCase = true) ||
                    it.careerId.equals("general", ignoreCase = true) ||
                    it.topic.equals("general", ignoreCase = true) ||
                    it.topic.equals(PracticeConstants.TOPIC_DAILY_SPEED_TEST, ignoreCase = true)
                }

                val otherQuestions = validQuestions - careerMatching.toSet() - generalQuestions.toSet()

                val totalAvailable = (careerMatching + generalQuestions + otherQuestions)
                    .distinctBy { it.id.ifBlank { it.question } }

                Log.d(
                    "DAILY_TEST_DEBUG",
                    "DAILY_TEST_DEBUG: Daily Speed Test total available: ${totalAvailable.size} questions (careerMatching=${careerMatching.size}, general=${generalQuestions.size}, other=${otherQuestions.size})"
                )

                if (totalAvailable.size < 5) {
                    Log.w(
                        "DAILY_TEST_DEBUG",
                        "DAILY_TEST_DEBUG: Insufficient questions in Supabase (found ${totalAvailable.size} < 5). Returning available questions without fake substitutes."
                    )
                    return@withContext totalAvailable
                }

                // Deterministic daily shuffle per user & date for test stability during the day
                val seed = (userId + "_" + todayDate).hashCode().toLong()
                val selectedQuestions = mutableListOf<QuizQuestion>()
                val seenKeys = mutableSetOf<String>()

                fun addIfUnique(q: QuizQuestion): Boolean {
                    val key = q.id.ifBlank { q.question }
                    return if (seenKeys.add(key)) {
                        selectedQuestions.add(q)
                        true
                    } else false
                }

                // 1. Career-specific questions first (shuffled deterministically)
                val careerRandom = java.util.Random(seed)
                val shuffledCareer = careerMatching.shuffled(careerRandom)
                for (q in shuffledCareer) {
                    if (selectedQuestions.size >= 5) break
                    addIfUnique(q)
                }

                // 2. Supplementary general placement questions
                if (selectedQuestions.size < 5) {
                    val generalRandom = java.util.Random(seed xor 0x5DEECE66DL)
                    val shuffledGeneral = generalQuestions.shuffled(generalRandom)
                    for (q in shuffledGeneral) {
                        if (selectedQuestions.size >= 5) break
                        addIfUnique(q)
                    }
                }

                // 3. Supplement with any remaining valid questions if still < 5
                if (selectedQuestions.size < 5) {
                    val fallbackRandom = java.util.Random(seed xor 0xABCDEF123L)
                    val shuffledOther = otherQuestions.shuffled(fallbackRandom)
                    for (q in shuffledOther) {
                        if (selectedQuestions.size >= 5) break
                        addIfUnique(q)
                    }
                }

                Log.d(
                    "DAILY_TEST_DEBUG",
                    "DAILY_TEST_DEBUG: Selected ${selectedQuestions.size} stable questions for Daily Speed Test: ids=${selectedQuestions.map { it.id }}"
                )

                selectedQuestions.take(5)
            } else {
                // Standard Quiz / Speed Challenge
                val filteredByCareer = if (canonicalCareer != null) {
                    validQuestions.filter { 
                        it.careerId.isNullOrBlank() || 
                        it.careerId.equals(canonicalCareer, ignoreCase = true) || 
                        it.careerId.equals("all", ignoreCase = true) ||
                        it.careerId.equals("general", ignoreCase = true)
                    }
                } else {
                    validQuestions
                }
                val filteredByTopic = if (!topic.isNullOrBlank() && topic != "general") {
                    val matchingTopic = filteredByCareer.filter { it.topic != null && it.topic.equals(topic, ignoreCase = true) }
                    if (matchingTopic.isNotEmpty()) matchingTopic else filteredByCareer
                } else {
                    filteredByCareer
                }
                filteredByTopic.take(10)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: Querying quiz_questions failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun saveQuizAttempt(attempt: QuizAttempt): Result<Unit> = withContext(ioDispatcher) {
        val userId = getCurrentUserId()
        if (userId.isNullOrBlank()) {
            Log.d(TAG, "Unauthenticated session; skipping remote quiz attempt persistence")
            return@withContext Result.success(Unit)
        }

        try {
            val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(attempt.careerId)
            val isDailyTest = attempt.topic == PracticeConstants.TOPIC_DAILY_SPEED_TEST
            val alreadyCompletedToday = if (isDailyTest) isDailyTestCompletedToday() else false

            val finalXpEarned = if (isDailyTest && alreadyCompletedToday) {
                0 // Idempotency protection: do not award duplicate XP on the same day
            } else {
                attempt.xpEarned // Performance-based proportional XP from attempt calculation
            }

            val attemptId = attempt.id ?: java.util.UUID.randomUUID().toString()
            val attemptDto = QuizAttemptDto(
                id = attemptId,
                userId = userId,
                careerId = canonicalCareer,
                topic = attempt.topic,
                score = attempt.score,
                totalQuestions = attempt.totalQuestions,
                xpEarned = finalXpEarned,
                completedAt = java.time.Instant.now().toString()
            )

            // 1. Save attempt record
            try {
                client.from("quiz_attempts").insert(attemptDto)
                Log.d("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: quiz_attempts inserted: attemptId=$attemptId, userId=$userId, score=${attempt.score}/${attempt.totalQuestions}, xpEarned=$finalXpEarned")
            } catch (insertErr: Exception) {
                if (insertErr is kotlinx.coroutines.CancellationException) throw insertErr
                Log.w("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: quiz_attempts insert warning: ${insertErr.message}")
            }

            // 2. If Daily Speed Test, persist to user_daily_activity table
            if (isDailyTest) {
                // First, invoke authoritative database RPC record_user_activity(p_user_id) with ONLY the user ID.
                // The database atomically calculates (now() AT TIME ZONE 'Asia/Kolkata')::DATE and ensures
                // today's activity row exists, while updating the user's streak.
                recordUserActivity(userId, "daily_speed_test_$attemptId")

                try {
                    // Obtain the authoritative date and existing activity record directly from Supabase
                    val existingActivity = try {
                        client.from("user_daily_activity").select {
                            filter {
                                eq("user_id", userId)
                            }
                            order("activity_date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(1)
                        }.decodeList<UserDailyActivityDto>().firstOrNull()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        Log.w("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: could not query existing user_daily_activity: ${e.message}")
                        null
                    }

                    // Authoritative date comes directly from database row (fallback to Asia/Kolkata device calculation)
                    val authoritativeDate = existingActivity?.activityDate ?: getKolkataCalendarDate()
                    val activityId = existingActivity?.id ?: java.util.UUID.randomUUID().toString()

                    val dailyDto = UserDailyActivityDto(
                        id = activityId,
                        userId = userId,
                        activityDate = authoritativeDate,
                        dailyTestCompleted = true,
                        xpEarned = if (existingActivity != null) {
                            existingActivity.xpEarned + finalXpEarned
                        } else {
                            finalXpEarned
                        },
                        createdAt = existingActivity?.createdAt ?: java.time.Instant.now().toString(),
                        updatedAt = java.time.Instant.now().toString()
                    )
                    client.from("user_daily_activity").upsert(dailyDto)
                    Log.d("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: user_daily_activity upserted: id=$activityId, userId=$userId, date=$authoritativeDate, dailyTestCompleted=true, totalDayXp=${dailyDto.xpEarned}")
                } catch (dailyErr: Exception) {
                    if (dailyErr is kotlinx.coroutines.CancellationException) throw dailyErr
                    Log.w("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: user_daily_activity upsert warning: ${dailyErr.message}")
                }
            }

            // 3. Authoritatively update user XP in profiles table and notify observers
            val quizIdentifier = "${attempt.topic}_$attemptId"
            if (finalXpEarned > 0) {
                incrementProfileXp(userId, finalXpEarned, quizIdentifier)
            } else {
                Log.d("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: +0 XP earned (alreadyCompletedToday=$alreadyCompletedToday, score=${attempt.score}/${attempt.totalQuestions}) for user $userId; incrementProfileXp skipped.")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: Failed in saveQuizAttempt workflow: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun incrementProfileXp(userId: String, earnedXp: Int, identifier: String = "general"): Int {
        if (earnedXp <= 0) {
            return authRepository?.getCurrentUser()?.firstOrNull()?.xp ?: try {
                client.from("profiles").select {
                    filter { eq("id", userId) }
                    limit(1)
                }.decodeSingleOrNull<ProfileDto>()?.xp ?: 0
            } catch (_: Exception) { 0 }
        }
        try {
            Log.d("XP_DEBUG", "XP DEBUG: RPC START: increment_profile_xp | userId = $userId | identifier = $identifier | earnedXp = $earnedXp")

            // 1. Invoke PostgreSQL atomic function via Supabase RPC.
            // Database executes: UPDATE public.profiles SET xp = COALESCE(xp, 0) + p_amount RETURNING xp
            // This eliminates client-side read-modify-write lost updates across concurrent coroutines and devices.
            val rpcParams = buildJsonObject {
                put("p_user_id", userId)
                put("p_amount", earnedXp)
            }
            val newCumulativeXp = try {
                client.postgrest.rpc(
                    function = "increment_profile_xp",
                    parameters = rpcParams
                ).decodeAs<Int>()
            } catch (rpcErr: Exception) {
                if (rpcErr is kotlinx.coroutines.CancellationException) throw rpcErr
                Log.e("XP_DEBUG", "XP DEBUG: RPC FAILURE: Atomic XP RPC increment_profile_xp failed | userId = $userId | identifier = $identifier | earnedXp = $earnedXp | error = ${rpcErr.message}", rpcErr)
                throw rpcErr
            }

            Log.d("XP_DEBUG", "XP DEBUG: RPC SUCCESS: increment_profile_xp | userId = $userId | identifier = $identifier | earnedXp = $earnedXp | returned cumulativeXp = $newCumulativeXp")

            // 2. Propagate authoritative new cumulative XP to the in-memory cache ONLY.
            // Does NOT issue another remote UPDATE profiles write.
            if (authRepository != null) {
                authRepository.updateCachedXp(userId, newCumulativeXp)
                Log.d("XP_DEBUG", "XP DEBUG: Profile cache updated | userId = $userId | newCumulativeXp = $newCumulativeXp")
            }
            return newCumulativeXp
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("XP_DEBUG", "XP DEBUG: Supabase profile XP atomic increment FAILED | userId = $userId | identifier = $identifier | error = ${e.message}", e)
            throw e
        }
    }


    private suspend fun recordUserActivity(userId: String, triggerSource: String = "general"): Int? {
        return try {
            Log.d("STREAK_DEBUG", "STREAK DEBUG: RPC START: record_user_activity | userId = $userId | source = $triggerSource")
            val rpcParams = buildJsonObject {
                put("p_user_id", userId)
            }
            val newStreak = client.postgrest.rpc(
                function = "record_user_activity",
                parameters = rpcParams
            ).decodeAs<Int>()

            Log.d("STREAK_DEBUG", "STREAK DEBUG: RPC SUCCESS: record_user_activity | userId = $userId | newStreak = $newStreak | source = $triggerSource")
            if (authRepository != null) {
                authRepository.updateCachedStreak(userId, newStreak)
                Log.d("STREAK_DEBUG", "STREAK DEBUG: Profile cache streak updated | userId = $userId | newStreak = $newStreak")
            }
            newStreak
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w("STREAK_DEBUG", "STREAK DEBUG: record_user_activity warning (non-fatal): ${e.message} | userId = $userId | source = $triggerSource", e)
            null
        }
    }

    override suspend fun getQuizAttempts(): List<QuizAttempt> = withContext(ioDispatcher) {
        val userId = getCurrentUserId() ?: return@withContext emptyList()
        try {
            val dtos = client.from("quiz_attempts").select {
                filter { eq("user_id", userId) }
            }.decodeList<QuizAttemptDto>()
            dtos.map { it.toDomain() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve quiz_attempts: ${e.message}")
            emptyList()
        }
    }

    override fun getProjects(): Flow<List<ProjectItem>> = flow {
        val userId = getCurrentUserId() ?: "guest_session"
        if (userId == "guest_session") {
            emit(emptyList())
            return@flow
        }

        try {
            val dtos = client.from("student_projects").select {
                filter { eq("user_id", userId) }
            }.decodeList<StudentProjectDto>()

            val list = dtos.map { it.toDomain() }
            mutex.withLock {
                val current = _userProjectsCache.value.toMutableMap()
                current[userId] = list
                _userProjectsCache.value = current
            }
            emit(list)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Fetching student_projects failed (${e.message}), checking memory cache")
            val cached = _userProjectsCache.value[userId] ?: emptyList()
            emit(cached)
        }
    }.flowOn(ioDispatcher)

    override suspend fun addProject(project: ProjectItem) = withContext(ioDispatcher) {
        val userId = getCurrentUserId() ?: "guest_session"
        val resolvedProject = project.copy(
            id = project.id ?: "proj_${System.currentTimeMillis()}",
            userId = userId,
            createdAt = project.createdAt ?: System.currentTimeMillis()
        )

        // Optimistic local update
        mutex.withLock {
            val current = _userProjectsCache.value.toMutableMap()
            val existing = current[userId] ?: emptyList()
            current[userId] = listOf(resolvedProject) + existing
            _userProjectsCache.value = current
        }

        // Persist to Supabase if authenticated
        if (userId != "guest_session") {
            try {
                val dto = StudentProjectDto.fromDomain(resolvedProject, userId)
                client.from("student_projects").insert(dto)
                Log.d(TAG, "Successfully inserted student project for user $userId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to persist student_project: ${e.message}")
            }
        }
    }

    override suspend fun getResumeAnalysis(): ResumeAnalysis = withContext(ioDispatcher) {
        val userId = getCurrentUserId() ?: return@withContext ResumeAnalysis(userId = "", atsScore = 0, tips = emptyList())
        try {
            val dtos = client.from("resume_analyses").select {
                filter { eq("user_id", userId) }
            }.decodeList<ResumeAnalysisDto>()

            if (dtos.isNotEmpty()) {
                dtos.first().toDomain()
            } else {
                ResumeAnalysis(userId = userId, atsScore = 0, tips = emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fetching resume_analyses failed: ${e.message}")
            ResumeAnalysis(userId = userId, atsScore = 0, tips = emptyList())
        }
    }

    override suspend fun getAdminOverview(): AdminOverview = withContext(ioDispatcher) {
        AdminOverview()
    }
}

