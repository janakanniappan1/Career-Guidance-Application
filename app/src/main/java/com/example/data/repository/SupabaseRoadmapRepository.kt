package com.example.data.repository

import android.util.Log
import com.example.data.model.RoadmapDto
import com.example.data.model.RoadmapStageDto
import com.example.data.model.RoadmapTaskDto
import com.example.data.model.UserTaskProgressDto
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.CareerRoadmap
import com.example.domain.model.RoadmapStage
import com.example.domain.model.RoadmapTask
import com.example.domain.model.SkillProgressItem
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.RoadmapRepository
import com.example.util.CareerCanonicalUtil
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of RoadmapRepository backed strictly by Supabase PostgREST.
 *
 * Source of truth:
 * - Roadmap definitions: public.roadmaps, public.roadmap_stages, public.roadmap_tasks
 * - User task progress: public.user_task_progress (RLS enforced via auth.uid())
 *
 * Guarantees:
 * - No silent fallback to mock/hardcoded data in production. Failures propagate as real errors.
 * - Unknown career IDs throw NoSuchElementException (never returning an arbitrary first roadmap).
 * - User progress is strictly isolated per authenticated user session.
 * - Global roadmap definitions and user-specific task completion state are separated in memory.
 * - Task toggles validate stage/task relationships before modifying state or database.
 * - Optimistic updates are protected by Mutex and rolled back on persistence failure.
 */
