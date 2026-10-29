package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CareerRecommendation
import com.example.domain.model.SkillProgressItem
import com.example.domain.model.StudentProfile
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CareerRepository
import com.example.domain.repository.PracticeRepository
import com.example.domain.repository.RoadmapRepository
import com.example.util.CareerCanonicalUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val studentName: String = "Student",
    val department: String = "Department",
    val yearOfStudy: String = "1st Year",
    val currentStreak: Int = 0,
    val xp: Int = 0,
    val isAssessmentCompleted: Boolean = false,
    val recommendedCareer: String = "Not Assessed Yet",
    val recommendedCareerId: String = "",
    val matchPercentage: Int = 0,
    val roadmapProgress: Int = 0,
    val isRoadmapProgressAvailable: Boolean = true,
    val dailyMotivationalMessage: String = "Consistency is the bridge between goals and accomplishment.",
    val skillBreakdown: List<SkillProgressItem> = emptyList(),
    val placementReadinessPercentage: Int = 0,
    val practiceProgress: Int = 0,
    val solvedPracticeProblemsCount: Int = 0,
    val totalPracticeProblemsCount: Int = 84
)

private data class DashboardSessionSnapshot(
    val userId: String?,
    val isAssessmentCompleted: Boolean,
    val recommendedCareerId: String?,
    val studentName: String,
    val department: String,
    val yearOfStudy: String,
    val currentStreak: Int,
    val xp: Int,
    val dailyMotivationalMessage: String,
    val recommendationTitle: String?,
    val matchPercentage: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val careerRepository: CareerRepository,
    private val roadmapRepository: RoadmapRepository,
    private val practiceRepository: PracticeRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val practiceFlow: Flow<Pair<Int, Int>> = if (practiceRepository != null) {
                    practiceRepository.getProblems().map { problems ->
                        val total = if (problems.isNotEmpty()) problems.size else 52
                        val solved = problems.count { it.isSolved }
                        Pair(solved, total)
                    }.onStart {
                        emit(Pair(0, 52))
                    }.catch {
                        emit(Pair(0, 52))
                    }
                } else {
                    flowOf(Pair(0, 52))
                }

                val userAndRoadmapFlow = authRepository.getCurrentUser()
                    .distinctUntilChanged()
                    .flatMapLatest { user ->
                        val isCompleted = user?.isAssessmentCompleted == true && !user.recommendedCareerId.isNullOrBlank()
                        val canonicalCareerId = if (isCompleted) {
                            CareerCanonicalUtil.toCanonicalCareerIdOrNull(user?.recommendedCareerId)
                        } else null

                        val effectiveCareerId = canonicalCareerId ?: "aiml_engineer"

                        val skillsFlow: Flow<List<SkillProgressItem>> = roadmapRepository.getSkillBreakdown(effectiveCareerId)
                            .catch { emit(emptyList()) }
                            .onStart { emit(emptyList()) }

                        val progressFlow: Flow<Int> = roadmapRepository.getOverallProgress(effectiveCareerId)

                        val roadmapDetailsFlow: Flow<Triple<Int, List<SkillProgressItem>, Boolean>> = progressFlow
                            .combine(skillsFlow) { progress: Int, skills: List<SkillProgressItem> ->
                                Triple(progress, skills, true)
                            }.catch {
                                emit(Triple(0, emptyList(), false))
                            }

                        // For unassessed users, do NOT make career recommendation network request
                        val recommendationFlow: Flow<CareerRecommendation?> = if (isCompleted) {
                            careerRepository.getLatestRecommendation()
                                .catch { emit(null) }
                                .onStart { emit(null) }
                        } else {
                            flowOf(null)
                        }

                        combine(
                            recommendationFlow,
                            roadmapDetailsFlow
                        ) { recommendation, roadmapTriple ->
                            Triple(user, recommendation, roadmapTriple)
                        }
                    }

                combine(
                    userAndRoadmapFlow,
                    practiceFlow
                ) { (user, recommendation, roadmapTriple), (solvedPracticeCount, totalPracticeCount) ->
                    val (roadmapProgress, skillBreakdown, isRoadmapAvailable) = roadmapTriple
                    val isCompleted = user?.isAssessmentCompleted == true && !user.recommendedCareerId.isNullOrBlank()
                    val canonicalCareerId = if (isCompleted) {
                        CareerCanonicalUtil.toCanonicalCareerIdOrNull(user?.recommendedCareerId)
                    } else null
                    val effectiveCareerId = canonicalCareerId ?: "aiml_engineer"

                    val careerTitle = if (isCompleted) {
                        recommendation?.title ?: CareerCanonicalUtil.toCanonicalCareerTitle(effectiveCareerId)
                    } else {
                        "Not Assessed Yet"
                    }
                    val matchPercentage = if (isCompleted) {
                        recommendation?.matchPercentage ?: 85
                    } else 0

                    val assessmentScore = if (isCompleted) 100 else 0
                    val roadmapScore = if (isCompleted) roadmapProgress.coerceIn(0, 100) else 0
                    val practiceScore = if (totalPracticeCount > 0) {
                        ((solvedPracticeCount.toFloat() / totalPracticeCount.toFloat()) * 100).toInt().coerceIn(0, 100)
                    } else 0
                    val activityScore = ((user?.currentStreak ?: 0) * 10).coerceIn(0, 100)

                    val readinessRaw = (assessmentScore * 0.25) + (roadmapScore * 0.35) + (practiceScore * 0.30) + (activityScore * 0.10)
                    val readinessPercentage = Math.round(readinessRaw).toInt().coerceIn(0, 100)

                    DashboardUiState(
                        isLoading = false,
                        studentName = user?.name?.ifBlank { "Student" } ?: "Student",
                        department = user?.department?.ifBlank { "Department" } ?: "Department",
                        yearOfStudy = user?.yearOfStudy?.ifBlank { "1st Year" } ?: "1st Year",
                        currentStreak = user?.currentStreak ?: 0,
                        xp = user?.xp ?: 0,
                        isAssessmentCompleted = isCompleted,
                        recommendedCareer = careerTitle,
                        recommendedCareerId = if (isCompleted) effectiveCareerId else "",
                        matchPercentage = matchPercentage,
                        roadmapProgress = roadmapProgress,
                        isRoadmapProgressAvailable = isRoadmapAvailable,
                        dailyMotivationalMessage = user?.dailyMotivationalMessage ?: "Continuous learning turns ambitions into achievements.",
                        skillBreakdown = skillBreakdown,
                        placementReadinessPercentage = readinessPercentage,
                        practiceProgress = practiceScore,
                        solvedPracticeProblemsCount = solvedPracticeCount,
                        totalPracticeProblemsCount = totalPracticeCount
                    )
                }.catch { _ ->
                    // Guard against any unhandled stream error
                }.collect { state ->
                    _uiState.value = state
                }
            } catch (_: Exception) {
                // Ignore initialization failures gracefully
            }
        }
    }
}
