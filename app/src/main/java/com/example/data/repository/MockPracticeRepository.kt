package com.example.data.repository

import com.example.domain.model.AdminOverview
import com.example.domain.model.InterviewQuestion
import com.example.domain.model.PracticeConstants
import com.example.domain.model.PracticeProblem
import com.example.domain.model.ProjectItem
import com.example.domain.model.QuizAttempt
import com.example.domain.model.QuizQuestion
import com.example.domain.model.ResumeAnalysis
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PracticeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * In-memory Mock implementation of [PracticeRepository] for unit tests.
 */
class MockPracticeRepository(
    private val authRepository: AuthRepository? = null,
    initialSolvedProblemIds: Set<String> = setOf("dsa_01", "dsa_02", "dsa_05", "apt_02", "tech_01")
) : PracticeRepository {

    // User-specific solved state
    private val _userSolvedProblemIds = MutableStateFlow<Set<String>>(initialSolvedProblemIds)
    private val _everSolvedProblemIds = MutableStateFlow<Set<String>>(initialSolvedProblemIds)
    private val _userSolvedMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    // User-specific completed daily test dates: Map<userId, Set<YYYY-MM-DD>>
    private val _dailyTestCompletedDates = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    private val _quizAttempts = MutableStateFlow<List<QuizAttempt>>(emptyList())
    private val _projects = MutableStateFlow<List<ProjectItem>>(DefaultPracticeCatalog.DEFAULT_PROJECTS)

    private fun getTodayDateString(): String {
        return LocalDate.now().toString()
    }

    private suspend fun getCurrentUserId(): String {
        return authRepository?.getCurrentUser()?.firstOrNull()?.id ?: "guest_session"
    }

    override fun getProblems(careerId: String?): Flow<List<PracticeProblem>> {
        val problems = DefaultPracticeCatalog.getPracticeProblems(careerId)
        val userFlow = authRepository?.getCurrentUser()?.map { it?.id ?: "guest_session" } ?: flowOf("guest_session")
        return combine(_userSolvedProblemIds, _userSolvedMap, userFlow) { guestSolved, userMap, userId ->
            val solvedForThisUser = if (userId != "guest_session" && userMap.containsKey(userId)) {
                userMap[userId] ?: emptySet()
            } else if (userId != "guest_session") {
                guestSolved
            } else {
                guestSolved
            }
            problems.map { problem ->
                problem.copy(isSolved = solvedForThisUser.contains(problem.id))
            }
        }
    }

    fun setUserSolvedProblems(userId: String, solvedIds: Set<String>) {
        _userSolvedMap.update { currentMap ->
            currentMap + (userId to solvedIds)
        }
    }

    override suspend fun solveProblem(id: String, selectedAnswerIndex: Int?): com.example.domain.model.PracticeProblemSolveResult {
        val problem = DefaultPracticeCatalog.getProblemById(id)
        if (selectedAnswerIndex != null) {
            if (problem != null && selectedAnswerIndex != problem.correctIndex) {
                val currentXp = authRepository?.getCurrentUser()?.firstOrNull()?.xp ?: 0
                return com.example.domain.model.PracticeProblemSolveResult(
                    isCorrect = false,
                    xpAwarded = 0,
                    isFirstSolve = false,
                    isSolved = false,
                    totalXp = currentXp,
                    message = "Incorrect. Check the hint or explanation and try again!"
                )
            }
        }

        val userId = getCurrentUserId()
        val wasEverSolved = _everSolvedProblemIds.value.contains(id)
        _userSolvedProblemIds.update { solvedIds -> solvedIds + id }
        _userSolvedMap.update { currentMap ->
            val existing = currentMap[userId] ?: _userSolvedProblemIds.value
            currentMap + (userId to (existing + id))
        }
        _everSolvedProblemIds.update { it + id }

        val xpReward = problem?.xpReward ?: 20
        var totalXp: Int? = null

        if (!wasEverSolved && authRepository != null) {
            val userId = getCurrentUserId()
            val profile = authRepository.getCurrentUser().firstOrNull()
            val currentXp = profile?.xp ?: 0
            val newXp = currentXp + xpReward
            totalXp = newXp
            authRepository.updateCachedXp(userId, newXp)
        } else if (authRepository != null) {
            totalXp = authRepository.getCurrentUser().firstOrNull()?.xp
        }

        return com.example.domain.model.PracticeProblemSolveResult(
            isCorrect = true,
            xpAwarded = if (!wasEverSolved) xpReward else 0,
            isFirstSolve = !wasEverSolved,
            isSolved = true,
            totalXp = totalXp,
            message = if (!wasEverSolved) "First solve success" else "Already completed"
        )
    }

    override suspend fun completeProblem(id: String, selectedAnswerIndex: Int?): Boolean {
        val result = solveProblem(id, selectedAnswerIndex)
        return result.isCorrect && result.isSolved
    }


    override suspend fun toggleProblemSolved(id: String): Boolean {
        return completeProblem(id)
    }

    fun getSolvedProblemIds(): Set<String> = _userSolvedProblemIds.value

    override suspend fun getInterviewQuestions(careerId: String?): List<InterviewQuestion> {
        return DefaultPracticeCatalog.getInterviewQuestions(careerId)
    }

    override suspend fun getQuizQuestions(careerId: String?, topic: String?): List<QuizQuestion> {
        return DefaultPracticeCatalog.getQuizQuestions(careerId, topic)
    }

    override suspend fun isDailyTestCompletedToday(): Boolean {
        val userId = getCurrentUserId()
        val today = getTodayDateString()
        return _dailyTestCompletedDates.value[userId]?.contains(today) == true
    }

    override suspend fun saveQuizAttempt(attempt: QuizAttempt): Result<Unit> {
        val userId = getCurrentUserId()
        val today = getTodayDateString()

        val isDaily = attempt.topic == PracticeConstants.TOPIC_DAILY_SPEED_TEST
        val alreadyCompletedToday = if (isDaily) {
            _dailyTestCompletedDates.value[userId]?.contains(today) == true
        } else {
            false
        }

        val resolvedAttempt = if (isDaily) {
            if (alreadyCompletedToday) {
                // Idempotency: Duplicate Daily Speed Test on the same day awards 0 XP
                attempt.copy(xpEarned = 0)
            } else {
                // First completion today: mark completed and award performance-calculated XP (attempt.xpEarned)
                _dailyTestCompletedDates.update { currentMap ->
                    val userDates = currentMap[userId] ?: emptySet()
                    currentMap + (userId to (userDates + today))
                }
                attempt
            }
        } else {
            // Speed Challenge or other practice modes: awards proportional attempt.xpEarned
            attempt
        }

        _quizAttempts.update { listOf(resolvedAttempt) + it }

        // Authoritative Streak update for Daily Speed Test
        if (isDaily && !alreadyCompletedToday && authRepository != null) {
            val profile = authRepository.getCurrentUser().firstOrNull()
            val currentStreak = profile?.currentStreak ?: 0
            val newStreak = currentStreak + 1
            authRepository.updateCachedStreak(userId, newStreak)
        }

        if (resolvedAttempt.xpEarned > 0 && authRepository != null) {
            val profile = authRepository.getCurrentUser().firstOrNull()
            val currentXp = profile?.xp ?: 0
            authRepository.updateCachedXp(userId, currentXp + resolvedAttempt.xpEarned)
        }

        return Result.success(Unit)
    }

    override suspend fun getQuizAttempts(): List<QuizAttempt> {
        return _quizAttempts.value
    }

    override fun getProjects(): Flow<List<ProjectItem>> = _projects.asStateFlow()

    override suspend fun addProject(project: ProjectItem) {
        val resolvedProject = project.copy(
            id = project.id ?: "proj_${_projects.value.size + 1}",
            userId = if (project.userId.isNotBlank()) project.userId else "usr_student_demo",
            createdAt = project.createdAt ?: System.currentTimeMillis()
        )
        _projects.update { listOf(resolvedProject) + it }
    }

    override suspend fun getResumeAnalysis(): ResumeAnalysis = ResumeAnalysis()

    override suspend fun getAdminOverview(): AdminOverview = AdminOverview()
}
