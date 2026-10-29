package com.example.domain.coordinator

import android.util.Log
import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorFocusItem
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CareerRepository
import com.example.domain.repository.PracticeRepository
import com.example.domain.repository.RoadmapRepository
import com.example.util.CareerCanonicalUtil
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Coordinator responsible for assembling verified, read-only student context from
 * authoritative CGA data sources and applying deterministic relevance filtering.
 *
 * Design Guarantees:
 * - Read-Only: Never modifies underlying repository state or database tables.
 * - Strict Truthfulness: Missing data remains null; never defaults to 0 or hallucinated values.
 * - Deterministic Filtering: Context fields sent to the AI Mentor are tailored to query intent.
 * - Fault Tolerant: A failure in an optional source (e.g. Resume or Practice) does not crash chat.
 */
class CGAMentorContextProvider(
    private val authRepository: AuthRepository? = null,
    private val careerRepository: CareerRepository? = null,
    private val roadmapRepository: RoadmapRepository? = null,
    private val practiceRepository: PracticeRepository? = null
) {
    companion object {
        private const val TAG = "MentorContextProv"

        private val RESUME_KEYWORDS = listOf(
            "resume", "cv", "ats", "format", "bullet", "project description",
            "skills gap", "missing skill", "portfolio", "ats score", "missing"
        )

        private val ROADMAP_KEYWORDS = listOf(
            "roadmap", "milestone", "stage", "learn next", "checkpoint",
            "study plan", "step by step", "next topic", "curriculum",
            "what should i do this week", "what to learn", "next task",
            "focus", "today", "what to do"
        )

        private val PRACTICE_KEYWORDS = listOf(
            "practice", "interview", "quiz", "speed test", "leetcode",
            "challenge", "problem", "solve", "dsa"
        )

        private val CAREER_KEYWORDS = listOf(
            "career", "domain", "role", "job", "placement", "salary", "lpa",
            "match", "company", "hiring", "aiml", "sde", "data scientist",
            "recommend", "recommended", "recommendation", "which career", "career path",
            "topic", "topics", "which topic", "my topic", "what topic", "path", "track", "field",
            "stream", "cga", "assessment", "suggested", "suggest", "profile",
            "guidance", "interest", "advised", "advice", "direction", "choice",
            "specialization", "specialise", "major", "my recommendation", "what did you recommend",
            "which topic i have recommended", "what is my topic"
        )

        private val MOTIVATION_KEYWORDS = listOf(
            "streak", "xp", "rank", "points", "leaderboard", "progress",
            "daily", "motivat", "score", "level", "accountability"
        )

        private val PROFILE_KEYWORDS = listOf(
            "who am i", "about me", "my profile", "my details", "my info",
            "do you know me", "my name", "what do you know about me",
            "my dashboard", "my account", "my status"
        )
    }

    /**
     * Assembles full verified student context across all authoritative CGA repositories.
     */
    suspend fun assembleVerifiedContext(): CGAMentorContext {
        var studentName: String? = null
        var department: String? = null
        var yearOfStudy: String? = null
        var careerInterest: String? = null
        var recommendedCareer: String? = null
        var matchPercentage: Int? = null
        var currentRoadmapStage: String? = null
        var roadmapProgress: Int? = null
        var completedMilestones: List<String>? = null
        var nextPendingMilestones: List<String>? = null
        var solvedPracticeCount: Int? = null
        var totalPracticeCount: Int? = null
        var currentStreak: Int? = null
        var xp: Int? = null
        var resumeAtsScore: Int? = null
        var resumeMatchedSkills: List<String>? = null
        var resumeMissingSkills: List<String>? = null

        var canonicalCareerId: String? = null

        // 1. Profile & Gamification Context
        try {
            val userFlow = authRepository?.getCurrentUser()
            val profile = try {
                withTimeoutOrNull(2500L) {
                    userFlow?.filterNotNull()?.first()
                }
            } catch (_: Throwable) {
                null
            } ?: userFlow?.firstOrNull()

            if (profile != null) {
                studentName = profile.name.trim().ifBlank { null }
                department = profile.department.trim().ifBlank { null }
                yearOfStudy = profile.yearOfStudy.trim().ifBlank { null }
                careerInterest = profile.careerInterest.trim().ifBlank { null }
                canonicalCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(profile.recommendedCareerId)
                if (canonicalCareerId != null) {
                    recommendedCareer = CareerCanonicalUtil.getCareerTitle(canonicalCareerId)
                }
                currentStreak = if (profile.currentStreak > 0) profile.currentStreak else null
                xp = if (profile.xp > 0) profile.xp else null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Non-fatal: Unable to load profile context: ${e.message}")
        }

        // 2. Career Recommendation Context
        try {
            if (careerRepository != null) {
                val recommendation = try {
                    careerRepository.loadLatestRecommendation().getOrNull()
                } catch (_: Throwable) {
                    null
                } ?: try {
                    withTimeoutOrNull(2500L) {
                        careerRepository.getLatestRecommendation().filterNotNull().first()
                    }
                } catch (_: Throwable) {
                    null
                } ?: careerRepository.getLatestRecommendation().firstOrNull()

                if (recommendation != null) {
                    recommendedCareer = recommendation.title.trim().ifBlank { recommendedCareer }
                    if (recommendation.matchPercentage > 0) {
                        matchPercentage = recommendation.matchPercentage
                    }
                    if (canonicalCareerId.isNullOrBlank()) {
                        canonicalCareerId = CareerCanonicalUtil.toCanonicalCareerIdOrNull(recommendation.careerId)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Non-fatal: Unable to load recommendation context: ${e.message}")
        }

        // 3. Roadmap Progress Context
        try {
            if (roadmapRepository != null && !canonicalCareerId.isNullOrBlank()) {
                val roadmap = try {
                    withTimeoutOrNull(2500L) {
                        roadmapRepository.getRoadmap(canonicalCareerId).filterNotNull().first()
                    }
                } catch (_: Throwable) {
                    null
                } ?: roadmapRepository.getRoadmap(canonicalCareerId).firstOrNull()

                if (roadmap != null) {
                    roadmapProgress = roadmap.overallProgressPercentage
                    currentRoadmapStage = roadmap.stages.firstOrNull { it.progressPercentage < 100 }?.stageName
                        ?: roadmap.stages.lastOrNull()?.stageName

                    val completed = roadmap.stages.flatMap { it.tasks }.filter { it.isCompleted }.map { it.title.trim() }
                    completedMilestones = completed.ifEmpty { null }

                    val pending = roadmap.stages.flatMap { it.tasks }.filterNot { it.isCompleted }.map { it.title.trim() }
                    nextPendingMilestones = pending.ifEmpty { null }
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Non-fatal: Unable to load roadmap context: ${e.message}")
        }

        // 4. Practice Progress Context
        try {
            if (practiceRepository != null) {
                val problems = try {
                    withTimeoutOrNull(2500L) {
                        (practiceRepository.getProblems(canonicalCareerId).firstOrNull()
                            ?: practiceRepository.getProblems().firstOrNull())
                    }
                } catch (_: Throwable) {
                    null
                }
                if (problems != null && problems.isNotEmpty()) {
                    solvedPracticeCount = problems.count { it.isSolved }
                    totalPracticeCount = problems.size
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Non-fatal: Unable to load practice context: ${e.message}")
        }

        // 5. Resume Analysis Context
        try {
            if (practiceRepository != null) {
                val resume = practiceRepository.getResumeAnalysis()
                if (resume.atsScore > 0) {
                    resumeAtsScore = resume.atsScore
                }
                val matched = resume.matchedSkills.map { it.trim() }.filter { it.isNotBlank() }
                resumeMatchedSkills = matched.ifEmpty { null }

                val missing = resume.missingSkills.map { it.trim() }.filter { it.isNotBlank() }
                resumeMissingSkills = missing.ifEmpty { null }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Non-fatal: Unable to load resume context: ${e.message}")
        }

        return CGAMentorContext(
            studentName = studentName,
            department = department,
            yearOfStudy = yearOfStudy,
            careerInterest = careerInterest,
            recommendedCareer = recommendedCareer,
            matchPercentage = matchPercentage,
            currentRoadmapStage = currentRoadmapStage,
            roadmapProgress = roadmapProgress,
            completedMilestones = completedMilestones,
            nextPendingMilestones = nextPendingMilestones,
            solvedPracticeCount = solvedPracticeCount,
            totalPracticeCount = totalPracticeCount,
            currentStreak = currentStreak,
            xp = xp,
            resumeAtsScore = resumeAtsScore,
            resumeMatchedSkills = resumeMatchedSkills,
            resumeMissingSkills = resumeMissingSkills
        )
    }

    /**
     * Deterministically selects only the relevant context fields based on user query intent.
     * Prevents dumping unrelated context into casual or general queries.
     */
    fun selectRelevantContext(fullContext: CGAMentorContext?, userQuery: String): CGAMentorContext? {
        if (fullContext == null) return null
        val queryLower = userQuery.trim().lowercase()
        if (queryLower.isBlank()) return null

        val isResumeIntent = RESUME_KEYWORDS.any { queryLower.contains(it) }
        val isRoadmapIntent = ROADMAP_KEYWORDS.any { queryLower.contains(it) }
        val isPracticeIntent = PRACTICE_KEYWORDS.any { queryLower.contains(it) }
        val isCareerIntent = CAREER_KEYWORDS.any { queryLower.contains(it) }
        val isMotivationIntent = MOTIVATION_KEYWORDS.any { queryLower.contains(it) }
        val isProfileIntent = PROFILE_KEYWORDS.any { queryLower.contains(it) }

        // If completely general/casual question with no CGA intent detected
        if (!isResumeIntent && !isRoadmapIntent && !isPracticeIntent && !isCareerIntent && !isMotivationIntent && !isProfileIntent) {
            return CGAMentorContext(
                studentName = fullContext.studentName
            )
        }

        return CGAMentorContext(
            studentName = fullContext.studentName,
            department = if (isCareerIntent || isResumeIntent || isProfileIntent) fullContext.department else null,
            yearOfStudy = if (isCareerIntent || isResumeIntent || isProfileIntent) fullContext.yearOfStudy else null,
            careerInterest = if (isCareerIntent || isProfileIntent) fullContext.careerInterest else null,
            recommendedCareer = if (isCareerIntent || isRoadmapIntent || isPracticeIntent || isResumeIntent || isProfileIntent) fullContext.recommendedCareer else null,
            matchPercentage = if (isCareerIntent || isProfileIntent) fullContext.matchPercentage else null,
            currentRoadmapStage = if (isRoadmapIntent || isPracticeIntent) fullContext.currentRoadmapStage else null,
            roadmapProgress = if (isRoadmapIntent || isMotivationIntent) fullContext.roadmapProgress else null,
            completedMilestones = if (isRoadmapIntent) fullContext.completedMilestones else null,
            nextPendingMilestones = if (isRoadmapIntent) fullContext.nextPendingMilestones else null,
            solvedPracticeCount = if (isPracticeIntent || isMotivationIntent) fullContext.solvedPracticeCount else null,
            totalPracticeCount = if (isPracticeIntent || isMotivationIntent) fullContext.totalPracticeCount else null,
            currentStreak = if (isMotivationIntent || isPracticeIntent || isRoadmapIntent || isProfileIntent) fullContext.currentStreak else null,
            xp = if (isMotivationIntent || isProfileIntent) fullContext.xp else null,
            resumeAtsScore = if (isResumeIntent) fullContext.resumeAtsScore else null,
            resumeMatchedSkills = if (isResumeIntent) fullContext.resumeMatchedSkills else null,
            resumeMissingSkills = if (isResumeIntent) fullContext.resumeMissingSkills else null
        )
    }

    /**
     * Derives lightweight, actionable focus items from verified data.
     * Never fabricates focus items without genuine backing data.
     */
    fun deriveProactiveFocusItems(context: CGAMentorContext?): List<MentorFocusItem> {
        if (context == null) return emptyList()
        val items = mutableListOf<MentorFocusItem>()

        // 1. Next Pending Roadmap Task
        val pendingTask = context.nextPendingMilestones?.firstOrNull()
        if (!pendingTask.isNullOrBlank()) {
            items.add(
                MentorFocusItem(
                    title = "Complete Next Roadmap Task",
                    description = "Next checkpoint: $pendingTask",
                    priority = "HIGH",
                    source = "ROADMAP"
                )
            )
        }

        // 2. Missing Resume Skills
        val missingSkills = context.resumeMissingSkills?.take(2)
        if (!missingSkills.isNullOrEmpty()) {
            items.add(
                MentorFocusItem(
                    title = "Bridge Resume Skill Gaps",
                    description = "Focus on mastering: ${missingSkills.joinToString(", ")}",
                    priority = "HIGH",
                    source = "RESUME"
                )
            )
        }

        // 3. Technical Practice Problems
        if (context.solvedPracticeCount != null && context.totalPracticeCount != null && context.solvedPracticeCount < context.totalPracticeCount) {
            val remaining = context.totalPracticeCount - context.solvedPracticeCount
            items.add(
                MentorFocusItem(
                    title = "Solve Technical Coding Problems",
                    description = "$remaining practice problems remaining in your domain",
                    priority = "MEDIUM",
                    source = "PRACTICE"
                )
            )
        }

        // 4. Learning Streak Preservation
        if ((context.currentStreak ?: 0) > 0) {
            items.add(
                MentorFocusItem(
                    title = "Maintain Study Streak",
                    description = "You're on a ${context.currentStreak}-day learning streak. Keep up the momentum today!",
                    priority = "MEDIUM",
                    source = "STREAK"
                )
            )
        }

        // 5. Initial Career Assessment
        if (context.recommendedCareer.isNullOrBlank()) {
            items.add(
                MentorFocusItem(
                    title = "Complete Career Assessment",
                    description = "Take the 15-question AI assessment to unlock your personalized roadmap.",
                    priority = "HIGH",
                    source = "ASSESSMENT"
                )
            )
        }

        return items
    }
}