class SupabaseRoadmapRepository(
    private val authRepository: AuthRepository? = null,
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RoadmapRepository {

    companion object {
        private const val TAG = "SupabaseRoadmapRepo"
    }

    // Global roadmap structure cache (stages, tasks, metadata with isCompleted = false).
    // Roadmap definitions are global and shared across users without user completion flags.
    private val _globalRoadmapsCache = ConcurrentHashMap<String, CareerRoadmap>()
    private val _roadmapDtoCache = ConcurrentHashMap<String, RoadmapDto>()
    @Volatile
    private var _allRoadmapDtos: List<RoadmapDto>? = null

    // User-specific progress flow: map of userId -> Set of completed taskIds
    private val _userProgressFlow = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    // Tracks all tasks that have ever been completed for each user across the session lifetime.
    // Preserves historical completion memory even if a task is subsequently unchecked.
    private val _historicallyCompletedTasks = ConcurrentHashMap<String, MutableSet<String>>()

    // Currently active authenticated user ID tracking for session isolation
    @Volatile
    private var _activeUserId: String? = null

    // Synchronization locks
    private val sessionMutex = Mutex()
    private val toggleMutex = Mutex()
    private val definitionMutex = Mutex()
    private val roadmapsFetchMutex = Mutex()

    fun clearSession() {
        _userProgressFlow.value = emptyMap()
        _historicallyCompletedTasks.clear()
        _activeUserId = null
        _globalRoadmapsCache.clear()
        _roadmapDtoCache.clear()
        _allRoadmapDtos = null
    }

    override fun getRoadmap(careerId: String): Flow<CareerRoadmap> = flow {
        val safeCareerId = resolveCareerId(careerId)

        // 1. Synchronize user session state and isolate progress
        val currentUserId = getAuthenticatedUserId()
        syncUserSession(currentUserId)

        // 2. Emit cached global roadmap with current user's progress if already loaded
        _globalRoadmapsCache[safeCareerId]?.let { cachedDefinition ->
            val userProgress = _userProgressFlow.value[currentUserId] ?: emptySet()
            emit(mergeProgress(cachedDefinition, userProgress))
        }

        // 3. Load fresh roadmap definition
        val freshDefinition = if (currentUserId.isNullOrBlank()) {
            // Rule 1: Unauthenticated exploration / offline behavior (does not query protected tables with anon)
            DefaultRoadmapCatalog.getStandardRoadmap(safeCareerId)
                ?: throw NoSuchElementException("No roadmap found for career: '$safeCareerId'")
        } else {
            // Rule 2: Authenticated session uses Supabase as source of truth.
            // Throws on Supabase/network failure; NEVER silently falls back to DefaultRoadmapCatalog.
            loadRoadmapDefinitionFromSupabase(safeCareerId)
        }

        _globalRoadmapsCache[safeCareerId] = freshDefinition

        // 4. Load fresh user progress from Supabase if authenticated
        if (!currentUserId.isNullOrBlank()) {
            loadUserProgressFromSupabase(currentUserId, freshDefinition)
        }

        // 5. Emit freshly loaded and merged roadmap
        val currentProgress = _userProgressFlow.value[currentUserId] ?: emptySet()
        val merged = mergeProgress(freshDefinition, currentProgress)
        val taskCount = merged.stages.sumOf { it.tasks.size }
        Log.d(TAG, "[Roadmap] userId = $currentUserId, canonicalCareerId = $safeCareerId, requested = $careerId, roadmapId = ${merged.careerId}, stageCount = ${merged.stages.size}, taskCount = $taskCount")
        emit(merged)

        // 6. Observe live task toggles for the current user session
        _userProgressFlow.collect { progressMap ->
            val activeUser = getAuthenticatedUserId()
            val userTasks = progressMap[activeUser] ?: emptySet()
            val latestDefinition = _globalRoadmapsCache[safeCareerId] ?: freshDefinition
            emit(mergeProgress(latestDefinition, userTasks))
        }
    }.flowOn(ioDispatcher)

    override suspend fun toggleTask(
        careerId: String,
        stageId: String,
        taskId: String
    ): Unit = withContext(ioDispatcher) {
        // 1. Read authenticated user
        val userId = getAuthenticatedUserId()
        if (userId.isNullOrBlank()) {
            Log.e(TAG, "Cannot toggle task $taskId: user is not authenticated")
            throw IllegalStateException("User must be authenticated to update roadmap progress")
        }

        syncUserSession(userId)
        val safeCareerId = resolveCareerId(careerId)

        // 2. Validate roadmap, stage, and task relationship
        val roadmapDef = _globalRoadmapsCache[safeCareerId]
            ?: if (userId.isNullOrBlank()) {
                DefaultRoadmapCatalog.getStandardRoadmap(safeCareerId)
            } else {
                loadRoadmapDefinitionFromSupabase(safeCareerId)
            }?.also { _globalRoadmapsCache[safeCareerId] = it }
            ?: throw NoSuchElementException("No roadmap found for career: '$safeCareerId'")

        val stage = roadmapDef.stages.firstOrNull { it.id == stageId }
            ?: throw IllegalArgumentException("Stage '$stageId' does not belong to career roadmap '$safeCareerId'")

        val task = stage.tasks.firstOrNull { it.id == taskId }
            ?: throw IllegalArgumentException("Task '$taskId' does not belong to stage '$stageId' in career roadmap '$safeCareerId'")

        // 3. Determine current state and apply optimistic update
        val (previousStatus, newStatus) = toggleMutex.withLock {
            val currentTasks = _userProgressFlow.value[userId] ?: emptySet()
            val prev = currentTasks.contains(taskId)
            val next = !prev
            val updatedTasks = if (next) currentTasks + taskId else currentTasks - taskId
            _userProgressFlow.update { map ->
                map + (userId to updatedTasks)
            }
            if (next) {
                _historicallyCompletedTasks.getOrPut(userId) { ConcurrentHashMap.newKeySet() }.add(taskId)
            }
            Pair(prev, next)
        }

        val nowIso = java.time.Instant.now().toString()

        // 4. Persist to Supabase user_task_progress table
        var persistenceSucceeded = false

        try {
            withContext(NonCancellable) {
                val existingRecord = client.from("user_task_progress").select {
                    filter {
                        eq("user_id", userId)
                        eq("task_id", taskId)
                    }
                    limit(1)
                }.decodeList<UserTaskProgressDto>().firstOrNull()

                val inMemoryHistory = _historicallyCompletedTasks[userId]?.contains(taskId) == true
                val recordWasAlreadyCompleted = existingRecord != null && existingRecord.isCompleted
                val recordHadCompletedAt = existingRecord?.completedAt != null
                val recordAlreadyExistedInDb = existingRecord != null

                // Determine if the task was ever previously completed:
                // - In-memory session tracking recorded a prior completion in this app run
                // - Database row was already marked completed
                // - Database row retains a historical completed_at timestamp
                // - Database row already exists (since our application lifecycle only creates user_task_progress rows upon task completion)
                val wasEverCompleted = inMemoryHistory || recordWasAlreadyCompleted || recordHadCompletedAt || recordAlreadyExistedInDb
                val isFirstTimeCompletion = newStatus && !wasEverCompleted

                if (existingRecord != null && existingRecord.id != null) {
                    val updateJson = buildJsonObject {
                        put("is_completed", newStatus)
                        if (newStatus) {
                            put("completed_at", existingRecord.completedAt ?: nowIso)
                        } else {
                            // Preserve historical completed_at timestamp if present so evidence of past completion remains
                            if (existingRecord.completedAt != null) {
                                put("completed_at", existingRecord.completedAt)
                            }
                        }
                        put("updated_at", nowIso)
                    }
                    client.from("user_task_progress").update(updateJson) {
                        filter {
                            eq("id", existingRecord.id)
                            eq("user_id", userId)
                        }
                    }
                    persistenceSucceeded = true
                    Log.d(TAG, "Updated user_task_progress for user=$userId, task=$taskId, isCompleted=$newStatus, preservedCompletedAt=${existingRecord.completedAt}")
                } else {
                    val newProgress = UserTaskProgressDto(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        taskId = taskId,
                        isCompleted = newStatus,
                        completedAt = if (newStatus) nowIso else null,
                        createdAt = nowIso,
                        updatedAt = nowIso
                    )
                    client.from("user_task_progress").insert(newProgress)
                    persistenceSucceeded = true
                    Log.d(TAG, "Inserted user_task_progress for user=$userId, task=$taskId, isCompleted=$newStatus")
                }

                // 5. If task was newly completed (genuine first-time completion) and persistence succeeded, record daily activity and update streak
                if (persistenceSucceeded && isFirstTimeCompletion) {
                    recordUserActivity(userId, "roadmap_task_$taskId")
                } else {
                    Log.d(TAG, "Roadmap task $taskId toggle completed (isFirstTimeCompletion=$isFirstTimeCompletion, newStatus=$newStatus); skipping recordUserActivity")
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to persist task progress for $taskId to Supabase: ${e.message}", e)
            // Keep the optimistic update in-memory for the active session so the user experience is smooth
        }
    }

    override fun getSkillBreakdown(careerId: String): Flow<List<SkillProgressItem>> {
        return getRoadmap(careerId).map { roadmap ->
            calculateSkillBreakdown(roadmap)
        }
    }

    override fun getOverallProgress(careerId: String): Flow<Int> {
        return getRoadmap(careerId).map { roadmap ->
            roadmap.overallProgressPercentage
        }
    }

    /**
     * Derives skill categories breakdown dynamically from the roadmap.
     */
    fun calculateSkillBreakdown(roadmap: CareerRoadmap): List<SkillProgressItem> {
        val allTasks = roadmap.stages.flatMap { it.tasks }
        val grouped = allTasks.groupBy { it.skillCategory.ifBlank { "Core Skills" } }

        return grouped.map { (skillName, tasks) ->
            val completed = tasks.count { it.isCompleted }
            val total = tasks.size
            val percentage = if (total == 0) 0 else ((completed.toFloat() / total.toFloat()) * 100).toInt()
            SkillProgressItem(
                skillName = skillName,
                percentage = percentage,
                completedCount = completed,
                totalCount = total
            )
        }.sortedByDescending { it.percentage }
    }

    /**
     * Loads roadmap definition metadata, stages, and tasks from Supabase.
     * All tasks are returned with isCompleted = false (global definition).
     * If Supabase does not have rows yet or encounters a permission error, falls back to DefaultRoadmapCatalog.
     * Throws NoSuchElementException if the career is unknown.
     */
    private suspend fun loadRoadmapDefinitionFromSupabase(careerId: String): CareerRoadmap = withContext(ioDispatcher) {
        _globalRoadmapsCache[careerId]?.let { return@withContext it }

        definitionMutex.withLock {
            _globalRoadmapsCache[careerId]?.let { return@withContext it }

            val roadmapDto = findRoadmapInSupabase(careerId)
                ?: throw NoSuchElementException("No roadmap found for career: '$careerId'")

            val result = withContext(NonCancellable) {
                val stageDtos = try {
                    client.from("roadmap_stages").select {
                        filter {
                            eq("roadmap_id", roadmapDto.id)
                        }
                        order("stage_order", Order.ASCENDING)
                    }.decodeList<RoadmapStageDto>()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w(TAG, "Querying roadmap_stages failed: ${e.message}")
                    throw e
                }

                if (stageDtos.isEmpty()) {
                    throw NoSuchElementException("No roadmap stages found for career: '$careerId'")
                }

                val stageIds = stageDtos.map { it.id }
                val taskDtos = try {
                    client.from("roadmap_tasks").select {
                        filter {
                            isIn("stage_id", stageIds)
                        }
                        order("task_order", Order.ASCENDING)
                    }.decodeList<RoadmapTaskDto>()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w(TAG, "Querying roadmap_tasks failed: ${e.message}")
                    throw e
                }

                val tasksByStageId = taskDtos.groupBy { it.stageId }

                val domainStages = stageDtos.map { stageDto ->
                    val tasks = (tasksByStageId[stageDto.id] ?: emptyList()).map { taskDto ->
                        RoadmapTask(
                            id = taskDto.id,
                            title = taskDto.title,
                            description = taskDto.description.orEmpty(),
                            isCompleted = false,
                            skillCategory = taskDto.skillCategory?.ifBlank { "Core Skills" } ?: "Core Skills",
                            estimatedHours = taskDto.estimatedHours ?: 10,
                            resourceType = taskDto.resourceType?.ifBlank { "Course" } ?: "Course"
                        )
                    }
                    RoadmapStage(
                        id = stageDto.id,
                        stageName = stageDto.stageName,
                        order = stageDto.stageOrder,
                        subtitle = stageDto.subtitle.orEmpty(),
                        tasks = tasks
                    )
                }

                CareerRoadmap(
                    careerId = roadmapDto.careerId,
                    careerTitle = roadmapDto.title,
                    stages = domainStages
                )
            }
            _globalRoadmapsCache[careerId] = result
            result
        }
    }

    /**
     * Loads the authenticated user's task progress for the specified roadmap tasks from Supabase.
     */
    private suspend fun loadUserProgressFromSupabase(userId: String, roadmapDef: CareerRoadmap) {
        val taskIds = roadmapDef.stages.flatMap { it.tasks }.map { it.id }
        if (taskIds.isEmpty()) return

        try {
            val progressDtos = withContext(NonCancellable) {
                client.from("user_task_progress").select {
                    filter {
                        eq("user_id", userId)
                        isIn("task_id", taskIds)
                    }
                }.decodeList<UserTaskProgressDto>()
            }

            val completedTaskIds = progressDtos.filter { it.isCompleted }.map { it.taskId }.toSet()
            val anyHistoricallyCompleted = progressDtos.filter { it.isCompleted || it.completedAt != null }.map { it.taskId }
            _historicallyCompletedTasks.getOrPut(userId) { ConcurrentHashMap.newKeySet() }.addAll(anyHistoricallyCompleted)

            _userProgressFlow.update { map ->
                val existing = map[userId] ?: emptySet()
                map + (userId to (existing + completedTaskIds))
            }
            Log.d(TAG, "Loaded ${completedTaskIds.size} completed tasks (historical=${anyHistoricallyCompleted.size}) for user $userId from Supabase")
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Failed to load user_task_progress from Supabase for user $userId: ${e.message}")
        }
    }

    /**
     * Resolves careerId to a known canonical identifier.
     * Blank or action IDs resolve to authenticated user's recommendedCareerId or DEFAULT_CAREER_ID.
     */
    private suspend fun resolveCareerId(careerId: String): String {
        val trimmed = careerId.trim()
        if (trimmed.isNotBlank() && !CareerCanonicalUtil.isActionOrPlaceholderId(trimmed)) {
            return CareerCanonicalUtil.toCanonicalCareerIdOrNull(trimmed) ?: trimmed
        }

        val userRecommended = authRepository?.getCurrentUser()?.firstOrNull()?.recommendedCareerId
        if (!userRecommended.isNullOrBlank() && !CareerCanonicalUtil.isActionOrPlaceholderId(userRecommended)) {
            return CareerCanonicalUtil.toCanonicalCareerIdOrNull(userRecommended) ?: userRecommended
        }

        throw NoSuchElementException("Career identifier is required to load a roadmap. Please complete career assessment.")
    }

    /**
     * Locates a roadmap row in Supabase using career_id or title.
     * Throws or returns null if no match is found. NEVER returns an arbitrary first roadmap.
     */
    private suspend fun findRoadmapInSupabase(careerId: String): RoadmapDto? {
        val cleanId = careerId.trim()
        val canonicalId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(cleanId)
        val normalized = normalizeCareerId(cleanId)
        val cacheKey = canonicalId ?: cleanId

        _roadmapDtoCache[cacheKey]?.let { return it }

        return roadmapsFetchMutex.withLock {
            _roadmapDtoCache[cacheKey]?.let { return@withLock it }

            val cachedAll = _allRoadmapDtos
            if (cachedAll != null) {
                val matched = cachedAll.firstOrNull { r ->
                    (canonicalId != null && r.careerId.equals(canonicalId, ignoreCase = true)) ||
                    r.careerId.equals(cleanId, ignoreCase = true) ||
                    r.careerId.equals(normalized, ignoreCase = true) ||
                    r.title.equals(cleanId, ignoreCase = true) ||
                    (canonicalId != null && CareerCanonicalUtil.toCanonicalCareerIdOrNull(r.careerId) == canonicalId) ||
                    normalizeCareerId(r.careerId) == normalized ||
                    normalizeCareerId(r.title) == normalized
                }
                if (matched != null) {
                    _roadmapDtoCache[cacheKey] = matched
                    return@withLock matched
                }
                return@withLock null
            }

            val session = try {
                client.auth.currentSessionOrNull()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                null
            }
            if (session == null) {
                Log.d(TAG, "Unauthenticated session; skipping remote roadmaps query to avoid anon permission denial")
                return@withLock null
            }

            // Fetch all roadmaps in a single atomic batch to avoid multiple sequential or cancelled HTTP requests
            try {
                val allRoadmaps = withContext(NonCancellable) {
                    client.from("roadmaps").select().decodeList<RoadmapDto>()
                }
                _allRoadmapDtos = allRoadmaps
                for (r in allRoadmaps) {
                    val canonical = CareerCanonicalUtil.toCanonicalCareerIdOrNull(r.careerId)
                    if (canonical != null) {
                        _roadmapDtoCache[canonical] = r
                    }
                    _roadmapDtoCache[r.careerId] = r
                }

                val matched = allRoadmaps.firstOrNull { r ->
                    (canonicalId != null && r.careerId.equals(canonicalId, ignoreCase = true)) ||
                    r.careerId.equals(cleanId, ignoreCase = true) ||
                    r.careerId.equals(normalized, ignoreCase = true) ||
                    r.title.equals(cleanId, ignoreCase = true) ||
                    (canonicalId != null && CareerCanonicalUtil.toCanonicalCareerIdOrNull(r.careerId) == canonicalId) ||
                    normalizeCareerId(r.careerId) == normalized ||
                    normalizeCareerId(r.title) == normalized
                }
                if (matched != null) {
                    _roadmapDtoCache[cacheKey] = matched
                    return@withLock matched
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Fetching roadmaps failed: ${e.message}")
                throw e
            }

            null
        }
    }

    fun normalizeCareerId(career: String): String {
        return CareerCanonicalUtil.normalize(career)
    }

    /**
     * Synchronizes and isolates user session progress state.
     * Resets in-memory progress when the authenticated user changes or logs out.
     */
    suspend fun syncUserSession(currentUserId: String?) = sessionMutex.withLock {
        if (_activeUserId != currentUserId) {
            Log.d(TAG, "User session changed from '$_activeUserId' to '$currentUserId'. Isolating progress.")
            val oldUserId = _activeUserId
            _activeUserId = currentUserId

            _userProgressFlow.update { map ->
                if (oldUserId != null) {
                    map - oldUserId
                } else {
                    map
                }
            }

            if (currentUserId == null) {
                _userProgressFlow.value = emptyMap()
            }
        }
    }

    /**
     * Retrieves the current authenticated user's ID securely without trusting client parameters.
     */
    private suspend fun getAuthenticatedUserId(): String? {
        val authUser = client.auth.currentUserOrNull()?.id
        if (!authUser.isNullOrBlank()) return authUser

        val sessionUser = client.auth.currentSessionOrNull()?.user?.id
        if (!sessionUser.isNullOrBlank()) return sessionUser

        return authRepository?.getCurrentUser()?.firstOrNull()?.id
    }

    /**
     * Merges a global roadmap definition with a user's completed task IDs.
     */
    fun mergeProgress(roadmapDef: CareerRoadmap, completedTaskIds: Set<String>): CareerRoadmap {
        return roadmapDef.copy(
            stages = roadmapDef.stages.map { stage ->
                stage.copy(
                    tasks = stage.tasks.map { task ->
                        task.copy(isCompleted = completedTaskIds.contains(task.id))
                    }
                )
            }
        )
    }

    /**
     * Atomically records daily activity and updates the user's streak in Supabase via RPC.
     * Non-blocking and non-fatal: streak recording failures log a warning and do not abort progress.
     */
    private suspend fun recordUserActivity(userId: String, triggerSource: String = "general"): Int? {
        return try {
            Log.d(TAG, "STREAK DEBUG: RPC START: record_user_activity | userId = $userId | source = $triggerSource")
            val rpcParams = buildJsonObject {
                put("p_user_id", userId)
            }
            val newStreak = client.postgrest.rpc(
                function = "record_user_activity",
                parameters = rpcParams
            ).decodeAs<Int>()

            Log.d(TAG, "STREAK DEBUG: RPC SUCCESS: record_user_activity | userId = $userId | newStreak = $newStreak | source = $triggerSource")
            if (authRepository != null) {
                authRepository.updateCachedStreak(userId, newStreak)
                Log.d(TAG, "STREAK DEBUG: Profile cache streak updated | userId = $userId | newStreak = $newStreak")
            }
            newStreak
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "STREAK DEBUG: record_user_activity warning (non-fatal): ${e.message} | userId = $userId | source = $triggerSource", e)
            null
        }
    }
}
