package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AiRoadmapGuidance
import com.example.domain.model.CareerRoadmap
import com.example.domain.model.RoadmapGuidanceContext
import com.example.domain.model.SkillProgressItem
import com.example.domain.repository.AiRoadmapGuidanceRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.RoadmapRepository
import com.example.util.CareerCanonicalUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AiRoadmapGuidanceUiState {
    object Idle : AiRoadmapGuidanceUiState
    object Loading : AiRoadmapGuidanceUiState
    data class Success(val guidance: AiRoadmapGuidance) : AiRoadmapGuidanceUiState
    data class Error(val message: String) : AiRoadmapGuidanceUiState
}

data class RoadmapUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val roadmap: CareerRoadmap? = null,
    val skillBreakdown: List<SkillProgressItem> = emptyList(),
    val overallProgress: Int = 0,
    val activeCareerId: String = "",
    val isUnassessed: Boolean = false,
    val currentStreak: Int = 0
)

class RoadmapViewModel(
    private val roadmapRepository: RoadmapRepository,
    private val aiRoadmapGuidanceRepository: AiRoadmapGuidanceRepository? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoadmapUiState())
    val uiState: StateFlow<RoadmapUiState> = _uiState.asStateFlow()

    private val _aiGuidanceState = MutableStateFlow<AiRoadmapGuidanceUiState>(AiRoadmapGuidanceUiState.Idle)
    val aiGuidanceState: StateFlow<AiRoadmapGuidanceUiState> = _aiGuidanceState.asStateFlow()

    private var lastGuidanceCacheKey: String? = null
    private var roadmapJob: Job? = null
    private var currentLoadingCareerId: String? = null

    init {
        if (authRepository != null) {
            viewModelScope.launch {
                authRepository.getCurrentUser().collect { user ->
                    val streak = user?.currentStreak ?: 0
                    _uiState.update { it.copy(currentStreak = streak) }
                }
            }
        }
    }

    fun clearState() {
        roadmapJob?.cancel()
        roadmapJob = null
        currentLoadingCareerId = null
        lastGuidanceCacheKey = null
        _aiGuidanceState.value = AiRoadmapGuidanceUiState.Idle
        _uiState.value = RoadmapUiState(currentStreak = _uiState.value.currentStreak)
    }

    fun loadRoadmapForCareer(careerId: String? = null, forceReload: Boolean = false) {
        val explicitCanonical = if (!careerId.isNullOrBlank() && !CareerCanonicalUtil.isActionOrPlaceholderId(careerId)) {
            CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
        } else {
            null
        }

        // Fast path: if career ID matches active or loading and already loaded or in progress, do nothing
        if (!forceReload) {
            if (explicitCanonical != null) {
                if (_uiState.value.activeCareerId == explicitCanonical && _uiState.value.roadmap != null) {
                    return
                }
                if (currentLoadingCareerId == explicitCanonical && roadmapJob?.isActive == true) {
                    return
                }
            } else {
                // If careerId is not explicitly specified, but we already have an active or loading roadmap, avoid restarting
                if (_uiState.value.activeCareerId.isNotBlank() && _uiState.value.roadmap != null) {
                    return
                }
                if (roadmapJob?.isActive == true) {
                    return
                }
            }
        }

        currentLoadingCareerId = explicitCanonical ?: "__RESOLVING__"
        roadmapJob?.cancel()
        roadmapJob = viewModelScope.launch {
            val safeCareerId = explicitCanonical ?: run {
                val userRecommended = authRepository?.getCurrentUser()?.firstOrNull()?.recommendedCareerId
                CareerCanonicalUtil.toCanonicalCareerIdOrNull(userRecommended)
            }

            if (safeCareerId == null) {
                currentLoadingCareerId = null
                _uiState.value = RoadmapUiState(
                    isLoading = false,
                    isUnassessed = true,
                    error = null,
                    activeCareerId = "",
                    currentStreak = _uiState.value.currentStreak
                )
                return@launch
            }

            currentLoadingCareerId = safeCareerId

            val careerChanged = _uiState.value.activeCareerId != safeCareerId

            if (!careerChanged && !forceReload && _uiState.value.roadmap != null) {
                currentLoadingCareerId = null
                return@launch
            }

            currentLoadingCareerId = safeCareerId

            _uiState.value = _uiState.value.copy(
                activeCareerId = safeCareerId,
                isLoading = _uiState.value.roadmap == null || careerChanged,
                isUnassessed = false,
                error = null
            )

            if (careerChanged) {
                lastGuidanceCacheKey = null
                _aiGuidanceState.value = AiRoadmapGuidanceUiState.Idle
            }

            try {
                roadmapRepository.getRoadmap(safeCareerId)
                    .catch { e ->
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        val friendlyError = if (e is NoSuchElementException) {
                            "The curriculum for ${CareerCanonicalUtil.toCanonicalCareerTitle(safeCareerId)} is currently being updated in Supabase. Please explore available milestones or try again later."
                        } else {
                            "Unable to load roadmap progress for ${CareerCanonicalUtil.toCanonicalCareerTitle(safeCareerId)}. Please check your connection and tap retry."
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = friendlyError
                        )
                    }
                    .collect { roadmap ->
                        currentLoadingCareerId = null
                        _uiState.value = _uiState.value.copy(
                            roadmap = roadmap,
                            overallProgress = roadmap.overallProgressPercentage,
                            skillBreakdown = calculateSkillBreakdown(roadmap),
                            isLoading = false,
                            isUnassessed = false,
                            error = null
                        )
                        loadAiGuidance(roadmap = roadmap, forceRefresh = false)
                    }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Expected on coroutine cancellation, ignore
            } catch (e: NoSuchElementException) {
                currentLoadingCareerId = null
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "The curriculum for ${CareerCanonicalUtil.toCanonicalCareerTitle(safeCareerId)} is currently being updated in Supabase. Please explore available milestones or try again later."
                )
            } catch (e: Exception) {
                currentLoadingCareerId = null
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unable to load roadmap progress for ${CareerCanonicalUtil.toCanonicalCareerTitle(safeCareerId)}. Please check your connection and tap retry."
                )
            }
        }
    }

    private fun calculateSkillBreakdown(roadmap: CareerRoadmap): List<SkillProgressItem> {
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

    fun toggleTask(stageId: String, taskId: String) {
        viewModelScope.launch {
            try {
                roadmapRepository.toggleTask(_uiState.value.activeCareerId, stageId, taskId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update task progress"
                )
            }
        }
    }

    fun loadAiGuidance(roadmap: CareerRoadmap? = _uiState.value.roadmap, forceRefresh: Boolean = false) {
        val currentRoadmap = roadmap ?: _uiState.value.roadmap ?: return
        val aiRepo = aiRoadmapGuidanceRepository ?: return

        val allTasks = currentRoadmap.stages.flatMap { it.tasks }
        val completedTasks = allTasks.filter { it.isCompleted }.map { it.title }
        val pendingTasks = allTasks.filterNot { it.isCompleted }.map { it.title }

        val currentStage = currentRoadmap.stages.firstOrNull { stage ->
            stage.tasks.any { !it.isCompleted }
        }?.stageName ?: currentRoadmap.stages.lastOrNull()?.stageName.orEmpty()

        val nextTasks = currentRoadmap.stages
            .firstOrNull { stage -> stage.tasks.any { !it.isCompleted } }
            ?.tasks?.filterNot { it.isCompleted }?.map { it.title }
            ?: pendingTasks.take(3)

        val skillCategories = allTasks.map { it.skillCategory }.distinct()

        val cacheKey = "${currentRoadmap.careerId}_${currentRoadmap.overallProgressPercentage}_${completedTasks.hashCode()}"

        if (!forceRefresh && cacheKey == lastGuidanceCacheKey && _aiGuidanceState.value is AiRoadmapGuidanceUiState.Success) {
            return
        }

        if (_aiGuidanceState.value is AiRoadmapGuidanceUiState.Loading && !forceRefresh) {
            return
        }

        viewModelScope.launch {
            _aiGuidanceState.value = AiRoadmapGuidanceUiState.Loading
            try {
                val context = RoadmapGuidanceContext(
                    career = currentRoadmap.careerTitle,
                    overallProgressPercentage = currentRoadmap.overallProgressPercentage,
                    completedTasks = completedTasks,
                    pendingTasks = pendingTasks,
                    currentStage = currentStage,
                    nextTasks = nextTasks,
                    skillCategories = skillCategories
                )

                val result = aiRepo.generateRoadmapGuidance(context)
                result.fold(
                    onSuccess = { guidance ->
                        lastGuidanceCacheKey = cacheKey
                        _aiGuidanceState.value = AiRoadmapGuidanceUiState.Success(guidance)
                    },
                    onFailure = { throwable ->
                        _aiGuidanceState.value = AiRoadmapGuidanceUiState.Error(
                            throwable.message ?: "Personalized guidance is temporarily unavailable."
                        )
                    }
                )
            } catch (e: Exception) {
                _aiGuidanceState.value = AiRoadmapGuidanceUiState.Error(
                    e.message ?: "Personalized guidance is temporarily unavailable."
                )
            }
        }
    }

    fun retryAiGuidance() {
        if (_aiGuidanceState.value is AiRoadmapGuidanceUiState.Loading) return
        loadAiGuidance(forceRefresh = true)
    }
}
