package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AiCareerGuidance
import com.example.domain.model.CareerGuidanceContext
import com.example.domain.model.CareerRecommendation
import com.example.domain.repository.AiGuidanceRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CareerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiGuidanceUiState {
    object Idle : AiGuidanceUiState
    object Loading : AiGuidanceUiState
    data class Success(val guidance: AiCareerGuidance) : AiGuidanceUiState
    data class Error(val message: String) : AiGuidanceUiState
}

sealed interface AssessmentResultUiState {
    object Loading : AssessmentResultUiState
    data class Success(val recommendation: CareerRecommendation) : AssessmentResultUiState
    data class Error(val message: String) : AssessmentResultUiState
    object Empty : AssessmentResultUiState
}

class CareerViewModel(
    private val careerRepository: CareerRepository,
    private val aiGuidanceRepository: AiGuidanceRepository? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    val latestRecommendation: StateFlow<CareerRecommendation?> = careerRepository.getLatestRecommendation()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _assessmentResultUiState = MutableStateFlow<AssessmentResultUiState>(
        latestRecommendation.value?.let { AssessmentResultUiState.Success(it) } ?: AssessmentResultUiState.Loading
    )
    val assessmentResultUiState: StateFlow<AssessmentResultUiState> = _assessmentResultUiState.asStateFlow()

    private val _allCareers = MutableStateFlow<List<CareerRecommendation>>(careerRepository.getAllCareers())
    val allCareers: StateFlow<List<CareerRecommendation>> = _allCareers.asStateFlow()

    init {
        // Continuously update assessment result UI state whenever repository emits recommendation
        viewModelScope.launch {
            latestRecommendation.collect { rec ->
                if (rec != null) {
                    _assessmentResultUiState.value = AssessmentResultUiState.Success(rec)
                }
            }
        }

        viewModelScope.launch {
            val result = careerRepository.loadCareers()
            result.onSuccess {
                _allCareers.value = it
            }
        }
        if (authRepository != null) {
            viewModelScope.launch {
                authRepository.authState.collect { state ->
                    when (state) {
                        is com.example.domain.model.AuthState.ProfileComplete -> {
                            val result = careerRepository.loadCareers()
                            result.onSuccess {
                                _allCareers.value = it
                            }
                            if (_assessmentResultUiState.value !is AssessmentResultUiState.Success) {
                                loadAssessmentResult(state.profile.id)
                            }
                        }
                        is com.example.domain.model.AuthState.ProfileIncomplete -> {
                            val result = careerRepository.loadCareers()
                            result.onSuccess {
                                _allCareers.value = it
                            }
                            if (_assessmentResultUiState.value !is AssessmentResultUiState.Success) {
                                loadAssessmentResult(state.profile.id)
                            }
                        }
                        is com.example.domain.model.AuthState.SignedOut -> {
                            _assessmentResultUiState.value = AssessmentResultUiState.Empty
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun setRecommendation(recommendation: CareerRecommendation) {
        _assessmentResultUiState.value = AssessmentResultUiState.Success(recommendation)
        viewModelScope.launch {
            try {
                careerRepository.saveRecommendation(recommendation)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("CareerViewModel", "saveRecommendation failed: ${e.message}")
            }
        }
    }

    fun loadAssessmentResult(userId: String? = null) {
        val current = latestRecommendation.value
        if (current != null) {
            _assessmentResultUiState.value = AssessmentResultUiState.Success(current)
            return
        }

        viewModelScope.launch {
            _assessmentResultUiState.value = AssessmentResultUiState.Loading
            val effectiveUserId = userId ?: authRepository?.getCurrentUser()?.firstOrNull()?.id
            val result = careerRepository.loadLatestRecommendation(effectiveUserId)
            result.onSuccess { rec ->
                if (rec != null) {
                    _assessmentResultUiState.value = AssessmentResultUiState.Success(rec)
                } else {
                    _assessmentResultUiState.value = AssessmentResultUiState.Empty
                }
            }.onFailure { err ->
                _assessmentResultUiState.value = AssessmentResultUiState.Error(
                    err.message ?: "Failed to load career recommendation"
                )
            }
        }
    }

    fun retryLoadAssessmentResult(userId: String? = null) {
        loadAssessmentResult(userId)
    }

    fun refreshCareers() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = careerRepository.loadCareers()
            result.onSuccess {
                _allCareers.value = it
            }.onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }

    private val _selectedCareer = MutableStateFlow<CareerRecommendation?>(null)
    val selectedCareer: StateFlow<CareerRecommendation?> = _selectedCareer.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // AI Career Guidance State
    private val _aiGuidanceState = MutableStateFlow<AiGuidanceUiState>(AiGuidanceUiState.Idle)
    val aiGuidanceState: StateFlow<AiGuidanceUiState> = _aiGuidanceState.asStateFlow()

    private var lastLoadedCareerId: String? = null

    fun selectCareerDetail(careerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val detail = careerRepository.getCareerDetails(careerId)
                _selectedCareer.value = detail
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Requests personalized AI career guidance from the server-side backend.
     * Caches in-memory per careerId to avoid redundant requests on UI recompositions.
     */
    fun loadAiGuidance(recommendation: CareerRecommendation, forceRefresh: Boolean = false) {
        if (aiGuidanceRepository == null) {
            _aiGuidanceState.value = AiGuidanceUiState.Error("AI repository not configured.")
            return
        }

        // Avoid re-requesting if already loaded for this career unless explicitly retried
        if (!forceRefresh && lastLoadedCareerId == recommendation.careerId && _aiGuidanceState.value is AiGuidanceUiState.Success) {
            return
        }

        // Prevent duplicate concurrent requests
        if (_aiGuidanceState.value is AiGuidanceUiState.Loading && !forceRefresh) {
            return
        }

        viewModelScope.launch {
            _aiGuidanceState.value = AiGuidanceUiState.Loading

            try {
                // Build lightweight, non-sensitive student context
                val student = authRepository?.getCurrentUser()?.firstOrNull()
                val context = CareerGuidanceContext(
                    department = student?.department.orEmpty(),
                    year = student?.yearOfStudy.orEmpty(),
                    careerInterest = student?.careerInterest.orEmpty(),
                    career = recommendation.title,
                    matchPercentage = recommendation.matchPercentage,
                    careerSkills = recommendation.recommendedSkills
                )

                val result = aiGuidanceRepository.generateCareerGuidance(context)
                result.fold(
                    onSuccess = { guidance ->
                        lastLoadedCareerId = recommendation.careerId
                        _aiGuidanceState.value = AiGuidanceUiState.Success(guidance)
                    },
                    onFailure = { throwable ->
                        _aiGuidanceState.value = AiGuidanceUiState.Error(
                            throwable.message ?: "Personalized guidance is temporarily unavailable."
                        )
                    }
                )
            } catch (e: Exception) {
                _aiGuidanceState.value = AiGuidanceUiState.Error(
                    e.message ?: "Personalized guidance is temporarily unavailable."
                )
            }
        }
    }

    fun retryAiGuidance(recommendation: CareerRecommendation) {
        if (_aiGuidanceState.value is AiGuidanceUiState.Loading) return
        loadAiGuidance(recommendation, forceRefresh = true)
    }
}
