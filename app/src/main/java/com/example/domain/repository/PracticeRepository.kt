package com.example.domain.repository

import com.example.domain.model.AdminOverview
import com.example.domain.model.InterviewQuestion
import com.example.domain.model.PracticeProblem
import com.example.domain.model.ProjectItem
import com.example.domain.model.QuizAttempt
import com.example.domain.model.QuizQuestion
import com.example.domain.model.ResumeAnalysis
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Placement Preparation, Coding Practice, and Quizzes.
 */
interface PracticeRepository {
    /**
     * Streams practice problems for the given career (or all if null/empty).
     * Solved status is dynamically resolved against the authenticated user's progress.
     */
    fun getProblems(careerId: String? = null): Flow<List<PracticeProblem>>

    /**
     * Backward-compatible default overload.
     */
    fun getProblems(): Flow<List<PracticeProblem>> = getProblems(null)

    /**
     * Marks a practice problem as completed/solved for the active user.
     * Checks user_problem_progress and only awards XP if never previously solved.
     * If selectedAnswerIndex is provided, validates it against problem's correct answer.
     * Returns true if correct and marked solved; false if incorrect.
     */
    suspend fun completeProblem(id: String, selectedAnswerIndex: Int? = null): Boolean

    /**
     * Solves a practice problem with full atomic result metadata (XP awarded, first solve, new total XP).
     */
    suspend fun solveProblem(id: String, selectedAnswerIndex: Int? = null): com.example.domain.model.PracticeProblemSolveResult


    /**
     * Backward-compatible alias for completeProblem.
     */
    suspend fun toggleProblemSolved(id: String) = completeProblem(id)

    /**
     * Retrieves interview questions for the given career (or general if null/empty).
     */
    suspend fun getInterviewQuestions(careerId: String? = null): List<InterviewQuestion>

    /**
     * Backward-compatible default overload.
     */
    suspend fun getInterviewQuestions(): List<InterviewQuestion> = getInterviewQuestions(null)

    /**
     * Retrieves quiz questions filtered by career and/or topic.
     */
    suspend fun getQuizQuestions(careerId: String?, topic: String? = null): List<QuizQuestion>

    /**
     * Backward-compatible default overload.
     */
    suspend fun getQuizQuestions(topic: String): List<QuizQuestion> = getQuizQuestions(careerId = null, topic = topic)

    /**
     * Persists a completed quiz attempt for the current authenticated user.
     */
    suspend fun saveQuizAttempt(attempt: QuizAttempt): Result<Unit>

    /**
     * Checks if the active authenticated user has completed the Daily Speed Test today.
     */
    suspend fun isDailyTestCompletedToday(): Boolean

    /**
     * Retrieves previous quiz attempts for the current authenticated user.
     */
    suspend fun getQuizAttempts(): List<QuizAttempt>

    /**
     * Streams projects for the current authenticated user.
     */
    fun getProjects(): Flow<List<ProjectItem>>

    /**
     * Adds a new project for the current authenticated user.
     */
    suspend fun addProject(project: ProjectItem)

    /**
     * Retrieves resume analysis for the current authenticated user.
     */
    suspend fun getResumeAnalysis(): ResumeAnalysis

    /**
     * Retrieves campus placement overview metrics.
     */
    suspend fun getAdminOverview(): AdminOverview

    /**
     * Clears user-specific transient caches on logout or session change.
     */
    fun clearUserCache(userId: String? = null) {}
}
