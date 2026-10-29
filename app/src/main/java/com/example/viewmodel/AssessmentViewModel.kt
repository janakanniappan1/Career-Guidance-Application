package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AssessmentAttempt
import com.example.domain.model.AssessmentQuestion
import com.example.domain.model.CareerRecommendation
import com.example.domain.repository.AssessmentRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CareerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class AssessmentUiState(
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 15,
    val currentQuestion: AssessmentQuestion? = null,
    val selectedOptionId: String? = null,
    val selectedAnswers: Map<Int, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitting: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoNext: Boolean = false,
    val isLastQuestion: Boolean = false
)

class AssessmentViewModel(
    private val assessmentRepository: AssessmentRepository,
    private val careerRepository: CareerRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private var questions = assessmentRepository.getAssessmentQuestions()

    private val _uiState = MutableStateFlow(
        AssessmentUiState(
            currentQuestionIndex = 0,
            totalQuestions = questions.size,
            currentQuestion = questions.firstOrNull(),
            selectedOptionId = null,
            isLoading = questions.isEmpty(),
            error = null,
            canGoBack = false,
            canGoNext = false,
            isLastQuestion = questions.size <= 1
        )
    )
    val uiState: StateFlow<AssessmentUiState> = _uiState.asStateFlow()

    init {
        loadAssessmentData()
    }

    fun loadAssessmentData() {
        viewModelScope.launch {
            val initialCached = assessmentRepository.getAssessmentQuestions()
            if (initialCached.isNotEmpty()) {
                questions = initialCached
                val savedAnswers = assessmentRepository.getUserAnswers()
                val currentQ = questions.firstOrNull()
                val selectedOpt = currentQ?.let { savedAnswers[it.id] }
                _uiState.value = _uiState.value.copy(
                    totalQuestions = questions.size,
                    currentQuestion = currentQ,
                    selectedAnswers = savedAnswers,
                    selectedOptionId = selectedOpt,
                    isLoading = false,
                    error = null,
                    canGoNext = selectedOpt != null,
                    isLastQuestion = questions.size <= 1
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }

            val loadResult = assessmentRepository.loadQuestions()
            loadResult.fold(
                onSuccess = { loadedQuestions ->
                    questions = loadedQuestions
                    val savedAnswers = assessmentRepository.getUserAnswers()
                    val currIndex = _uiState.value.currentQuestionIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
                    val currentQ = questions.getOrNull(currIndex)
                    val selectedOpt = currentQ?.let { savedAnswers[it.id] }

                    _uiState.value = _uiState.value.copy(
                        currentQuestionIndex = currIndex,
                        totalQuestions = questions.size,
                        currentQuestion = currentQ,
                        selectedAnswers = savedAnswers,
                        selectedOptionId = selectedOpt,
                        isLoading = false,
                        error = null,
                        canGoBack = currIndex > 0,
                        canGoNext = selectedOpt != null,
                        isLastQuestion = currIndex >= questions.size - 1
                    )
                },
                onFailure = { throwable ->
                    if (questions.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load assessment questions. Please try again."
                        )
                    } else {
                        // Keep using already cached questions if remote refresh failed
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            )
        }
    }

    fun retryLoadingQuestions() {
        loadAssessmentData()
    }

    fun selectOption(optionId: String) {
        val currentQ = _uiState.value.currentQuestion ?: return
        val currentAnswers = _uiState.value.selectedAnswers + (currentQ.id to optionId)

        // Update UI state immediately to ensure instant, responsive visual feedback
        _uiState.value = _uiState.value.copy(
            selectedOptionId = optionId,
            selectedAnswers = currentAnswers,
            canGoNext = true
        )

        // Asynchronously persist answer in background with robust exception handling
        viewModelScope.launch {
            try {
                assessmentRepository.saveAnswer(currentQ.id, optionId)
            } catch (e: Throwable) {
                try {
                    android.util.Log.w("AssessmentViewModel", "Non-fatal: failed to save answer remotely: ${e.message}", e)
                } catch (_: Throwable) {
                    // Safe fallback for JVM testing
                }
            }
        }
    }

    fun nextQuestion() {
        val nextIndex = _uiState.value.currentQuestionIndex + 1
        if (nextIndex < questions.size) {
            val nextQ = questions[nextIndex]
            val existingAnswer = _uiState.value.selectedAnswers[nextQ.id]
            _uiState.value = _uiState.value.copy(
                currentQuestionIndex = nextIndex,
                currentQuestion = nextQ,
                selectedOptionId = existingAnswer,
                canGoBack = true,
                canGoNext = existingAnswer != null,
                isLastQuestion = nextIndex == questions.size - 1
            )
        }
    }

    fun previousQuestion() {
        val prevIndex = _uiState.value.currentQuestionIndex - 1
        if (prevIndex >= 0) {
            val prevQ = questions[prevIndex]
            val existingAnswer = _uiState.value.selectedAnswers[prevQ.id]
            _uiState.value = _uiState.value.copy(
                currentQuestionIndex = prevIndex,
                currentQuestion = prevQ,
                selectedOptionId = existingAnswer,
                canGoBack = prevIndex > 0,
                canGoNext = existingAnswer != null,
                isLastQuestion = false
            )
        }
    }

    fun submitAssessment(onComplete: (CareerRecommendation) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            val answers = assessmentRepository.getUserAnswers().toMutableMap().apply {
                putAll(_uiState.value.selectedAnswers)
            }
            val recommendation = careerRepository.calculateRecommendation(answers)

            // Update user profile status and record attempt
            val currentUser = authRepository.getCurrentUser().firstOrNull()
            val userId = currentUser?.id ?: ""

            val attempt = AssessmentAttempt(
                id = null,
                userId = userId,
                answers = answers,
                topCareerId = recommendation.careerId,
                matchPercentage = recommendation.matchPercentage
            )
            val saveResult = assessmentRepository.saveAssessmentAttempt(attempt)
            if (saveResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = saveResult.exceptionOrNull()?.message ?: "Failed to save assessment attempt"
                )
                return@launch
            }

            if (currentUser != null) {
                val alreadyCompleted = currentUser.isAssessmentCompleted
                val updatedXp = if (!alreadyCompleted) currentUser.xp + 100 else currentUser.xp

                try {
                    val updatedProfile = currentUser.copy(
                        isAssessmentCompleted = true,
                        recommendedCareerId = recommendation.careerId,
                        xp = updatedXp
                    )
                    authRepository.updateProfile(updatedProfile)
                    authRepository.updateCachedXp(currentUser.id, updatedXp)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = e.message ?: "Failed to update profile"
                    )
                    return@launch
                }
            }

            _uiState.value = _uiState.value.copy(isSubmitting = false)
            onComplete(recommendation)
        }
    }
}
