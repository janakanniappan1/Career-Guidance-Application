package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AdminOverview
import com.example.domain.model.InterviewQuestion
import com.example.domain.model.PracticeCategories
import com.example.domain.model.PracticeConstants
import com.example.domain.model.PracticeProblem
import com.example.domain.model.ProjectItem
import com.example.domain.model.QuizAttempt
import com.example.domain.model.QuizMode
import com.example.domain.model.QuizQuestion
import com.example.domain.model.ResumeAnalysis
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CareerRepository
import com.example.domain.repository.PracticeRepository
import com.example.data.repository.DefaultPracticeCatalog
import com.example.util.CareerCanonicalUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PracticeCompletionFilter(val displayName: String) {
    ALL("All"),
    COMPLETED("Completed"),
    INCOMPLETE("Incomplete");

    companion object {
        fun fromString(value: String): PracticeCompletionFilter {
            return when (value.trim().lowercase()) {
                "completed", "solved" -> COMPLETED
                "incomplete", "unsolved" -> INCOMPLETE
                else -> ALL
            }
        }
    }
}

data class PracticeUiState(
    val problems: List<PracticeProblem> = emptyList(),
    val selectedProblem: PracticeProblem? = null,
    val isSubmittingProblem: Boolean = false,
    val selectedOptionIndex: Int? = null,
    val problemSubmissionResult: Boolean? = null,
    val isSaveFailed: Boolean = false,
    val feedbackMessage: String? = null,
    val interviewQuestions: List<InterviewQuestion> = emptyList(),
    val projects: List<ProjectItem> = emptyList(),
    val resumeAnalysis: ResumeAnalysis = ResumeAnalysis(),
    val adminOverview: AdminOverview = AdminOverview(),
    val selectedFilter: String = "All",
    val completionFilter: PracticeCompletionFilter = PracticeCompletionFilter.ALL,
    val activeTab: Int = 0, // 0 = Coding, 1 = Aptitude, 2 = Technical
    val selectedCareerId: String? = null,
    val currentStreak: Int = 0,
    val totalXp: Int = 0,
    val isDailyTestCompletedToday: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class QuizUiState(
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerChecked: Boolean = false,
    val isCorrect: Boolean = false,
    val score: Int = 0,
    val isQuizFinished: Boolean = false,
    val xpEarned: Int = 0,
    val maximumXp: Int = PracticeConstants.DAILY_SPEED_TEST_MAX_XP,
    val careerId: String? = null,
    val topic: String = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
    val mode: QuizMode = QuizMode.DAILY_SPEED_TEST,
    val isAttemptSaved: Boolean = false,
    val currentStreak: Int = 0,
    val isAlreadyCompletedToday: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PracticeViewModel(
    private val practiceRepository: PracticeRepository,
    private val careerRepository: CareerRepository? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private val _quizState = MutableStateFlow(QuizUiState())
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    private var problemsJob: Job? = null
    private var projectsJob: Job? = null
    private var lastLoadedCareer: String? = null
    private var lastLoadedUserId: String? = null

    init {
        observeUserAndLoadData()
    }

    private fun observeUserAndLoadData() {
        if (authRepository != null) {
            // 1. Observe streak and XP changes reactively without triggering full catalog reloads
            viewModelScope.launch {
                authRepository.getCurrentUser()
                    .map { user -> Pair(user?.currentStreak ?: 0, user?.xp ?: 0) }
                    .distinctUntilChanged()
                    .collect { (streak, xp) ->
                        _uiState.update {
                            it.copy(
                                currentStreak = streak,
                                totalXp = xp
                            )
                        }
                        _quizState.update { it.copy(currentStreak = streak) }
                    }
            }

            // 2. Observe user ID and career changes to load catalog once per career/user change
            viewModelScope.launch {
                val recommendationFlow = careerRepository?.getLatestRecommendation() ?: flowOf(null)
                combine(
                    authRepository.getCurrentUser(),
                    recommendationFlow
                ) { user, recommendation ->
                    Pair(user, recommendation)
                }.collect { (user, recommendation) ->
                    if (user == null) {
                        resetForUser(null)
                    } else {
                        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(
                            recommendation?.careerId 
                                ?: user.recommendedCareerId 
                                ?: user.careerInterest
                        )
                        if (user.id != lastLoadedUserId || canonicalCareer != lastLoadedCareer) {
                            lastLoadedUserId = user.id
                            lastLoadedCareer = canonicalCareer
                            _quizState.value = QuizUiState()
                            executeLoadData(canonicalCareer)
                        }
                    }
                }
            }
        } else {
            loadData(null)
        }
    }

    suspend fun resolveCurrentUserCareer(): String? {
        val recommendation = try {
            careerRepository?.getLatestRecommendation()?.firstOrNull()
        } catch (_: Throwable) {
            null
        }
        val fromRec = CareerCanonicalUtil.toCanonicalCareerIdOrNull(recommendation?.careerId)
        if (fromRec != null) return fromRec

        val profile = try {
            authRepository?.getCurrentUser()?.firstOrNull()
        } catch (_: Throwable) {
            null
        }
        return CareerCanonicalUtil.toCanonicalCareerIdOrNull(profile?.recommendedCareerId)
            ?: CareerCanonicalUtil.toCanonicalCareerIdOrNull(profile?.careerInterest)
    }

    fun resetForUser(userId: String? = null) {
        lastLoadedUserId = null
        lastLoadedCareer = null
        problemsJob?.cancel()
        projectsJob?.cancel()
        _uiState.value = PracticeUiState()
        _quizState.value = QuizUiState()
        practiceRepository.clearUserCache(userId)
    }

    fun loadData(careerId: String? = null, forceReload: Boolean = false) {
        val candidateCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(
            careerId ?: _uiState.value.selectedCareerId ?: lastLoadedCareer
        )
        if (candidateCareer != null) {
            executeLoadData(candidateCareer, forceReload)
        } else {
            viewModelScope.launch {
                val resolvedCareer = resolveCurrentUserCareer()
                executeLoadData(resolvedCareer, forceReload)
            }
        }
    }

    fun setCareer(careerId: String?) {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(careerId)
        executeLoadData(canonicalCareer, forceReload = true)
    }

    private fun executeLoadData(canonicalCareer: String?, forceReload: Boolean = false) {
        if (!forceReload && canonicalCareer == lastLoadedCareer && (_uiState.value.problems.isNotEmpty() || problemsJob?.isActive == true)) {
            return
        }
        lastLoadedCareer = canonicalCareer
        _uiState.update { 
            it.copy(
                selectedCareerId = canonicalCareer, 
                isLoading = _uiState.value.problems.isEmpty(), 
                errorMessage = null
            ) 
        }

        // Cancel previous problem observation and re-subscribe cleanly
        problemsJob?.cancel()
        problemsJob = viewModelScope.launch {
            try {
                practiceRepository.getProblems(canonicalCareer).collect { problemsList ->
                    _uiState.update { it.copy(problems = problemsList, isLoading = false) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected on coroutine cancellation, ignore
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }

        projectsJob?.cancel()
        projectsJob = viewModelScope.launch {
            try {
                practiceRepository.getProjects().collect { projectsList ->
                    _uiState.update { it.copy(projects = projectsList) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Expected on coroutine cancellation, ignore
            } catch (_: Exception) {
                // Keep current state
            }
        }

        viewModelScope.launch {
            try {
                val interviewList = practiceRepository.getInterviewQuestions(canonicalCareer)
                val resume = practiceRepository.getResumeAnalysis()
                val admin = practiceRepository.getAdminOverview()
                val isDailyCompleted = practiceRepository.isDailyTestCompletedToday()
                _uiState.update {
                    it.copy(
                        interviewQuestions = interviewList,
                        resumeAnalysis = resume,
                        adminOverview = admin,
                        isDailyTestCompletedToday = isDailyCompleted
                    )
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Expected on coroutine cancellation, ignore
            } catch (_: Exception) {
                // Keep safe state
            }
        }
    }

    fun setFilter(category: String) {
        _uiState.update { it.copy(selectedFilter = category) }
    }

    fun setCompletionFilter(filter: PracticeCompletionFilter) {
        _uiState.update { it.copy(completionFilter = filter) }
    }

    fun setCompletionFilter(filterName: String) {
        _uiState.update { it.copy(completionFilter = PracticeCompletionFilter.fromString(filterName)) }
    }

    fun getFilteredProblems(
        categoryIndex: Int = _uiState.value.activeTab,
        filter: PracticeCompletionFilter = _uiState.value.completionFilter
    ): List<PracticeProblem> {
        val allProblems = _uiState.value.problems
        val categoryProblems = when (categoryIndex) {
            0 -> allProblems.filter { PracticeCategories.isCoding(it.category) }
            1 -> allProblems.filter { PracticeCategories.isAptitude(it.category) }
            2 -> allProblems.filter { PracticeCategories.isTechnical(it.category) }
            else -> emptyList()
        }
        return when (filter) {
            PracticeCompletionFilter.ALL -> categoryProblems
            PracticeCompletionFilter.COMPLETED -> categoryProblems.filter { it.isSolved }
            PracticeCompletionFilter.INCOMPLETE -> categoryProblems.filter { !it.isSolved }
        }
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    fun startProblem(id: String) {
        val foundProblem = _uiState.value.problems.find { it.id == id }
            ?: _uiState.value.problems.find { it.title.equals(id, ignoreCase = true) }
            ?: DefaultPracticeCatalog.getProblemById(id)
        val catalogItem = DefaultPracticeCatalog.getProblemById(id)
            ?: (foundProblem?.let { DefaultPracticeCatalog.getProblemById(it.title) })
            ?: (foundProblem?.let { DefaultPracticeCatalog.getProblemByQuery(it.title, it.description, it.topic) })
        
        val problem = if (foundProblem != null && catalogItem != null) {
            catalogItem.copy(
                id = catalogItem.id,
                isSolved = foundProblem.isSolved || catalogItem.isSolved,
                xpReward = if (foundProblem.xpReward > 0) foundProblem.xpReward else catalogItem.xpReward,
                title = if (foundProblem.title.isNotBlank()) foundProblem.title else catalogItem.title,
                category = if (foundProblem.category.isNotBlank()) foundProblem.category else catalogItem.category,
                difficulty = if (foundProblem.difficulty.isNotBlank()) foundProblem.difficulty else catalogItem.difficulty,
                description = if (foundProblem.description.isNotBlank()) foundProblem.description else catalogItem.description,
                options = if (foundProblem.options.size == 4) foundProblem.options else catalogItem.options,
                correctIndex = if (foundProblem.options.size == 4) foundProblem.correctIndex else catalogItem.correctIndex,
                hint = foundProblem.hint?.ifBlank { null } ?: catalogItem.hint,
                explanation = foundProblem.explanation?.ifBlank { null } ?: catalogItem.explanation,
                exampleInput = foundProblem.exampleInput?.ifBlank { null } ?: catalogItem.exampleInput,
                exampleOutput = foundProblem.exampleOutput?.ifBlank { null } ?: catalogItem.exampleOutput,
                constraints = foundProblem.constraints?.ifBlank { null } ?: catalogItem.constraints
            )
        } else {
            foundProblem ?: catalogItem
        }

        _uiState.update {
            it.copy(
                selectedProblem = problem,
                selectedOptionIndex = null,
                problemSubmissionResult = null,
                feedbackMessage = null
            )
        }
    }

    fun selectProblem(problem: PracticeProblem?) {
        _uiState.update {
            it.copy(
                selectedProblem = problem,
                selectedOptionIndex = null,
                problemSubmissionResult = null,
                isSaveFailed = false,
                feedbackMessage = null
            )
        }
    }

    fun selectProblemOption(index: Int) {
        _uiState.update {
            it.copy(
                selectedOptionIndex = index,
                problemSubmissionResult = null,
                isSaveFailed = false,
                feedbackMessage = null
            )
        }
    }

    fun clearSelectedProblem() {
        _uiState.update {
            it.copy(
                selectedProblem = null,
                selectedOptionIndex = null,
                problemSubmissionResult = null,
                isSaveFailed = false,
                feedbackMessage = null
            )
        }
    }

    fun submitProblemAnswer(id: String) {
        val selectedIndex = _uiState.value.selectedOptionIndex ?: return
        completeProblem(id, selectedIndex)
    }

    fun completeProblem(id: String, selectedAnswerIndex: Int? = null) {
        if (_uiState.value.isSubmittingProblem) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingProblem = true) }
            try {
                val problem = _uiState.value.problems.find { it.id == id }
                    ?: _uiState.value.problems.find { it.title.equals(id, ignoreCase = true) }
                    ?: _uiState.value.selectedProblem
                    ?: DefaultPracticeCatalog.getProblemById(id)

                val solveResult = practiceRepository.solveProblem(id, selectedAnswerIndex)

                if (!solveResult.isCorrect) {
                    // 1. Wrong answer selected by user
                    _uiState.update { current ->
                        current.copy(
                            isSubmittingProblem = false,
                            problemSubmissionResult = false,
                            isSaveFailed = false,
                            feedbackMessage = solveResult.message ?: "Incorrect. Check the hint or explanation and try again!"
                        )
                    }
                } else if (solveResult.isCorrect && solveResult.isSolved) {
                    // 2. Correct answer AND successfully persisted to Supabase
                    val rewardXp = solveResult.xpAwarded
                    val feedback = if (solveResult.isFirstSolve) {
                        "Correct! +$rewardXp XP earned."
                    } else {
                        "Correct! Problem already solved previously (+0 XP)."
                    }

                    val currentUser = authRepository?.getCurrentUser()?.firstOrNull()
                    val targetTitle = problem?.title.orEmpty().trim().lowercase()
                    val targetCatalog = DefaultPracticeCatalog.getProblemById(id)
                        ?: DefaultPracticeCatalog.getProblemById(problem?.title ?: "")

                    _uiState.update { current ->
                        current.copy(
                            isSubmittingProblem = false,
                            problemSubmissionResult = true,
                            isSaveFailed = false,
                            feedbackMessage = feedback,
                            totalXp = solveResult.totalXp ?: currentUser?.xp ?: (if (solveResult.isFirstSolve) current.totalXp + rewardXp else current.totalXp),
                            currentStreak = currentUser?.currentStreak ?: current.currentStreak,
                            selectedProblem = (current.selectedProblem ?: problem)?.copy(isSolved = true),
                            problems = current.problems.map { p ->
                                val pCat = DefaultPracticeCatalog.getProblemById(p.id)
                                    ?: DefaultPracticeCatalog.getProblemById(p.title)
                                val isMatch = p.id == id ||
                                    (targetCatalog != null && p.id == targetCatalog.id) ||
                                    (targetCatalog != null && pCat != null && pCat.id == targetCatalog.id) ||
                                    (targetTitle.isNotEmpty() && p.title.trim().lowercase() == targetTitle)
                                if (isMatch) p.copy(isSolved = true) else p
                            }
                        )
                    }
                } else {
                    // 3. Correct answer BUT backend persistence failed
                    // CRITICAL: NEVER mark as incorrect when user selected correct answer!
                    _uiState.update { current ->
                        current.copy(
                            isSubmittingProblem = false,
                            problemSubmissionResult = true,
                            isSaveFailed = true,
                            feedbackMessage = solveResult.message ?: "Could not save your progress. Please try again."
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isSubmittingProblem = false,
                        problemSubmissionResult = null,
                        isSaveFailed = true,
                        feedbackMessage = "Submission failed: ${e.message ?: "Please try again."}"
                    )
                }
            }
        }
    }



    fun toggleProblemSolved(id: String) {
        completeProblem(id)
    }

    fun addProject(title: String, description: String, techStack: List<String>, githubUrl: String) {
        viewModelScope.launch {
            val currentUserId = authRepository?.getCurrentUser()?.firstOrNull()?.id ?: ""
            val newProject = ProjectItem(
                id = null,
                userId = currentUserId,
                title = title,
                description = description,
                techStack = techStack,
                githubUrl = githubUrl,
                liveDemoUrl = null,
                stars = 0
            )
            practiceRepository.addProject(newProject)
        }
    }

    // --- QUIZ & SPEED TEST LOGIC ---

    fun startDailySpeedTest(careerId: String? = null) {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(
            careerId ?: _uiState.value.selectedCareerId
        )

        _quizState.value = QuizUiState(
            isLoading = true,
            careerId = canonicalCareer,
            topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
            mode = QuizMode.DAILY_SPEED_TEST,
            currentStreak = _uiState.value.currentStreak
        )

        viewModelScope.launch {
            try {
                val alreadyCompleted = practiceRepository.isDailyTestCompletedToday()
                val questions = practiceRepository.getQuizQuestions(
                    careerId = canonicalCareer,
                    topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST
                )

                Log.d(
                    "DAILY_TEST_DEBUG",
                    "DAILY_TEST_DEBUG: startDailySpeedTest loaded ${questions.size} questions, alreadyCompleted=$alreadyCompleted, careerId=$canonicalCareer"
                )

                if (questions.size < 5) {
                    _quizState.value = QuizUiState(
                        isLoading = false,
                        errorMessage = if (questions.isEmpty()) {
                            "Daily Test unavailable. Questions could not be loaded. Please try again."
                        } else {
                            "Not enough questions available. Found ${questions.size} of 5 required questions."
                        },
                        questions = questions,
                        careerId = canonicalCareer,
                        topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
                        mode = QuizMode.DAILY_SPEED_TEST,
                        isAlreadyCompletedToday = alreadyCompleted,
                        currentStreak = _uiState.value.currentStreak
                    )
                } else {
                    val fiveQuestions = questions.take(5)
                    _quizState.value = QuizUiState(
                        questions = fiveQuestions,
                        currentIndex = 0,
                        selectedOptionIndex = null,
                        isAnswerChecked = false,
                        isCorrect = false,
                        score = 0,
                        isQuizFinished = false,
                        xpEarned = 0,
                        maximumXp = QuizMode.DAILY_SPEED_TEST.maximumXp,
                        careerId = canonicalCareer,
                        topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
                        mode = QuizMode.DAILY_SPEED_TEST,
                        isAttemptSaved = false,
                        currentStreak = _uiState.value.currentStreak,
                        isAlreadyCompletedToday = alreadyCompleted,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("DAILY_TEST_DEBUG", "DAILY_TEST_DEBUG: Error starting Daily Speed Test: ${e.message}", e)
                _quizState.value = QuizUiState(
                    isLoading = false,
                    errorMessage = "Questions could not be loaded. Please try again.",
                    careerId = canonicalCareer,
                    topic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
                    mode = QuizMode.DAILY_SPEED_TEST,
                    currentStreak = _uiState.value.currentStreak
                )
            }
        }
    }

    fun startSpeedChallenge(careerId: String? = null, topic: String = PracticeConstants.TOPIC_SPEED_CHALLENGE) {
        val canonicalCareer = CareerCanonicalUtil.toCanonicalCareerIdOrNull(
            careerId ?: _uiState.value.selectedCareerId
        )

        _quizState.value = QuizUiState(
            isLoading = true,
            careerId = canonicalCareer,
            topic = topic,
            mode = QuizMode.SPEED_CHALLENGE,
            currentStreak = _uiState.value.currentStreak
        )

        viewModelScope.launch {
            try {
                val questions = practiceRepository.getQuizQuestions(
                    careerId = canonicalCareer,
                    topic = topic
                )
                if (questions.isEmpty()) {
                    _quizState.value = QuizUiState(
                        isLoading = false,
                        errorMessage = "Speed challenge questions could not be loaded. Please try again.",
                        careerId = canonicalCareer,
                        topic = topic,
                        mode = QuizMode.SPEED_CHALLENGE,
                        currentStreak = _uiState.value.currentStreak
                    )
                } else {
                    _quizState.value = QuizUiState(
                        questions = questions.take(5),
                        currentIndex = 0,
                        selectedOptionIndex = null,
                        isAnswerChecked = false,
                        isCorrect = false,
                        score = 0,
                        isQuizFinished = false,
                        xpEarned = 0,
                        maximumXp = QuizMode.SPEED_CHALLENGE.maximumXp,
                        careerId = canonicalCareer,
                        topic = topic,
                        mode = QuizMode.SPEED_CHALLENGE,
                        isAttemptSaved = false,
                        currentStreak = _uiState.value.currentStreak,
                        isAlreadyCompletedToday = false,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _quizState.value = QuizUiState(
                    isLoading = false,
                    errorMessage = "Failed to load quiz questions.",
                    careerId = canonicalCareer,
                    topic = topic,
                    mode = QuizMode.SPEED_CHALLENGE,
                    currentStreak = _uiState.value.currentStreak
                )
            }
        }
    }

    fun retryQuiz() {
        val current = _quizState.value
        if (current.mode == QuizMode.DAILY_SPEED_TEST) {
            startDailySpeedTest(current.careerId)
        } else {
            startSpeedChallenge(current.careerId, current.topic)
        }
    }

    fun initializeQuizIfNeeded(
        careerId: String? = null,
        topic: String = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
        mode: QuizMode = QuizMode.DAILY_SPEED_TEST
    ) {
        val current = _quizState.value
        if (current.questions.isNotEmpty()) {
            return
        }
        if (mode == QuizMode.DAILY_SPEED_TEST) {
            startDailySpeedTest(careerId)
        } else {
            startSpeedChallenge(careerId, topic)
        }
    }

    fun startQuiz(careerId: String? = null, topic: String = PracticeConstants.TOPIC_DAILY_SPEED_TEST) {
        if (topic == PracticeConstants.TOPIC_DAILY_SPEED_TEST) {
            startDailySpeedTest(careerId)
        } else {
            startSpeedChallenge(careerId, topic)
        }
    }

    fun startNewQuiz(careerId: String? = null, topic: String = "general", mode: QuizMode? = null) {
        val effectiveMode = mode ?: if (topic == PracticeConstants.TOPIC_DAILY_SPEED_TEST) {
            QuizMode.DAILY_SPEED_TEST
        } else {
            QuizMode.SPEED_CHALLENGE
        }
        if (effectiveMode == QuizMode.DAILY_SPEED_TEST) {
            startDailySpeedTest(careerId)
        } else {
            startSpeedChallenge(careerId, topic)
        }
    }

    fun selectQuizOption(index: Int) {
        if (!_quizState.value.isAnswerChecked) {
            _quizState.update { it.copy(selectedOptionIndex = index) }
        }
    }

    fun checkAnswer() {
        val current = _quizState.value
        val question = current.questions.getOrNull(current.currentIndex) ?: return
        val isCorrect = current.selectedOptionIndex == question.correctIndex
        val addedScore = if (isCorrect) 1 else 0
        val newScore = current.score + addedScore
        val intermediateXp = current.mode.calculateEarnedXp(newScore, current.questions.size)

        _quizState.update {
            it.copy(
                isAnswerChecked = true,
                isCorrect = isCorrect,
                score = newScore,
                xpEarned = intermediateXp
            )
        }
    }

    fun nextQuizQuestion() {
        val current = _quizState.value
        if (current.currentIndex + 1 < current.questions.size) {
            _quizState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedOptionIndex = null,
                    isAnswerChecked = false,
                    isCorrect = false
                )
            }
        } else {
            // Calculate final proportional performance XP based on test's maximum XP
            val totalQuestions = current.questions.size
            val finalXp = if (current.mode == QuizMode.DAILY_SPEED_TEST && current.isAlreadyCompletedToday) {
                0 // Duplicate on same day awards 0 XP
            } else {
                current.mode.calculateEarnedXp(current.score, totalQuestions)
            }

            _quizState.update {
                it.copy(
                    isQuizFinished = true,
                    xpEarned = finalXp
                )
            }
            saveCompletedQuizAttempt(finalXp)
        }
    }

    private fun saveCompletedQuizAttempt(finalXp: Int) {
        val current = _quizState.value
        if (current.isAttemptSaved) return
        _quizState.update { it.copy(isAttemptSaved = true) }

        viewModelScope.launch {
            val attempt = QuizAttempt(
                id = null,
                userId = "", // Resolved securely inside SupabasePracticeRepository
                careerId = current.careerId,
                topic = current.topic,
                score = current.score,
                totalQuestions = current.questions.size,
                xpEarned = finalXp,
                completedAt = System.currentTimeMillis()
            )
            val result = practiceRepository.saveQuizAttempt(attempt)

            if (result.isSuccess) {
                // Refresh daily completion status in UI state
                val isDailyCompleted = practiceRepository.isDailyTestCompletedToday()
                _uiState.update { it.copy(isDailyTestCompletedToday = isDailyCompleted) }

                // Immediately reflect the updated streak and XP in UI state from the authoritative profile cache
                authRepository?.let { auth ->
                    val updatedUser = auth.getCurrentUser().firstOrNull()
                    if (updatedUser != null) {
                        _uiState.update {
                            it.copy(
                                currentStreak = updatedUser.currentStreak,
                                totalXp = updatedUser.xp
                            )
                        }
                        _quizState.update { it.copy(currentStreak = updatedUser.currentStreak) }
                    }
                }
            } else {
                try {
                    android.util.Log.e("PracticeViewModel", "Failed to save quiz attempt: ${result.exceptionOrNull()?.message}")
                } catch (_: Throwable) {}
            }
        }
    }
}
